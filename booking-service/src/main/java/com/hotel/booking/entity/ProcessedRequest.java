package com.hotel.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "processed_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedRequest {
    @Id
    private String requestId;
    private Instant processedAt;
}
