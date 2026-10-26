package com.hotel.booking.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

public record BookingRequest(
        Long roomId,
        boolean autoSelect,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,
        String requestId
) {}