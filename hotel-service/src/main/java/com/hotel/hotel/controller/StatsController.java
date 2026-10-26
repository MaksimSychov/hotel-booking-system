package com.hotel.hotel.controller;

import com.hotel.hotel.dto.RoomStatsDto;
import com.hotel.hotel.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class StatsController {
    private final RoomService roomService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoomStatsDto> stats() {
        return roomService.stats();
    }
}
