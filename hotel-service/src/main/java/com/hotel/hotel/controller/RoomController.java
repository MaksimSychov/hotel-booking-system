package com.hotel.hotel.controller;

import com.hotel.hotel.dto.AvailabilityRequest;
import com.hotel.hotel.dto.RoomDto;
import com.hotel.hotel.dto.RoomRequest;
import com.hotel.hotel.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDto create(@RequestBody @Valid RoomRequest req) {
        return roomService.create(req);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<RoomDto> allFree() {
        return roomService.findAllFree();
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<RoomDto> recommend() {
        return roomService.recommend();
    }

    @PostMapping("/{id}/confirm-availability")
    @PreAuthorize("hasAnyRole('USER','ADMIN','INTERNAL')")
    public void confirm(@PathVariable Long id, @RequestBody AvailabilityRequest req) {
        roomService.confirmAvailability(id, req);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('INTERNAL')")
    public void release(@PathVariable Long id, @RequestBody AvailabilityRequest req) {
        roomService.release(id, req);
    }
}
