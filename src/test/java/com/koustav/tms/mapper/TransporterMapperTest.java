package com.koustav.tms.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.koustav.tms.dto.response.TransporterResponse;
import com.koustav.tms.entity.Transporter;

@DisplayName("TransporterMapper Tests")
class TransporterMapperTest {

    private UUID transporterId;
    private Transporter transporter;

    @BeforeEach
    void setUp() {
        transporterId = UUID.randomUUID();

        transporter = Transporter.builder()
            .transporterId(transporterId)
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 10, "Container", 5, "Tanker", 8))
            .build();
    }

    @Test
    @DisplayName("Should correctly map Transporter entity to TransporterResponse DTO")
    void toResponse_Success() {
        // Act
        TransporterResponse response = TransporterMapper.toResponse(transporter);

        // Assert
        assertNotNull(response);
        assertEquals(transporterId, response.getTransporterId());
        assertEquals("Fast Logistics", response.getCompanyName());
        assertEquals(4.5, response.getRating());
        assertNotNull(response.getAvailableTrucks());
        assertEquals(3, response.getAvailableTrucks().size());
        assertEquals(10, response.getAvailableTrucks().get("Flatbed"));
        assertEquals(5, response.getAvailableTrucks().get("Container"));
        assertEquals(8, response.getAvailableTrucks().get("Tanker"));
    }

    @Test
    @DisplayName("Should map transporter with empty truck map correctly")
    void toResponse_EmptyTruckMap() {
        // Arrange
        transporter.setAvailableTrucks(Map.of());

        // Act
        TransporterResponse response = TransporterMapper.toResponse(transporter);

        // Assert
        assertNotNull(response.getAvailableTrucks());
        assertTrue(response.getAvailableTrucks().isEmpty());
    }

    @Test
    @DisplayName("Should map transporter with different rating correctly")
    void toResponse_DifferentRating() {
        // Arrange
        transporter.setRating(3.8);

        // Act
        TransporterResponse response = TransporterMapper.toResponse(transporter);

        // Assert
        assertEquals(3.8, response.getRating());
    }
}
