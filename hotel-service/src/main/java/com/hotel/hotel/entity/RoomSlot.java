package com.hotel.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "room_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_date",
                columnNames = {"room_id", "slot_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "request_id", nullable = false)
    private String requestId;
}
