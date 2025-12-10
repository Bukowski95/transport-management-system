package com.koustav.tms.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
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
import com.koustav.tms.dto.request.TransporterRequest;
import com.koustav.tms.dto.request.UpdateTrucksRequest;
import com.koustav.tms.dto.response.TransporterResponse;
import com.koustav.tms.service.TransporterService;

@WebMvcTest(TransporterController.class)
@DisplayName("TransporterController Tests")
class TransporterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransporterService transporterService;

    private UUID transporterId;
    private TransporterRequest transporterRequest;
    private TransporterResponse transporterResponse;

    @BeforeEach
    void setUp() {
        transporterId = UUID.randomUUID();

        transporterRequest = TransporterRequest.builder()
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 10, "Container", 5))
            .build();

        transporterResponse = TransporterResponse.builder()
            .transporterId(transporterId)
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 10, "Container", 5))
            .build();
    }

    @Test
    @DisplayName("Should register transporter and return 201 CREATED")
    void registerTransporter_Success() throws Exception {
        // Arrange
        when(transporterService.registerTransporter(any(TransporterRequest.class)))
            .thenReturn(transporterResponse);

        // Act & Assert
        mockMvc.perform(post("/transporter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transporterRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transporterId").value(transporterId.toString()))
            .andExpect(jsonPath("$.companyName").value("Fast Logistics"));

        verify(transporterService).registerTransporter(any(TransporterRequest.class));
    }

    @Test
    @DisplayName("Should get transporter by ID and return 200 OK")
    void getTransporter_Success() throws Exception {
        // Arrange
        when(transporterService.getTransporter(transporterId)).thenReturn(transporterResponse);

        // Act & Assert
        mockMvc.perform(get("/transporter/{transporterId}", transporterId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transporterId").value(transporterId.toString()))
            .andExpect(jsonPath("$.rating").value(4.5));

        verify(transporterService).getTransporter(transporterId);
    }

    @Test
    @DisplayName("Should update truck availability and return 200 OK")
    void updateTrucks_Success() throws Exception {
        // Arrange
        UpdateTrucksRequest updateRequest = UpdateTrucksRequest.builder()
            .availableTrucks(Map.of("Flatbed", 15, "Container", 8))
            .build();

        TransporterResponse updatedResponse = TransporterResponse.builder()
            .transporterId(transporterId)
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 15, "Container", 8))
            .build();

        when(transporterService.updateTrucks(eq(transporterId), any(UpdateTrucksRequest.class)))
            .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/transporter/{transporterId}/trucks", transporterId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableTrucks.Flatbed").value(15))
            .andExpect(jsonPath("$.availableTrucks.Container").value(8));

        verify(transporterService).updateTrucks(eq(transporterId), any(UpdateTrucksRequest.class));
    }
}
