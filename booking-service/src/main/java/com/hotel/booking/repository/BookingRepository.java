package com.hotel.booking.repository;

import com.hotel.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.roomId = :roomId
          AND b.status IN ('PENDING','CONFIRMED')
          AND b.startDate < :endDate
          AND b.endDate > :startDate
        """)
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    boolean existsByRequestId(String requestId);
}
