package com.koustav.tms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.koustav.tms.dto.request.BookingRequest;
import com.koustav.tms.dto.response.BookingResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Booking;
import com.koustav.tms.entity.BookingStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.entity.WeightUnit;
import com.koustav.tms.exception.ConflictException;
import com.koustav.tms.exception.InsufficientCapacityException;
import com.koustav.tms.exception.InvalidStatusTransitionException;
import com.koustav.tms.exception.ResourceNotFoundException;
import com.koustav.tms.repository.BidRepository;
import com.koustav.tms.repository.BookingRepository;
import com.koustav.tms.repository.LoadRepository;
import com.koustav.tms.repository.TransporterRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LoadRepository loadRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @Mock
    private BidTransactionService bidTransactionService;

    @InjectMocks
    private BookingService bookingService;

    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private UUID bookingId;
    private Bid bid;
    private Load load;
    private Transporter transporter;
    private Booking booking;

    @BeforeEach
    void setUp() {
        bidId = UUID.randomUUID();
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        load = Load.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .status(LoadStatus.OPEN_FOR_BIDS)
            .build();

        Map<String, Integer> trucks = new java.util.HashMap<>();
        trucks.put("Flatbed", 10);

        transporter = Transporter.builder()
            .transporterId(transporterId)
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(trucks)
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

        booking = Booking.builder()
            .bookingId(bookingId)
            .load(load)
            .bid(bid)
            .transporter(transporter)
            .allocatedTrucks(3)
            .finalRate(5000.0)
            .status(BookingStatus.CONFIRMED)
            .bookedAt(new Timestamp(System.currentTimeMillis()))
            .build();
    }

    @Test
    @DisplayName("Should successfully accept a bid and create booking")
    void acceptBid_Success() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setBidId(bidId);

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(0);
        when(transporterRepository.save(any(Transporter.class))).thenReturn(transporter);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        BookingResponse response = bookingService.acceptBid(request);

        // Assert
        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        verify(bookingRepository).save(any(Booking.class));
        verify(transporterRepository).save(transporter);
        assertEquals(BidStatus.ACCEPTED, bid.getStatus());
        assertEquals(7, transporter.getAvailableTrucks().get("Flatbed")); // 10 - 3 = 7
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when bid does not exist")
    void acceptBid_BidNotFound_ThrowsException() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setBidId(bidId);

        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bookingService.acceptBid(request)
        );
        assertTrue(exception.getMessage().contains("Bid"));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when bid is not pending")
    void acceptBid_BidNotPending_ThrowsException() {
        // Arrange
        bid.setStatus(BidStatus.ACCEPTED);
        BookingRequest request = new BookingRequest();
        request.setBidId(bidId);

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> bookingService.acceptBid(request)
        );
        assertTrue(exception.getMessage().contains("Can only accept PENDING bids"));
    }

    @Test
    @DisplayName("Should throw InsufficientCapacityException when load capacity exceeded")
    void acceptBid_InsufficientLoadCapacity_ThrowsException() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setBidId(bidId);

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(4); // 4 already allocated, bid offers 3, but load needs only 5 total

        // Act & Assert
        InsufficientCapacityException exception = assertThrows(
            InsufficientCapacityException.class,
            () -> bookingService.acceptBid(request)
        );
        assertTrue(exception.getMessage().contains("Load only needs"));
    }

    @Test
    @DisplayName("Should reject bid and throw exception when transporter lacks capacity")
    void acceptBid_InsufficientTransporterCapacity_RejectsBidAndThrowsException() {
        // Arrange
        Map<String, Integer> limitedTrucks = new java.util.HashMap<>();
        limitedTrucks.put("Flatbed", 2);
        transporter.setAvailableTrucks(limitedTrucks); // Less than bid offers
        BookingRequest request = new BookingRequest();
        request.setBidId(bidId);

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(0);

        // Act & Assert
        InsufficientCapacityException exception = assertThrows(
            InsufficientCapacityException.class,
            () -> bookingService.acceptBid(request)
        );
        assertTrue(exception.getMessage().contains("Transporter no longer has sufficient"));
        verify(bidTransactionService).rejectBidInNewTransaction(bidId);
    }

    @Test
    @DisplayName("Should update load status to BOOKED when fully booked")
    void acceptBid_FullyBooked_UpdatesLoadStatus() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setBidId(bidId);
        bid.setTrucksOffered(5); // This will fill the load completely

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(0);
        when(transporterRepository.save(any(Transporter.class))).thenReturn(transporter);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bidRepository.save(any(Bid.class))).thenReturn(bid);
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        bookingService.acceptBid(request);

        // Assert
        assertEquals(LoadStatus.BOOKED, load.getStatus());
        verify(loadRepository).save(load);
    }

    @Test
    @DisplayName("Should throw ConflictException when OptimisticLockException occurs")
    void acceptBid_OptimisticLock_ThrowsConflictException() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setBidId(bidId);

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(0);
        when(transporterRepository.save(any(Transporter.class)))
            .thenThrow(new OptimisticLockException());

        // Act & Assert
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> bookingService.acceptBid(request)
        );
        assertTrue(exception.getMessage().contains("Another transaction modified"));
    }

    @Test
    @DisplayName("Should successfully retrieve booking by ID")
    void getBooking_Success() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act
        BookingResponse response = bookingService.getBooking(bookingId);

        // Assert
        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        verify(bookingRepository).findById(bookingId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when booking does not exist")
    void getBooking_NotFound_ThrowsException() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bookingService.getBooking(bookingId)
        );
        assertTrue(exception.getMessage().contains("Booking"));
    }

    @Test
    @DisplayName("Should successfully cancel booking and restore trucks")
    void cancelBooking_Success() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(transporterRepository.save(any(Transporter.class))).thenReturn(transporter);
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(0); // All bookings cancelled
        when(bidRepository.countByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING))
            .thenReturn(2L);
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        bookingService.cancelBooking(bookingId);

        // Assert
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(13, transporter.getAvailableTrucks().get("Flatbed")); // 10 + 3 = 13
        verify(bookingRepository).save(booking);
        verify(transporterRepository).save(transporter);
        verify(loadRepository).save(load);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when cancelling non-existent booking")
    void cancelBooking_NotFound_ThrowsException() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> bookingService.cancelBooking(bookingId)
        );
        assertTrue(exception.getMessage().contains("Booking"));
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when booking already cancelled")
    void cancelBooking_AlreadyCancelled_ThrowsException() {
        // Arrange
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act & Assert
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> bookingService.cancelBooking(bookingId)
        );
        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    @DisplayName("Should update load status to POSTED when all bookings cancelled and no pending bids")
    void cancelBooking_NoPendingBids_UpdatesLoadToPosted() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(transporterRepository.save(any(Transporter.class))).thenReturn(transporter);
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(0);
        when(bidRepository.countByLoad_LoadIdAndStatus(loadId, BidStatus.PENDING))
            .thenReturn(0L); // No pending bids
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        bookingService.cancelBooking(bookingId);

        // Assert
        assertEquals(LoadStatus.POSTED, load.getStatus());
    }

    @Test
    @DisplayName("Should update load status to OPEN_FOR_BIDS when partial cancellation")
    void cancelBooking_PartialCancellation_UpdatesLoadToOpenForBids() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(transporterRepository.save(any(Transporter.class))).thenReturn(transporter);
        when(bookingRepository.sumAllocatedTrucksByLoadIdAndStatus(loadId, BookingStatus.CONFIRMED))
            .thenReturn(2); // Still 2 trucks allocated
        when(loadRepository.save(any(Load.class))).thenReturn(load);

        // Act
        bookingService.cancelBooking(bookingId);

        // Assert
        assertEquals(LoadStatus.OPEN_FOR_BIDS, load.getStatus());
    }
}
