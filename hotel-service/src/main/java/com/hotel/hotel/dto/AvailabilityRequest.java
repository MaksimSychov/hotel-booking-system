package com.hotel.hotel.dto;

import java.time.LocalDate;

public record AvailabilityRequest(Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {}