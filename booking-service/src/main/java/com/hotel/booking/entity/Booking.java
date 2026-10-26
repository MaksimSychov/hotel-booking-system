package com.hotel.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings",
       indexes = {
           @Index(name = "idx_booking_room_dates", columnList = "room_id,start_date,end_date"),
           @Index(name = "idx_booking_user", columnList = "user_id")
       },
       uniqueConstraints = @UniqueConstraint(name = "uk_booking_request", columnNames = "request_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "request_id", nullable = false)
    private String requestId;
}
