package com.hotel.booking;

import com.hotel.booking.client.HotelClient;
import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.exception.ConflictException;
import com.hotel.booking.entity.User;
import com.hotel.booking.repository.UserRepository;
import com.hotel.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class BookingConcurrencyTest {

    private static final String TEST_TOKEN = "dummy-jwt-token";

    @Autowired BookingService bookingService;
    @Autowired UserRepository userRepository;
    @MockBean HotelClient hotelClient;

    @Test
    void multipleBookings_sameRoom_shouldConflictOnOverlap() {
        if (userRepository.findByUsername("conc-user").isEmpty()) {
            userRepository.save(User.builder()
                    .username("conc-user").password("{noop}pwd")
                    .role(com.hotel.booking.entity.Role.USER).build());
        }
        doAnswer(inv -> null).when(hotelClient).confirmAvailability(anyLong(), any(), anyString());
        doAnswer(inv -> null).when(hotelClient).release(anyLong(), any(), any(), anyString());

        LocalDate s = LocalDate.now().plusDays(10);
        LocalDate e = LocalDate.now().plusDays(12);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            try {
                bookingService.create("conc-user",
                        new BookingRequest(99L, false, s, e, "conc-" + i + "-" + System.nanoTime()),
                        TEST_TOKEN);
                success.incrementAndGet();
            } catch (ConflictException ex) {
                conflict.incrementAndGet();
            }
        }

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(4);
    }
}