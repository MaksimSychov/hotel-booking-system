package com.hotel.hotel;

import com.hotel.hotel.dto.AvailabilityRequest;
import com.hotel.hotel.dto.RoomDto;
import com.hotel.hotel.dto.RoomRequest;
import com.hotel.hotel.repository.RoomRepository;
import com.hotel.hotel.repository.RoomSlotRepository;
import com.hotel.hotel.repository.HotelRepository;
import com.hotel.hotel.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Transactional
class RoomServiceTest {

    @Autowired RoomService roomService;
    @Autowired RoomSlotRepository roomSlotRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired HotelRepository hotelRepository;

    @BeforeEach
    void cleanDb() {
        roomSlotRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
    }

    @Test
    void recommend_sortedByTimesBookedThenId() {
        RoomDto r1 = roomService.create(new RoomRequest(1L, "A1", true));
        RoomDto r2 = roomService.create(new RoomRequest(1L, "A2", true));

        roomService.confirmAvailability(r1.id(), new AvailabilityRequest(
                r1.id(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "req-1"));

        List<RoomDto> rec = roomService.recommend();
        assertThat(rec).hasSize(2);
        assertThat(rec.get(0).number()).isEqualTo("A2");
        assertThat(rec.get(1).number()).isEqualTo("A1");
    }

    @Test
    void confirm_idempotent() {
        RoomDto r = roomService.create(new RoomRequest(1L, "B1", true));
        AvailabilityRequest req = new AvailabilityRequest(
                r.id(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "req-dup");
        roomService.confirmAvailability(r.id(), req);
        roomService.confirmAvailability(r.id(), req);

        assertThat(roomService.recommend().stream()
                .filter(x -> x.id().equals(r.id())).findFirst().orElseThrow().timesBooked())
                .isEqualTo(1);
    }

    @Test
    void confirm_conflictOnOverlappingDates() {
        RoomDto r = roomService.create(new RoomRequest(1L, "C1", true));
        LocalDate s = LocalDate.now().plusDays(5);
        LocalDate e = LocalDate.now().plusDays(8);
        roomService.confirmAvailability(r.id(), new AvailabilityRequest(r.id(), s, e, "req-a"));

        assertThatThrownBy(() -> roomService.confirmAvailability(r.id(),
                new AvailabilityRequest(r.id(), s.plusDays(1), e.plusDays(1), "req-b")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void release_freesSlotAndDecrementsCount() {
        RoomDto r = roomService.create(new RoomRequest(1L, "D1", true));
        AvailabilityRequest req = new AvailabilityRequest(
                r.id(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "req-rel");
        roomService.confirmAvailability(r.id(), req);
        roomService.release(r.id(), req);

        assertThat(roomService.recommend().stream()
                .filter(x -> x.id().equals(r.id())).findFirst().orElseThrow().timesBooked())
                .isZero();
    }
}