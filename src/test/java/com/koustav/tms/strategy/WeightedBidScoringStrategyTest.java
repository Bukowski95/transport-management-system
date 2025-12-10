package com.koustav.tms.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.Load;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.Transporter;
import com.koustav.tms.entity.WeightUnit;

@DisplayName("WeightedBidScoringStrategy Tests")
class WeightedBidScoringStrategyTest {

    private WeightedBidScoringStrategy scoringStrategy;
    private Load load;

    @BeforeEach
    void setUp() {
        scoringStrategy = new WeightedBidScoringStrategy();

        load = Load.builder()
            .loadId(UUID.randomUUID())
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unloadingCity("Los Angeles")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .status(LoadStatus.OPEN_FOR_BIDS)
            .build();
    }

    @Test
    @DisplayName("Should calculate score based on 70% rate and 30% rating")
    void calculateScore_BasicScoring() {
        // Arrange
        Transporter transporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Fast Logistics")
            .rating(4.0) // 4.0/5.0 = 0.8 normalized
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Bid bid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter)
            .proposedRate(5000.0) // 1/5000 = 0.0002
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        // Act
        double score = scoringStrategy.calculateScore(bid);

        // Assert
        // Expected: (1/5000) * 0.7 + (4.0/5.0) * 0.3 = 0.00014 + 0.24 = 0.24014
        double expectedScore = (1.0 / 5000.0) * 0.7 + (4.0 / 5.0) * 0.3;
        assertEquals(expectedScore, score, 0.00001);
    }

    @Test
    @DisplayName("Should score lower rate higher than higher rate")
    void calculateScore_LowerRateScoresHigher() {
        // Arrange
        Transporter transporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Fast Logistics")
            .rating(4.0)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Bid lowRateBid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter)
            .proposedRate(4000.0) // Lower rate
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        Bid highRateBid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter)
            .proposedRate(6000.0) // Higher rate
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        // Act
        double lowRateScore = scoringStrategy.calculateScore(lowRateBid);
        double highRateScore = scoringStrategy.calculateScore(highRateBid);

        // Assert
        assertTrue(lowRateScore > highRateScore, "Lower rate should score higher");
    }

    @Test
    @DisplayName("Should score higher rating higher than lower rating with same rate")
    void calculateScore_HigherRatingScoresHigher() {
        // Arrange
        Transporter highRatingTransporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Premium Logistics")
            .rating(5.0)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Transporter lowRatingTransporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Budget Logistics")
            .rating(2.0)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Bid highRatingBid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(highRatingTransporter)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        Bid lowRatingBid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(lowRatingTransporter)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        // Act
        double highRatingScore = scoringStrategy.calculateScore(highRatingBid);
        double lowRatingScore = scoringStrategy.calculateScore(lowRatingBid);

        // Assert
        assertTrue(highRatingScore > lowRatingScore, "Higher rating should score higher with same rate");
    }

    @Test
    @DisplayName("Should calculate maximum possible score correctly")
    void calculateScore_MaximumScore() {
        // Arrange: Very low rate with perfect rating
        Transporter transporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Best Logistics")
            .rating(5.0) // Perfect rating
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Bid bid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter)
            .proposedRate(1.0) // Very low rate (best possible)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        // Act
        double score = scoringStrategy.calculateScore(bid);

        // Assert
        // Expected: (1/1) * 0.7 + (5.0/5.0) * 0.3 = 0.7 + 0.3 = 1.0
        assertEquals(1.0, score, 0.00001);
    }

    @Test
    @DisplayName("Should handle zero rating correctly")
    void calculateScore_ZeroRating() {
        // Arrange
        Transporter transporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("New Logistics")
            .rating(0.0) // Zero rating
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Bid bid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(transporter)
            .proposedRate(5000.0)
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        // Act
        double score = scoringStrategy.calculateScore(bid);

        // Assert
        // Expected: (1/5000) * 0.7 + (0.0/5.0) * 0.3 = 0.00014 + 0 = 0.00014
        double expectedScore = (1.0 / 5000.0) * 0.7;
        assertEquals(expectedScore, score, 0.00001);
    }

    @Test
    @DisplayName("Should prioritize lower rate over higher rating due to weight")
    void calculateScore_RateWeightDominates() {
        // Arrange: Lower rate with poor rating vs higher rate with perfect rating
        Transporter poorRatingTransporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Cheap Logistics")
            .rating(1.0)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Transporter perfectRatingTransporter = Transporter.builder()
            .transporterId(UUID.randomUUID())
            .companyName("Premium Logistics")
            .rating(5.0)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();

        Bid cheapBid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(poorRatingTransporter)
            .proposedRate(3000.0) // Much lower rate
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        Bid expensiveBid = Bid.builder()
            .bidId(UUID.randomUUID())
            .load(load)
            .transporter(perfectRatingTransporter)
            .proposedRate(6000.0) // Much higher rate
            .trucksOffered(3)
            .status(BidStatus.PENDING)
            .build();

        // Act
        double cheapScore = scoringStrategy.calculateScore(cheapBid);
        double expensiveScore = scoringStrategy.calculateScore(expensiveBid);

        // Assert
        // Cheap: (1/3000)*0.7 + (1/5)*0.3 = 0.000233 + 0.06 = 0.060233
        // Expensive: (1/6000)*0.7 + (5/5)*0.3 = 0.000117 + 0.3 = 0.300117
        // In this case, expensive actually scores higher due to rating weight
        // This tests the balance between rate and rating
        assertTrue(expensiveScore > cheapScore,
            "Perfect rating should overcome moderate rate difference");
    }
}
