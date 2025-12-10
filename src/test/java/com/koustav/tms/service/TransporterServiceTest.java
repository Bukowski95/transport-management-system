package com.koustav.tms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koustav.tms.dto.request.TransporterRequest;
import com.koustav.tms.dto.request.UpdateTrucksRequest;
import com.koustav.tms.dto.response.TransporterResponse;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.repository.TransporterRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransporterService Tests")
class TransporterServiceTest {

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private TransporterService transporterService;

    private UUID transporterId;
    private Transporter transporter;
    private TransporterRequest transporterRequest;
    private TransporterResponse transporterResponse;

    @BeforeEach
    void setUp() {
        transporterId = UUID.randomUUID();

        transporter = Transporter.builder()
            .transporterId(transporterId)
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 10, "Container", 5))
            .build();

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
    @DisplayName("Should successfully register a new transporter")
    void registerTransporter_Success() {
        // Arrange
        when(transporterRepository.save(any(Transporter.class))).thenReturn(transporter);

        // Act
        TransporterResponse response = transporterService.registerTransporter(transporterRequest);

        // Assert
        assertNotNull(response);
        assertEquals(transporterId, response.getTransporterId());
        assertEquals("Fast Logistics", response.getCompanyName());
        assertEquals(4.5, response.getRating());
        verify(transporterRepository).save(any(Transporter.class));
    }

    @Test
    @DisplayName("Should successfully retrieve transporter by ID")
    void getTransporter_Success() {
        // Arrange
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(transporter));

        // Act
        TransporterResponse response = transporterService.getTransporter(transporterId);

        // Assert
        assertNotNull(response);
        assertEquals(transporterId, response.getTransporterId());
        assertEquals("Fast Logistics", response.getCompanyName());
        verify(transporterRepository).findById(transporterId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when transporter does not exist")
    void getTransporter_NotFound_ThrowsException() {
        // Arrange
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> transporterService.getTransporter(transporterId)
        );
        assertTrue(exception.getMessage().contains("Transporter"));
    }

    @Test
    @DisplayName("Should successfully update transporter truck availability")
    void updateTrucks_Success() {
        // Arrange
        UpdateTrucksRequest updateRequest = UpdateTrucksRequest.builder()
            .availableTrucks(Map.of("Flatbed", 15, "Container", 8))
            .build();

        Transporter updatedTransporter = Transporter.builder()
            .transporterId(transporterId)
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 15, "Container", 8))
            .build();

        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(transporter));
        when(transporterRepository.save(any(Transporter.class))).thenReturn(updatedTransporter);

        // Act
        TransporterResponse response = transporterService.updateTrucks(transporterId, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(15, response.getAvailableTrucks().get("Flatbed"));
        assertEquals(8, response.getAvailableTrucks().get("Container"));
        verify(transporterRepository).findById(transporterId);
        verify(transporterRepository).save(transporter);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent transporter")
    void updateTrucks_NotFound_ThrowsException() {
        // Arrange
        UpdateTrucksRequest updateRequest = UpdateTrucksRequest.builder()
            .availableTrucks(Map.of("Flatbed", 15))
            .build();

        when(transporterRepository.findById(transporterId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> transporterService.updateTrucks(transporterId, updateRequest)
        );
        assertTrue(exception.getMessage().contains("Transporter"));
    }
}
