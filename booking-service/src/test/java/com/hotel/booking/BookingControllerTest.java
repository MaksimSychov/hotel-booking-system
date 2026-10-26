package com.hotel.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.dto.BookingResponse;
import com.hotel.booking.entity.BookingStatus;
import com.hotel.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;
    @MockBean BookingService bookingService;

    @Test
    void create_unauthorized_401() throws Exception {
        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_authorized_201() throws Exception {
        BookingRequest req = new BookingRequest(
                1L, false, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "req-1");

        when(bookingService.create(eq("user"), any(), any()))
                .thenReturn(new BookingResponse(1L, 1L, 1L, req.startDate(), req.endDate(),
                        BookingStatus.CONFIRMED));

        mockMvc.perform(post("/api/booking")
                        .with(jwt().jwt(j -> j.subject("user")
                                        .claim("roles", List.of("USER")))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void adminEndpoint_forbiddenForUser_403() throws Exception {
        mockMvc.perform(post("/user")
                        .with(jwt().jwt(j -> j.subject("user")
                                        .claim("roles", List.of("USER")))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"y\"}"))
                .andExpect(status().isForbidden());
    }
}