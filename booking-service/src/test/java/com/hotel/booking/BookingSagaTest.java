package com.hotel.booking;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.dto.BookingResponse;
import com.hotel.booking.entity.BookingStatus;
import com.hotel.booking.entity.User;
import com.hotel.booking.repository.UserRepository;
import com.hotel.booking.service.BookingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"
})
class BookingSagaTest {

    private static final String TEST_TOKEN = "dummy-jwt-token-for-tests";
    static WireMockServer wireMock;

    @TestConfiguration
    @SuppressWarnings("unused")
    static class WireMockConfig {
        @Bean
        @Primary
        public RestClient hotelRestClient() {
            if (wireMock == null || !wireMock.isRunning()) {
                wireMock = new WireMockServer(
                        com.github.tomakehurst.wiremock.core.WireMockConfiguration.options()
                                .dynamicPort()
                                .containerThreads(20)
                                .jettyAcceptors(4)
                                .jettyAcceptQueueSize(100)
                );
                wireMock.start();
            }
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(2000);
            factory.setReadTimeout(2000);

            return RestClient.builder()
                    .baseUrl("http://localhost:" + wireMock.port())
                    .requestFactory(factory)
                    .build();
        }
    }

    @Autowired BookingService bookingService;
    @Autowired UserRepository userRepository;
    @Autowired CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setup() {
        if (wireMock == null || !wireMock.isRunning()) {
            wireMock = new WireMockServer(
                    com.github.tomakehurst.wiremock.core.WireMockConfiguration.options()
                            .dynamicPort()
                            .containerThreads(20)
                            .jettyAcceptors(4)
                            .jettyAcceptQueueSize(100)
            );
            wireMock.start();
        }
        wireMock.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);

        if (userRepository.findByUsername("saga-user").isEmpty()) {
            userRepository.save(User.builder()
                    .username("saga-user")
                    .password("{noop}pwd")
                    .role(com.hotel.booking.entity.Role.USER).build());
        }
    }

    @AfterAll
    static void tearDown() {
        if (wireMock != null) wireMock.stop();
    }

    @Test
    void confirm_success() {
        wireMock.stubFor(post(urlEqualTo("/api/rooms/10/confirm-availability"))
                .willReturn(aResponse().withStatus(200)));

        BookingRequest req = new BookingRequest(10L, false,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                "saga-1-" + System.nanoTime());
        BookingResponse resp = bookingService.create("saga-user", req, TEST_TOKEN);

        assertThat(resp.status()).isEqualTo(BookingStatus.CONFIRMED);
        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/rooms/10/confirm-availability")));
    }

    @Test
    void confirm_fail_compensation() {
        wireMock.stubFor(post(urlEqualTo("/api/rooms/20/confirm-availability"))
                .willReturn(aResponse().withStatus(500)));
        wireMock.stubFor(post(urlEqualTo("/api/rooms/20/release"))
                .willReturn(aResponse().withStatus(200)));

        BookingRequest req = new BookingRequest(20L, false,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                "saga-2-" + System.nanoTime());
        BookingResponse resp = bookingService.create("saga-user", req, TEST_TOKEN);

        assertThat(resp.status()).isEqualTo(BookingStatus.CANCELLED);
        wireMock.verify(postRequestedFor(urlEqualTo("/api/rooms/20/release")));
    }

    @Test
    void confirm_timeout_compensation() {
        wireMock.stubFor(post(urlEqualTo("/api/rooms/30/confirm-availability"))
                .willReturn(aResponse().withFixedDelay(5000).withStatus(200)));
        wireMock.stubFor(post(urlEqualTo("/api/rooms/30/release"))
                .willReturn(aResponse().withStatus(200)));

        BookingRequest req = new BookingRequest(30L, false,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                "saga-3-" + System.nanoTime());
        BookingResponse resp = bookingService.create("saga-user", req, TEST_TOKEN);

        assertThat(resp.status()).isEqualTo(BookingStatus.CANCELLED);
    }
}