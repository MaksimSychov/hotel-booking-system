package com.hotel.booking.client;

import com.hotel.booking.dto.BookingRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotelClient {

    private final RestClient hotelRestClient;

    public record RoomView(Long id, Long hotelId, String number, boolean available, long timesBooked) {}

    @Retry(name = "hotelClient")
    @CircuitBreaker(name = "hotelClient")
    public List<RoomView> recommendRooms(String token) {
        RoomView[] rooms = hotelRestClient.get()
                .uri("/api/rooms/recommend")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(RoomView[].class);
        return rooms == null ? List.of() : List.of(rooms);
    }

    @Retry(name = "hotelClient")
    @CircuitBreaker(name = "hotelClient")
    public void confirmAvailability(Long roomId, BookingRequest req, String token) {
        hotelRestClient.post()
                .uri("/api/rooms/{id}/confirm-availability", roomId)
                .header("Authorization", "Bearer " + token)
                .body(Map.of(
                        "roomId", roomId,
                        "startDate", req.startDate().toString(),
                        "endDate", req.endDate().toString(),
                        "requestId", req.requestId()))
                .retrieve()
                .toBodilessEntity();
    }

    @Retry(name = "hotelClient")
    public void release(Long roomId, String requestId, BookingRequest req, String token) {
        log.info("Compensation: releasing room {} for requestId={}", roomId, requestId);
        hotelRestClient.post()
                .uri("/api/rooms/{id}/release", roomId)
                .header("Authorization", "Bearer " + token)
                .body(Map.of(
                        "roomId", roomId,
                        "startDate", req.startDate().toString(),
                        "endDate", req.endDate().toString(),
                        "requestId", requestId))
                .retrieve()
                .toBodilessEntity();
    }
}