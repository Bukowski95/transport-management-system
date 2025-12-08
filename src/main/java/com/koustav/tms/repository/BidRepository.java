package com.koustav.tms.repository;

import com.koustav.tms.entity.Bid;
import com.koustav.tms.entity.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    
    // ========================================
    // METHOD 1: Filter Bids (All Combinations)
    // ========================================
    
    /**
     * API: GET /bid?loadId=&transporterId=&status=
     * 
     * RATIONALE:
     * All three parameters are OPTIONAL, creating 8 possible combinations:
     * 1. No filters → All bids
     * 2. Only loadId → All bids for a load
     * 3. Only transporterId → All bids by a transporter
     * 4. Only status → All bids with status
     * 5. loadId + transporterId → Transporter's bids for a load
     * 6. loadId + status → Bids for a load with specific status
     * 7. transporterId + status → Transporter's bids with status
     * 8. All three → Fully filtered
     * 
     * USE CASES:
     * - Shipper: "Show all bids for MY load L1" → loadId=L1
     * - Transporter: "Show MY pending bids" → transporterId=T1, status=PENDING
     * - System: "Show all REJECTED bids" → status=REJECTED
     * 
     * WHY ONE METHOD?
     * Instead of creating 7 separate methods, use @Query with NULL checks
     * to handle all combinations elegantly.
     */
    @Query("SELECT b FROM Bid b WHERE " +
           "(:loadId IS NULL OR b.load.loadId = :loadId) AND " +
           "(:transporterId IS NULL OR b.transporter.transporterId = :transporterId) AND " +
           "(:status IS NULL OR b.status = :status)")
    Page<Bid> findByFilters(
        @Param("loadId") UUID loadId,
        @Param("transporterId") UUID transporterId,
        @Param("status") BidStatus status,
        Pageable pageable
    );
    
    // ========================================
    // METHOD 2: Get Bids for Best-Bids API
    // ========================================
    
    /**
     * API: GET /load/{loadId}/best-bids
     * 
     * RATIONALE:
     * This API returns bids sorted by score calculation:
     * score = (1 / proposedRate) * 0.7 + (rating / 5) * 0.3
     * 
     * We need:
     * - All PENDING bids for a specific load
     * - As a List (not Page) because we'll sort in-memory by score
     * - Only PENDING because ACCEPTED/REJECTED bids are no longer relevant
     * 
     * USE CASE:
     * Shipper wants to see best bid suggestions sorted by quality/price ratio
     * 
     * WHY NOT PAGINATED?
     * - A load typically has 5-20 bids (small number)
     * - We need ALL bids to calculate scores and sort
     * - Pagination would complicate sorting logic
     * 
     * FLOW:
     * 1. Get all PENDING bids for load
     * 2. For each bid, fetch transporter to get rating
     * 3. Calculate score = (1/rate)*0.7 + (rating/5)*0.3
     * 4. Sort by score DESC
     * 5. Return top results
     */
    List<Bid> findByLoad_LoadIdAndStatus(UUID loadId, BidStatus status);
    
    // ========================================
    // METHOD 3: Count Pending Bids
    // ========================================
    
    /**
     * Used internally by: BookingService.cancelBooking()
     * 
     * RATIONALE:
     * When ALL bookings for a load are cancelled, we need to determine 
     * the new load status:
     * 
     * - If pending bids exist → status = OPEN_FOR_BIDS
     * - If no pending bids → status = POSTED
     * 
     * We only need the COUNT, not the actual bid objects.
     * 
     * USE CASE:
     * Load had 5 trucks needed:
     * - Booking B1 (3 trucks) - CONFIRMED
     * - Booking B2 (2 trucks) - CONFIRMED
     * - Load status = BOOKED
     * 
     * Then B1 and B2 are cancelled:
     * - Check: countByLoad_LoadIdAndStatus(loadId, PENDING)
     * - If count > 0 → There are still interested transporters → OPEN_FOR_BIDS
     * - If count = 0 → No interest → POSTED
     * 
     * WHY COUNT INSTEAD OF FIND?
     * - More efficient - doesn't load bid objects into memory
     * - We only care about existence (count > 0), not the actual bids
     * - Database can optimize COUNT queries better
     */
    long countByLoad_LoadIdAndStatus(UUID loadId, BidStatus status);
}