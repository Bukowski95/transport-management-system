package com.koustav.tms.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.koustav.tms.dto.response.BookingResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Booking;
import com.koustav.tms.entity.BookingStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.entity.WeightUnit;

@DisplayName("BookingMapper Tests")
class BookingMapperTest {

    private UUID bookingId;
    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private Booking booking;
    private Bid bid;
    private Load load;
    private Transporter transporter;
    private Timestamp bookedAt;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        bidId = UUID.randomUUID();
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        bookedAt = new Timestamp(System.currentTimeMillis());

        load = Load.builder()
            .loadId(loadId)
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .status(LoadStatus.BOOKED)
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
            .status(BidStatus.ACCEPTED)
            .build();

        booking = Booking.builder()
            .bookingId(bookingId)
            .bid(bid)
            .load(load)
            .transporter(transporter)
            .allocatedTrucks(3)
            .finalRate(5000.0)
            .status(BookingStatus.CONFIRMED)
            .bookedAt(bookedAt)
            .build();
    }

    @Test
    @DisplayName("Should correctly map Booking entity to BookingResponse DTO")
    void toResponse_Success() {
        // Act
        BookingResponse response = BookingMapper.toResponse(booking);

        // Assert
        assertNotNull(response);
        assertEquals(bookingId, response.getBookingId());
        assertEquals(bidId, response.getBidId());
        assertEquals(loadId, response.getLoadId());
        assertEquals(transporterId, response.getTransporterId());
        assertEquals("Fast Logistics", response.getTransporterName());
        assertEquals(3, response.getAllocatedTrucks());
        assertEquals(5000.0, response.getFinalRate());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        assertEquals(bookedAt, response.getBookedAt());
    }

    @Test
    @DisplayName("Should map booking with CANCELLED status correctly")
    void toResponse_CancelledStatus() {
        // Arrange
        booking.setStatus(BookingStatus.CANCELLED);

        // Act
        BookingResponse response = BookingMapper.toResponse(booking);

        // Assert
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
    }
}
