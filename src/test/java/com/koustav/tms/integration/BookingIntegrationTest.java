package com.koustav.tms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.koustav.tms.dto.request.BidRequest;
import com.koustav.tms.dto.request.BookingRequest;
import com.koustav.tms.dto.request.LoadRequest;
import com.koustav.tms.dto.request.TransporterRequest;
import com.koustav.tms.dto.response.BidResponse;
import com.koustav.tms.dto.response.BookingResponse;
import com.koustav.tms.dto.response.LoadResponse;
import com.koustav.tms.dto.response.TransporterResponse;
import com.koustav.tms.entity.BidStatus;
import com.koustav.tms.entity.BookingStatus;
import com.koustav.tms.entity.LoadStatus;
import com.koustav.tms.entity.WeightUnit;
import com.koustav.tms.exception.ConflictException;
import com.koustav.tms.exception.InsufficientCapacityException;
import com.koustav.tms.repository.BidRepository;
import com.koustav.tms.repository.BookingRepository;
import com.koustav.tms.repository.LoadRepository;
import com.koustav.tms.repository.TransporterRepository;
import com.koustav.tms.service.BidService;
import com.koustav.tms.service.BookingService;
import com.koustav.tms.service.LoadService;
import com.koustav.tms.service.TransporterService;

/**
 * Integration test for the complete booking workflow.
 * Tests end-to-end scenarios including concurrency and optimistic locking.
 *
 * This test uses @SpringBootTest to load the full application context
 * and test with real database operations (with transactions rolled back after each test).
 */
@SpringBootTest
@DisplayName("Booking Integration Tests")
@org.junit.jupiter.api.Disabled("Integration tests require database setup - enable when DB is configured")
class BookingIntegrationTest {

    @Autowired
    private LoadService loadService;

    @Autowired
    private TransporterService transporterService;

