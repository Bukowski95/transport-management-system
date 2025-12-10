package com.koustav.tms.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koustav.tms.dto.request.BookingRequest;
import com.koustav.tms.dto.response.BookingResponse;
import com.koustav.tms.entity.BookingStatus;
import com.koustav.tms.service.BookingService;

@WebMvcTest(BookingController.class)
@DisplayName("BookingController Tests")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private UUID bookingId;
    private UUID bidId;
    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        bidId = UUID.randomUUID();

        bookingRequest = new BookingRequest();
        bookingRequest.setBidId(bidId);

        bookingResponse = BookingResponse.builder()
            .bookingId(bookingId)
            .bidId(bidId)
            .allocatedTrucks(3)
            .finalRate(5000.0)
            .status(BookingStatus.CONFIRMED)
            .build();
    }

    @Test
    @DisplayName("Should accept bid and create booking, return 201 CREATED")
    void acceptBid_Success() throws Exception {
        // Arrange
        when(bookingService.acceptBid(any(BookingRequest.class))).thenReturn(bookingResponse);

        // Act & Assert
        mockMvc.perform(post("/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingService).acceptBid(any(BookingRequest.class));
    }

    @Test
    @DisplayName("Should get booking by ID and return 200 OK")
    void getBooking_Success() throws Exception {
        // Arrange
        when(bookingService.getBooking(bookingId)).thenReturn(bookingResponse);

        // Act & Assert
        mockMvc.perform(get("/booking/{bookingId}", bookingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
            .andExpect(jsonPath("$.allocatedTrucks").value(3));

        verify(bookingService).getBooking(bookingId);
    }

    @Test
    @DisplayName("Should cancel booking and return 204 NO CONTENT")
    void cancelBooking_Success() throws Exception {
        // Arrange
        doNothing().when(bookingService).cancelBooking(bookingId);

        // Act & Assert
        mockMvc.perform(patch("/booking/{bookingId}/cancel", bookingId))
            .andExpect(status().isNoContent());

        verify(bookingService).cancelBooking(bookingId);
    }
}
