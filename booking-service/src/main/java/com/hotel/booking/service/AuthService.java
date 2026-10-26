package com.hotel.booking.service;

import com.hotel.booking.config.JwtService;
import com.hotel.booking.dto.AuthRequest;
import com.hotel.booking.dto.AuthResponse;
import com.hotel.booking.dto.RegisterRequest;
import com.hotel.booking.entity.Role;
import com.hotel.booking.entity.User;
import com.hotel.booking.exception.ConflictException;
import com.hotel.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ConflictException("Username already exists");
        }
        User user = userRepository.save(User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.USER).build());
        return new AuthResponse(jwtService.generate(user.getUsername(), user.getRole().name()));
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
        return new AuthResponse(jwtService.generate(user.getUsername(), user.getRole().name()));
    }
}