    @Autowired
    private BidService bidService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private LoadRepository loadRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        // Clean up before each test
        bookingRepository.deleteAll();
        bidRepository.deleteAll();
        loadRepository.deleteAll();
        transporterRepository.deleteAll();
    }

    @Test
    @DisplayName("Should complete full booking workflow successfully")
    @Transactional
    void completeBookingWorkflow_Success() {
        // Step 1: Register a transporter
        TransporterRequest transporterRequest = TransporterRequest.builder()
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 10))
            .build();
        TransporterResponse transporter = transporterService.registerTransporter(transporterRequest);
        assertNotNull(transporter.getTransporterId());

        // Step 2: Create a load
        LoadRequest loadRequest = LoadRequest.builder()
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unLoadingCity("Los Angeles")
            .productType("Steel")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .loadingDate(new Timestamp(System.currentTimeMillis() + 86400000))
            .build();
        LoadResponse load = loadService.createLoad(loadRequest);
        assertNotNull(load.getLoadId());
        assertEquals(LoadStatus.POSTED, load.getStatus());

        // Step 3: Submit a bid
        BidRequest bidRequest = new BidRequest();
        bidRequest.setLoadId(load.getLoadId());
        bidRequest.setTransporterId(transporter.getTransporterId());
        bidRequest.setProposedRate(5000.0);
        bidRequest.setTrucksOffered(3);
        BidResponse bid = bidService.submitBid(bidRequest);
        assertNotNull(bid.getBidId());
        assertEquals(BidStatus.PENDING, bid.getStatus());

        // Step 4: Accept the bid (create booking)
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setBidId(bid.getBidId());
        BookingResponse booking = bookingService.acceptBid(bookingRequest);
        assertNotNull(booking.getBookingId());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(3, booking.getAllocatedTrucks());

        // Step 5: Verify state changes
        // - Transporter trucks should be deducted
        TransporterResponse updatedTransporter = transporterService.getTransporter(transporter.getTransporterId());
        assertEquals(7, updatedTransporter.getAvailableTrucks().get("Flatbed")); // 10 - 3 = 7

        // - Load status should remain OPEN_FOR_BIDS (still needs 2 more trucks)
        LoadResponse updatedLoad = loadService.listLoads(load.getShipperId(), null, null)
            .getContent().get(0);
        assertEquals(LoadStatus.OPEN_FOR_BIDS, updatedLoad.getStatus());
    }

    @Test
    @DisplayName("Should handle concurrent booking attempts with optimistic locking")
    @Transactional
    void concurrentBooking_OptimisticLocking() throws InterruptedException {
        // Setup: Create load and 2 transporters with limited capacity
        LoadRequest loadRequest = LoadRequest.builder()
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unLoadingCity("Los Angeles")
            .productType("Steel")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .loadingDate(new Timestamp(System.currentTimeMillis() + 86400000))
            .build();
        LoadResponse load = loadService.createLoad(loadRequest);

        // Transporter 1 with only 3 trucks
        TransporterRequest transporter1Request = TransporterRequest.builder()
            .companyName("Transporter 1")
            .rating(4.0)
            .availableTrucks(new HashMap<>(Map.of("Flatbed", 3)))
            .build();
        TransporterResponse transporter1 = transporterService.registerTransporter(transporter1Request);

        // Both bid for 3 trucks each
        BidRequest bidRequest1 = new BidRequest();
        bidRequest1.setLoadId(load.getLoadId());
        bidRequest1.setTransporterId(transporter1.getTransporterId());
        bidRequest1.setProposedRate(5000.0);
        bidRequest1.setTrucksOffered(3);
        BidResponse bid1 = bidService.submitBid(bidRequest1);

        // Try to accept the same bid concurrently (simulating race condition)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    BookingRequest bookingRequest = new BookingRequest();
                    bookingRequest.setBidId(bid1.getBidId());
                    bookingService.acceptBid(bookingRequest);
                    successCount.incrementAndGet();
                } catch (ConflictException | IllegalStateException e) {
                    conflictCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Verify: Only one booking should succeed, others should fail due to optimistic locking
        // Note: This test demonstrates the concept but may not always trigger the race condition
        assertTrue(successCount.get() <= 1, "At most one booking should succeed");
    }

    @Test
    @DisplayName("Should fully book a load and update status to BOOKED")
    @Transactional
    void fullyBookLoad_UpdatesStatusToBooked() {
        // Create load needing 5 trucks
        LoadRequest loadRequest = LoadRequest.builder()
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unLoadingCity("Los Angeles")
            .productType("Steel")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .loadingDate(new Timestamp(System.currentTimeMillis() + 86400000))
            .build();
        LoadResponse load = loadService.createLoad(loadRequest);

        // Create transporter
        TransporterRequest transporterRequest = TransporterRequest.builder()
            .companyName("Large Fleet")
            .rating(4.5)
            .availableTrucks(Map.of("Flatbed", 20))
            .build();
        TransporterResponse transporter = transporterService.registerTransporter(transporterRequest);

        // Submit bid for all 5 trucks
        BidRequest bidRequest = new BidRequest();
        bidRequest.setLoadId(load.getLoadId());
        bidRequest.setTransporterId(transporter.getTransporterId());
        bidRequest.setProposedRate(5000.0);
        bidRequest.setTrucksOffered(5);
        BidResponse bid = bidService.submitBid(bidRequest);

        // Accept bid
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setBidId(bid.getBidId());
        bookingService.acceptBid(bookingRequest);

        // Verify load is fully booked
        LoadResponse updatedLoad = loadService.listLoads(load.getShipperId(), null, null)
            .getContent().get(0);
        assertEquals(LoadStatus.BOOKED, updatedLoad.getStatus());
    }

    @Test
    @DisplayName("Should prevent overbooking and reject bid automatically")
    @Transactional
    void preventOverbooking_RejectsBidAutomatically() {
        // Create load and transporter
        LoadRequest loadRequest = LoadRequest.builder()
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unLoadingCity("Los Angeles")
            .productType("Steel")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .loadingDate(new Timestamp(System.currentTimeMillis() + 86400000))
            .build();
        LoadResponse load = loadService.createLoad(loadRequest);

        // Transporter with only 3 trucks
        TransporterRequest transporterRequest = TransporterRequest.builder()
            .companyName("Small Fleet")
            .rating(4.0)
            .availableTrucks(new HashMap<>(Map.of("Flatbed", 3)))
            .build();
        TransporterResponse transporter = transporterService.registerTransporter(transporterRequest);

        // Submit bid for 5 trucks (more than available)
        BidRequest bidRequest = new BidRequest();
        bidRequest.setLoadId(load.getLoadId());
        bidRequest.setTransporterId(transporter.getTransporterId());
        bidRequest.setProposedRate(5000.0);
        bidRequest.setTrucksOffered(5);
        BidResponse bid = bidService.submitBid(bidRequest);

        // Manually deduct 2 trucks to simulate another booking
        UUID transporterId = transporter.getTransporterId();
        var transporterEntity = transporterRepository.findById(transporterId).orElseThrow();
        transporterEntity.deductTrucks("Flatbed", 2);
        transporterRepository.save(transporterEntity);

        // Try to accept bid (should fail because only 1 truck remains, but bid offers 5)
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setBidId(bid.getBidId());

        assertThrows(InsufficientCapacityException.class, () -> {
            bookingService.acceptBid(bookingRequest);
        });

        // Verify bid was rejected
        BidResponse rejectedBid = bidService.getBid(bid.getBidId());
        assertEquals(BidStatus.REJECTED, rejectedBid.getStatus());
    }

    @Test
    @DisplayName("Should cancel booking and restore transporter trucks")
    @Transactional
    void cancelBooking_RestoresTrucks() {
        // Complete booking workflow
        TransporterRequest transporterRequest = TransporterRequest.builder()
            .companyName("Fast Logistics")
            .rating(4.5)
            .availableTrucks(new HashMap<>(Map.of("Flatbed", 10)))
            .build();
        TransporterResponse transporter = transporterService.registerTransporter(transporterRequest);

        LoadRequest loadRequest = LoadRequest.builder()
            .shipperId("SHIP123")
            .loadingCity("New York")
            .unLoadingCity("Los Angeles")
            .productType("Steel")
            .truckType("Flatbed")
            .noOfTrucks(5)
            .weight(10000.0)
            .weightUnit(WeightUnit.KG)
            .loadingDate(new Timestamp(System.currentTimeMillis() + 86400000))
            .build();
        LoadResponse load = loadService.createLoad(loadRequest);

        BidRequest bidRequest = new BidRequest();
        bidRequest.setLoadId(load.getLoadId());
        bidRequest.setTransporterId(transporter.getTransporterId());
        bidRequest.setProposedRate(5000.0);
        bidRequest.setTrucksOffered(3);
        BidResponse bid = bidService.submitBid(bidRequest);

        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setBidId(bid.getBidId());
        BookingResponse booking = bookingService.acceptBid(bookingRequest);

        // Verify trucks were deducted
        TransporterResponse afterBooking = transporterService.getTransporter(transporter.getTransporterId());
        assertEquals(7, afterBooking.getAvailableTrucks().get("Flatbed"));

        // Cancel booking
        bookingService.cancelBooking(booking.getBookingId());

        // Verify trucks were restored
        TransporterResponse afterCancellation = transporterService.getTransporter(transporter.getTransporterId());
        assertEquals(10, afterCancellation.getAvailableTrucks().get("Flatbed"));

        // Verify booking status
        BookingResponse cancelledBooking = bookingService.getBooking(booking.getBookingId());
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());
    }
}
