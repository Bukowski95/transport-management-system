package com.koustav.tms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.koustav.tms.dto.request.BidRequest;
import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.entity.WeightUnit;
import com.koustav.tms.exception.InsufficientCapacityException;
import com.koustav.tms.exception.InvalidStatusTransitionException;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.repository.BidRepository;
import com.koustav.tms.repository.LoadRepository;
import com.koustav.tms.repository.TransporterRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidService Tests")
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private BidService bidService;

    private UUID loadId;
    private UUID transporterId;
    private UUID bidId;
    private Load load;
    private Transporter transporter;
    private Bid bid;

    @BeforeEach
    void setUp() {
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bidId = UUID.randomUUID();

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

        transporter = Transporter.builder()
            .transporterId(transporterId)
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        bid = Bid.builder()
            .bidId(bidId)
            .load(load)
            .transporter(transporter)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .dateSubmitted(new Timestamp(System.currentTimeMillis()))
            .build();
    }

    @Test
    @DisplayName("Should successfully submit a bid and update load status to OPEN_FOR_BIDS")
    void submitBid_Success() {
        // Arrange
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(5000.0);
        request.setTrucksOffered(3);

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(transporter));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        BidResponse response = bidService.submitBid(request);

        // Assert
        assertNotNull(response);
        assertEquals(bidId, response.getBidId());
        assertEquals(BidStatus.PENDING, response.getStatus());
        verify(bidRepository).save(any(Bid.class));
        verify(loadRepository).save(load);
        assertEquals(LoadStatus.OPEN_FOR_BIDS, load.getStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when load does not exist")
    void submitBid_LoadNotFound_ThrowsException() {
        // Arrange
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(5000.0);
        request.setTrucksOffered(3);

        when(loadRepository.findById(loadId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bidService.submitBid(request)
        );
        assertTrue(exception.getMessage().contains("Load"));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when transporter does not exist")
    void submitBid_TransporterNotFound_ThrowsException() {
        // Arrange
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(5000.0);
        request.setTrucksOffered(3);

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bidService.submitBid(request)
        );
        assertTrue(exception.getMessage().contains("Transporter"));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when trying to bid on a BOOKED load")
    void submitBid_LoadBooked_ThrowsException() {
        // Arrange
        load.setStatus(LoadStatus.BOOKED);
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(5000.0);
        request.setTrucksOffered(3);

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> bidService.submitBid(request)
        );
        assertTrue(exception.getMessage().contains("Can't bid on a load with status"));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when trying to bid on a CANCELLED load")
    void submitBid_LoadCancelled_ThrowsException() {
        // Arrange
        load.setStatus(LoadStatus.CANCELLED);
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(5000.0);
        request.setTrucksOffered(3);

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> bidService.submitBid(request)
        );
        assertTrue(exception.getMessage().contains("Can't bid on a load with status"));
    }

    @Test
    @DisplayName("Should throw InsufficientCapacityException when transporter doesn't have enough trucks")
    void submitBid_InsufficientCapacity_ThrowsException() {
        // Arrange
        transporter.setAvailableTrucks(Map.of("Flatbed", 2)); // Less than requested
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(5000.0);
        request.setTrucksOffered(3);

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(transporter));

        // Act & Assert
        InsufficientCapacityException exception = assertThrows(
            InsufficientCapacityException.class,
            () -> bidService.submitBid(request)
        );
        assertTrue(exception.getMessage().contains("doesn't have"));
    }

    @Test
    @DisplayName("Should not change load status when load is already OPEN_FOR_BIDS")
    void submitBid_LoadAlreadyOpenForBids_DoesNotChangeStatus() {
        // Arrange
        load.setStatus(LoadStatus.OPEN_FOR_BIDS);
        BidRequest request = new BidRequest();
        request.setLoadId(loadId);
        request.setTransporterId(transporterId);
        request.setProposedRate(5000.0);
        request.setTrucksOffered(3);

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(transporterRepository.findById(transporterId)).thenReturn(Optional.of(transporter));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);

        // Act
        bidService.submitBid(request);

        // Assert
        verify(loadRepository, never()).save(load);
        assertEquals(LoadStatus.OPEN_FOR_BIDS, load.getStatus());
    }

    @Test
    @DisplayName("Should return filtered page of bids when filters are applied")
    void getBids_WithFilters_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Bid> bidPage = new PageImpl<>(java.util.List.of(bid));

        when(bidRepository.findByFilters(loadId, transporterId, BidStatus.PENDING, pageable))
            .thenReturn(bidPage);

        // Act
        Page<BidResponse> result = bidService.getBids(loadId, transporterId, BidStatus.PENDING, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(bidRepository).findByFilters(loadId, transporterId, BidStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("Should return bid details when bid exists")
    void getBid_Success() {
        // Arrange
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        // Act
        BidResponse response = bidService.getBid(bidId);

        // Assert
        assertNotNull(response);
        assertEquals(bidId, response.getBidId());
        verify(bidRepository).findById(bidId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when bid does not exist")
    void getBid_NotFound_ThrowsException() {
        // Arrange
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bidService.getBid(bidId)
        );
        assertTrue(exception.getMessage().contains("Bid"));
    }

    @Test
    @DisplayName("Should successfully reject a pending bid")
    void rejectBid_Success() {
        // Arrange
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);

        // Act
        bidService.rejectBid(bidId);

        // Assert
        assertEquals(BidStatus.REJECTED, bid.getStatus());
        verify(bidRepository).save(bid);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when bid to reject does not exist")
    void rejectBid_NotFound_ThrowsException() {
        // Arrange
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bidService.rejectBid(bidId)
        );
        assertTrue(exception.getMessage().contains("Bid"));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when trying to reject an accepted bid")
    void rejectBid_AlreadyAccepted_ThrowsException() {
        // Arrange
        bid.setStatus(BidStatus.ACCEPTED);
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> bidService.rejectBid(bidId)
        );
        assertTrue(exception.getMessage().contains("can only reject PENDING bids"));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when trying to reject an already rejected bid")
    void rejectBid_AlreadyRejected_ThrowsException() {
        // Arrange
        bid.setStatus(BidStatus.REJECTED);
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> bidService.rejectBid(bidId)
        );
        assertTrue(exception.getMessage().contains("can only reject PENDING bids"));
    }
}
