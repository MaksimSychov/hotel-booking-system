package com.hotel.booking.dto;

import com.hotel.booking.entity.Role;

public record UserDto(Long id, String username, Role role) {}
