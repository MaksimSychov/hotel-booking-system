package com.hotel.booking.controller;

import com.hotel.booking.dto.*;
import com.hotel.booking.entity.Role;
import com.hotel.booking.service.AuthService;
import com.hotel.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/auth")
    public AuthResponse auth(@RequestBody AuthRequest req) {
        return authService.authenticate(req);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody RegisterRequest req) {
        return userService.create(req.username(), req.password(), Role.USER);
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto update(@RequestParam Long id,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) Role role) {
        return userService.update(id, username, role);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam Long id) {
        userService.delete(id);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return userService.findByUsername(jwt.getSubject());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> all() {
        return userService.findAll();
    }
}
