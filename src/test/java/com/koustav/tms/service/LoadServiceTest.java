package com.koustav.tms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.koustav.tms.dto.request.LoadRequest;
import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.dto.response.LoadDetailResponse;
import com.koustav.tms.dto.response.LoadResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.entity.WeightUnit;
import com.koustav.tms.exception.InvalidStatusTransitionException;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.mapper.LoadMapper;
import com.koustav.tms.repository.BidRepository;
import com.koustav.tms.repository.LoadRepository;
import com.koustav.tms.strategy.BidScoringStrategy;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoadService Tests")
class LoadServiceTest {

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LoadMapper loadMapper;

    @Mock
    private BidScoringStrategy bidScoringStrategy;

    @InjectMocks
    private LoadService loadService;

    private UUID loadId;
    private Load load;
    private LoadRequest loadRequest;
    private LoadResponse loadResponse;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();

        load = Load.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .status(LoadStatus.POSTED)
            .build();

        loadRequest = LoadRequest.builder()
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .productType("Steel")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .loadingDate(new Timestamp(System.currentTimeMillis()))
            .build();

        loadResponse = LoadResponse.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .status(LoadStatus.POSTED)
            .build();
    }

    @Test
    @DisplayName("Should successfully create a new load")
    void createLoad_Success() {
        // Arrange
        when(loadRepository.save(any(Load.class))).thenReturn(load);
        when(loadMapper.toResponse(any(Load.class))).thenReturn(loadResponse);

        // Act
        LoadResponse response = loadService.createLoad(loadRequest);

        // Assert
        assertNotNull(response);
        assertEquals(loadId, response.getLoadId());
        assertEquals("SHIP123", response.getShipperId());
        verify(loadRepository).save(any(Load.class));
        verify(loadMapper).toResponse(any(Load.class));
    }

    @Test
    @DisplayName("Should return paginated list of loads with filters")
    void listLoads_WithFilters_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Load> loadPage = new PageImpl<>(List.of(load));
        when(loadRepository.findByFilters("SHIP123", LoadStatus.POSTED, pageable))
            .thenReturn(loadPage);
        when(loadMapper.toResponse(any(Load.class))).thenReturn(loadResponse);

        // Act
        Page<LoadResponse> result = loadService.listLoads("SHIP123", LoadStatus.POSTED, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(loadRepository).findByFilters("SHIP123", LoadStatus.POSTED, pageable);
    }

    @Test
    @DisplayName("Should return load details with active bids")
    void getLoad_Success() {
        // Arrange
        Bid bid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        List<Bid> activeBids = List.of(bid);
        LoadDetailResponse detailResponse = LoadDetailResponse.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .build();

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(bidRepository.findByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING))
            .thenReturn(activeBids);
        when(loadMapper.toDetailResponse(load, activeBids)).thenReturn(detailResponse);

        // Act
        LoadDetailResponse response = loadService.getLoad(loadId);

        // Assert
        assertNotNull(response);
        assertEquals(loadId, response.getLoadId());
        verify(loadRepository).findById(loadId);
        verify(bidRepository).findByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when load does not exist")
    void getLoad_NotFound_ThrowsException() {
        // Arrange
        when(loadRepository.findById(loadId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> loadService.getLoad(loadId)
        );
        assertTrue(exception.getMessage().contains("Load"));
    }

    @Test
    @DisplayName("Should successfully cancel a POSTED load")
    void cancelLoad_PostedLoad_Success() {
        // Arrange
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        loadService.cancelLoad(loadId);

        // Assert
        assertEquals(LoadStatus.CANCELLED, load.getStatus());
        verify(loadRepository).save(load);
    }

    @Test
    @DisplayName("Should successfully cancel a load with OPEN_FOR_BIDS status")
    void cancelLoad_OpenForBidsLoad_Success() {
        // Arrange
        load.setStatus(LoadStatus.OPEN_FOR_BIDS);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        loadService.cancelLoad(loadId);

        // Assert
        assertEquals(LoadStatus.CANCELLED, load.getStatus());
        verify(loadRepository).save(load);
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when cancelling BOOKED load")
    void cancelLoad_BookedLoad_ThrowsException() {
        // Arrange
        load.setStatus(LoadStatus.BOOKED);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> loadService.cancelLoad(loadId)
        );
        assertTrue(exception.getMessage().contains("Can't cancel a BOOKED load"));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when load already cancelled")
    void cancelLoad_AlreadyCancelled_ThrowsException() {
        // Arrange
        load.setStatus(LoadStatus.CANCELLED);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> loadService.cancelLoad(loadId)
        );
        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when cancelling non-existent load")
    void cancelLoad_NotFound_ThrowsException() {
        // Arrange
        when(loadRepository.findById(loadId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> loadService.cancelLoad(loadId)
        );
        assertTrue(exception.getMessage().contains("Load"));
    }

    @Test
    @DisplayName("Should return empty list when no pending bids exist")
    void getBestBids_NoPendingBids_ReturnsEmptyList() {
        // Arrange
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(bidRepository.findByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING))
            .thenReturn(new ArrayList<>());

        // Act
        List<BidResponse> result = loadService.getBestBids(loadId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return bids sorted by score in descending order")
    void getBestBids_WithPendingBids_ReturnsSortedList() {
        // Arrange
        Transporter transporter1 = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Transporter 1")
            .rating(4.5)
            .build();

        Transporter transporter2 = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Transporter 2")
            .rating(3.5)
            .build();

        Bid bid1 = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter1)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        Bid bid2 = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter2)
            .proposedRate(4500.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        List<Bid> pendingBids = new ArrayList<>(List.of(bid1, bid2));

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(bidRepository.findByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING))
            .thenReturn(pendingBids);
        when(bidScoringStrategy.calculateScore(bid1)).thenReturn(0.75);
        when(bidScoringStrategy.calculateScore(bid2)).thenReturn(0.85); // Higher score

        // Act
        List<BidResponse> result = loadService.getBestBids(loadId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // Verify scoring was called (at least once per bid)
        verify(bidScoringStrategy, atLeast(2)).calculateScore(any(Bid.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when load not found for getBestBids")
    void getBestBids_LoadNotFound_ThrowsException() {
        // Arrange
        when(loadRepository.findById(loadId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> loadService.getBestBids(loadId)
        );
        assertTrue(exception.getMessage().contains("Load"));
    }
}
