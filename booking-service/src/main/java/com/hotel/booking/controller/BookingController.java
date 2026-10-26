package com.hotel.booking.controller;

import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.dto.BookingResponse;
import com.hotel.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/booking")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @RequestBody BookingRequest req) {
        String token = jwt.getTokenValue();
        return bookingService.create(jwt.getSubject(), req, token);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<BookingResponse> history(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return bookingService.history(jwt.getSubject(), pageable);
    }

    @GetMapping("/booking/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.getById(jwt.getSubject(), id);
    }

    @DeleteMapping("/booking/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        String token = jwt.getTokenValue();
        bookingService.cancel(jwt.getSubject(), id, token);
    }
}