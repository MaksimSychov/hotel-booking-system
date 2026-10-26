package com.hotel.booking.service;

import com.hotel.booking.client.HotelClient;
import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.dto.BookingResponse;
import com.hotel.booking.entity.*;
import com.hotel.booking.exception.ConflictException;
import com.hotel.booking.exception.NotFoundException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.ProcessedRequestRepository;
import com.hotel.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ProcessedRequestRepository processedRequestRepository;
    private final HotelClient hotelClient;

    @Transactional
    public BookingResponse create(String username, BookingRequest req, String token) {
        validateDates(req);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        String requestId = req.requestId() == null || req.requestId().isBlank()
                ? UUID.randomUUID().toString() : req.requestId();

        if (processedRequestRepository.existsById(requestId)) {
            log.info("Idempotent booking return for requestId={}", requestId);
            return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), Pageable.ofSize(100))
                    .stream()
                    .filter(b -> requestId.equals(b.getRequestId()))
                    .findFirst()
                    .map(this::toResponse)
                    .orElseThrow(() -> new ConflictException("Request already processed"));
        }

        Long roomId = req.roomId();
        if (req.autoSelect() || roomId == null) {
            roomId = chooseRoom(req, token);
        }
        if (bookingRepository.countOverlapping(roomId, req.startDate(), req.endDate()) > 0) {
            throw new ConflictException("Room " + roomId + " is already booked for these dates");
        }

        Booking booking = Booking.builder()
                .userId(user.getId())
                .roomId(roomId)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(BookingStatus.PENDING)
                .createdAt(Instant.now())
                .requestId(requestId)
                .build();
        booking = bookingRepository.save(booking);
        processedRequestRepository.save(ProcessedRequest.builder()
                .requestId(requestId).processedAt(Instant.now()).build());
        log.info("Booking {} PENDING room={} requestId={}", booking.getId(), roomId, requestId);

        BookingRequest enriched = new BookingRequest(roomId, false,
                req.startDate(), req.endDate(), requestId);
        try {
            hotelClient.confirmAvailability(roomId, enriched, token);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Booking {} CONFIRMED", booking.getId());
        } catch (Exception ex) {
            log.error("Booking {} confirm failed: {} — compensation", booking.getId(), ex.getMessage());
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            try {
                hotelClient.release(roomId, requestId, enriched, token);
                log.info("Booking {} compensation done", booking.getId());
            } catch (Exception cex) {
                log.error("Compensation failed for booking {}: {}", booking.getId(), cex.getMessage());
            }
        }
        return toResponse(booking);
    }

    private Long chooseRoom(BookingRequest req, String token) {
        List<HotelClient.RoomView> rooms = hotelClient.recommendRooms(token);
        return rooms.stream()
                .filter(r -> bookingRepository.countOverlapping(
                        r.id(), req.startDate(), req.endDate()) == 0)
                .findFirst()
                .orElseThrow(() -> new ConflictException("No free rooms for these dates"))
                .id();
    }

    private void validateDates(BookingRequest req) {
        if (req.startDate() == null || req.endDate() == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (!req.endDate().isAfter(req.startDate())) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }
        if (req.startDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("startDate must be in the future");
        }
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> history(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(String username, Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!b.getUserId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return toResponse(b);
    }

    @Transactional
    public void cancel(String username, Long id, String token) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!b.getUserId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        if (b.getStatus() == BookingStatus.CANCELLED) return;
        BookingRequest req = new BookingRequest(b.getRoomId(), false,
                b.getStartDate(), b.getEndDate(), b.getRequestId());
        b.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(b);
        try {
            hotelClient.release(b.getRoomId(), b.getRequestId(), req, token);   // ← token
        } catch (Exception ex) {
            log.warn("Release failed during cancellation of {}", id, ex);
        }
        log.info("Booking {} CANCELLED by {}", id, username);
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(b.getId(), b.getUserId(), b.getRoomId(),
                b.getStartDate(), b.getEndDate(), b.getStatus());
    }
}