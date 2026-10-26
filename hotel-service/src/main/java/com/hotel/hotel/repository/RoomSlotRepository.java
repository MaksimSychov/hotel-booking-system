package com.hotel.hotel.repository;

import com.hotel.hotel.entity.RoomSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RoomSlotRepository extends JpaRepository<RoomSlot, Long> {
    boolean existsByRoomIdAndSlotDateBetween(Long roomId, LocalDate start, LocalDate end);
    List<RoomSlot> findByRequestId(String requestId);
    long countByRoomId(Long roomId);
}
