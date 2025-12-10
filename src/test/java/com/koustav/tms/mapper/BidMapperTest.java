package com.koustav.tms.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.entity.WeightUnit;

@DisplayName("BidMapper Tests")
class BidMapperTest {

    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private Bid bid;
    private Load load;
    private Transporter transporter;
    private Timestamp dateSubmitted;

    @BeforeEach
    void setUp() {
        bidId = UUID.randomUUID();
        loadId = UUID.randomUUID();
        transporterId = UUID.randomUUID();
        dateSubmitted = new Timestamp(System.currentTimeMillis());

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
            .dateSubmitted(dateSubmitted)
            .build();
    }

    @Test
    @DisplayName("Should correctly map Bid entity to BidResponse DTO")
    void toResponse_Success() {
        // Act
        BidResponse response = BidMapper.toResponse(bid);

        // Assert
        assertNotNull(response);
        assertEquals(bidId, response.getBidId());
        assertEquals(loadId, response.getLoadId());
        assertEquals(transporterId, response.getTransporterId());
        assertEquals("Fast Logistics", response.getTransporterName());
        assertEquals(4.5, response.getTransporterRating());
        assertEquals(5000.0, response.getProposedRate());
        assertEquals(3, response.getTrucksOffered());
        assertEquals(BidStatus.PENDING, response.getStatus());
        assertEquals(dateSubmitted, response.getDateSubmitted());
    }

    @Test
    @DisplayName("Should map bid with ACCEPTED status correctly")
    void toResponse_AcceptedStatus() {
        // Arrange
        bid.setStatus(BidStatus.ACCEPTED);

        // Act
        BidResponse response = BidMapper.toResponse(bid);

        // Assert
        assertEquals(BidStatus.ACCEPTED, response.getStatus());
    }

    @Test
    @DisplayName("Should map bid with REJECTED status correctly")
    void toResponse_RejectedStatus() {
        // Arrange
        bid.setStatus(BidStatus.REJECTED);

        // Act
        BidResponse response = BidMapper.toResponse(bid);

        // Assert
        assertEquals(BidStatus.REJECTED, response.getStatus());
    }
}
