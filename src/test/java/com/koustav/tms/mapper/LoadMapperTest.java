package com.koustav.tms.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koustav.tms.dto.response.LoadDetailResponse;
import com.koustav.tms.dto.response.LoadResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.BookingStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.entity.WeightUnit;
import com.koustav.tms.repository.BookingRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoadMapper Tests")
class LoadMapperTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private LoadMapper loadMapper;

    private UUID loadId;
    private Load load;
    private Timestamp loadingDate;
    private Timestamp datePosted;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        loadingDate = new Timestamp(System.currentTimeMillis() + 86400000); // Tomorrow
        datePosted = new Timestamp(System.currentTimeMillis());

        load = Load.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .loadingDate(loadingDate)
            .productType("Steel")
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .truckType("Flatbed")
            .noOfTrucks(5)
            .status(LoadStatus.POSTED)
            .version(1L)
            .datePosted(datePosted)
            .build();
    }

    @Test
    @DisplayName("Should correctly map Load entity to LoadResponse DTO")
    void toResponse_Success() {
        // Act
        LoadResponse response = loadMapper.toResponse(load);

        // Assert
        assertNotNull(response);
        assertEquals(loadId, response.getLoadId());
        assertEquals("SHIP123", response.getShipperId());
        assertEquals("New York", response.getLoadingCity());
        assertEquals("Los Angeles", response.getUnloadingCity());
        assertEquals(loadingDate, response.getLoadingDate());
        assertEquals("Steel", response.getProductType());
        assertEquals(10000.0, response.getWeight());
        assertEquals(WeightUnit.KG, response.getWeightUnit());
        assertEquals("Flatbed", response.getTruckType());
        assertEquals(5, response.getNoOfTrucks());
        assertEquals(LoadStatus.POSTED, response.getStatus());
        assertEquals(1L, response.getVersion());
        assertEquals(datePosted, response.getDatePosted());
    }

    @Test
    @DisplayName("Should map load with different statuses correctly")
    void toResponse_DifferentStatus() {
        // Arrange
        load.setStatus(LoadStatus.OPEN_FOR_BIDS);

        // Act
        LoadResponse response = loadMapper.toResponse(load);

        // Assert
        assertEquals(LoadStatus.OPEN_FOR_BIDS, response.getStatus());
    }

    @Test
    @DisplayName("Should correctly map Load with active bids to LoadDetailResponse")
    void toDetailResponse_WithActiveBids_Success() {
        // Arrange
        Transporter transporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Bid bid1 = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        Bid bid2 = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter)
            .proposedRate(4500.0)
            .trucksOffered(2)
            .status(BidStatus.PENDING)
            .build();

        List<Bid> activeBids = List.of(bid1, bid2);

        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(2); // 2 trucks already allocated

        // Act
        LoadDetailResponse response = loadMapper.toDetailResponse(load, activeBids);

        // Assert
        assertNotNull(response);
        assertEquals(loadId, response.getLoadId());
        assertEquals("SHIP123", response.getShipperId());
        assertEquals(5, response.getNoOfTrucks());
        assertEquals(3, response.getRemainingTrucks()); // 5 - 2 = 3
        assertNotNull(response.getActiveBids());
        assertEquals(2, response.getActiveBids().size());
        verify(bookingRepository).sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should calculate remaining trucks correctly when no bookings exist")
    void toDetailResponse_NoBookings_Success() {
        // Arrange
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(null); // No bookings

        // Act
        LoadDetailResponse response = loadMapper.toDetailResponse(load, List.of());

        // Assert
        assertEquals(5, response.getRemainingTrucks()); // All 5 trucks available
        assertTrue(response.getActiveBids().isEmpty());
    }

    @Test
    @DisplayName("Should calculate remaining trucks correctly when load is fully booked")
    void toDetailResponse_FullyBooked_Success() {
        // Arrange
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(5); // All trucks allocated

        // Act
        LoadDetailResponse response = loadMapper.toDetailResponse(load, List.of());

        // Assert
        assertEquals(0, response.getRemainingTrucks()); // No trucks remaining
    }

    @Test
    @DisplayName("Should map load with empty active bids list correctly")
    void toDetailResponse_EmptyBidsList_Success() {
        // Arrange
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(0);

        // Act
        LoadDetailResponse response = loadMapper.toDetailResponse(load, List.of());

        // Assert
        assertNotNull(response.getActiveBids());
        assertTrue(response.getActiveBids().isEmpty());
        assertEquals(5, response.getRemainingTrucks());
    }
}
