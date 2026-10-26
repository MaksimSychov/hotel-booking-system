package com.hotel.hotel.service;

import com.hotel.hotel.dto.AvailabilityRequest;
import com.hotel.hotel.dto.RoomDto;
import com.hotel.hotel.dto.RoomRequest;
import com.hotel.hotel.dto.RoomStatsDto;
import com.hotel.hotel.entity.Room;
import com.hotel.hotel.entity.RoomSlot;
import com.hotel.hotel.exception.NotFoundException;
import com.hotel.hotel.mapper.RoomMapper;
import com.hotel.hotel.repository.RoomRepository;
import com.hotel.hotel.repository.RoomSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomSlotRepository roomSlotRepository;
    private final RoomMapper roomMapper;

    private final Map<String, Long> confirmedRequests = new ConcurrentHashMap<>();

    @Transactional
    public RoomDto create(RoomRequest req) {
        Room room = Room.builder()
                .hotelId(req.hotelId())
                .number(req.number())
                .available(req.available() == null || req.available())
                .timesBooked(0L)
                .build();
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Transactional(readOnly = true)
    public List<RoomDto> findAllFree() {
        return roomRepository.findByAvailableTrue().stream().map(roomMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RoomDto> recommend() {
        return roomRepository.findByAvailableTrueOrderByTimesBookedAscIdAsc()
                .stream().map(roomMapper::toDto).toList();
    }

    @Transactional
    public void confirmAvailability(Long roomId, AvailabilityRequest req) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        String requestId = req.requestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = "room-" + roomId + "-" + req.startDate() + "-" + req.endDate();
        }
        Long existing = confirmedRequests.putIfAbsent(requestId, roomId);
        if (existing != null) {
            log.info("Idempotent confirm skip: requestId={} roomId={}", requestId, roomId);
            return;
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found: " + roomId));
        if (!room.isAvailable()) {
            throw new IllegalStateException("Room not available: " + roomId);
        }

        LocalDate d = req.startDate();
        while (d.isBefore(req.endDate())) {
            if (roomSlotRepository.existsByRoomIdAndSlotDateBetween(roomId, d, d)) {
                roomSlotRepository.deleteAll(roomSlotRepository.findByRequestId(requestId));
                confirmedRequests.remove(requestId);
                throw new IllegalStateException("Room " + roomId + " is already booked for " + d);
            }
            roomSlotRepository.save(RoomSlot.builder()
                    .roomId(roomId).slotDate(d).requestId(requestId).build());
            d = d.plusDays(1);
        }

        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);
        log.info("Room {} confirmed [{}..{}), timesBooked={}, requestId={}",
                roomId, req.startDate(), req.endDate(), room.getTimesBooked(), requestId);
    }

    @Transactional
    public void release(Long roomId, AvailabilityRequest req) {
        String requestId = req.requestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = "room-" + roomId + "-" + req.startDate() + "-" + req.endDate();
        }

        if (confirmedRequests.remove(requestId) == null) {
            log.info("Idempotent release skip: requestId={}", requestId);
            return;
        }
        List<RoomSlot> slots = roomSlotRepository.findByRequestId(requestId);
        roomSlotRepository.deleteAll(slots);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found: " + roomId));
        if (room.getTimesBooked() > 0) {
            room.setTimesBooked(room.getTimesBooked() - 1);
            roomRepository.save(room);
        }
        log.info("Room {} released, timesBooked={}, requestId={}",
                roomId, room.getTimesBooked(), requestId);
    }

    @Transactional(readOnly = true)
    public List<RoomStatsDto> stats() {
        return roomRepository.findAll().stream()
                .map(r -> new RoomStatsDto(r.getId(), r.getNumber(),
                        r.getTimesBooked(), roomSlotRepository.countByRoomId(r.getId())))
                .toList();
    }
}
