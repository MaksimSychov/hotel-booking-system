package com.hotel.booking.dto;

import com.hotel.booking.entity.BookingStatus;
import java.time.LocalDate;

public record BookingResponse(
        Long id, Long userId, Long roomId,
        LocalDate startDate, LocalDate endDate, BookingStatus status
) {}
