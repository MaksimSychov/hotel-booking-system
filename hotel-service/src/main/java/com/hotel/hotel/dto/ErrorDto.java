package com.hotel.hotel.dto;

import java.time.Instant;

public record ErrorDto(int status, String error, String message, Instant timestamp) {
    public static ErrorDto of(int status, String error, String message) {
        return new ErrorDto(status, error, message, Instant.now());
    }
}
