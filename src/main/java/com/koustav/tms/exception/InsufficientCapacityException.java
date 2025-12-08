package com.koustav.tms.exception;

/**
 * Exception thrown when capacity constraints are violated
 * 
 * ═══════════════════════════════════════════════════════════════════
 * RATIONALE: Why This Exception Exists
 * ═══════════════════════════════════════════════════════════════════
 * 
 * The TMS is fundamentally about CAPACITY MANAGEMENT:
 * - Loads need X trucks
 * - Transporters have Y trucks available
 * - Bids offer Z trucks
 * 
 * Capacity violations occur at TWO critical points:
 * 
 * 1. BID SUBMISSION (Phase 1: Optimistic Check)
 *    - Transporter offers trucks they don't have
 *    - Example: Has 5 trucks, bids 6 trucks
 * 
 * 2. BID ACCEPTANCE (Phase 2: Strict Check - THE IMPORTANT ONE!)
 *    - Transporter had capacity when bidding
 *    - But capacity reduced (another booking accepted)
 *    - Now can't fulfill this bid (overbidding scenario!)
 * 
 * HTTP Status Code: 400 BAD REQUEST
 * 
 * Why 400?
 * - Request asks for something impossible (not enough capacity)
 * - Client should check capacity before attempting
 * - Not a server error (500) - it's a business rule violation
 * 
 * ═══════════════════════════════════════════════════════════════════
 * WHEN TO USE
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Use this exception when:
 * ✅ Transporter doesn't have enough trucks to bid
 * ✅ Transporter lost capacity after bidding (overbidding)
 * ✅ Bid offers more trucks than load needs
 * ✅ Load doesn't have remaining capacity for this bid
 * 
 * Don't use this when:
 * ❌ Resource doesn't exist → Use ResourceNotFoundException
 * ❌ Status transition invalid → Use InvalidStatusTransitionException
 * ❌ Concurrent modification → Use ConflictException
 * 
 * ═══════════════════════════════════════════════════════════════════
 * EXAMPLES FROM TMS
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Example 1: Bid with insufficient trucks (Phase 1 check)
 * Scenario: Transporter T1 has 5 TRAILER trucks
 * Request:  POST /bid {"transporterId": "T1", "trucksOffered": 6}
 * Problem:  6 > 5 (offers more than available)
 * Response: 400 BAD REQUEST
 *           {
 *             "status": 400,
 *             "message": "Insufficient truck capacity. Available: 5, Required: 6"
 *           }
 * 
 * Example 2: Overbidding scenario (Phase 2 check - CRITICAL!)
 * Timeline:
 * 1. T1 has 5 trucks
 * 2. T1 bids on L1 (3 trucks) → Bid B1 created (canBid ✅)
 * 3. T1 bids on L2 (3 trucks) → Bid B2 created (canBid ✅)
 * 4. Shipper accepts B1:
 *    - T1 trucks: 5 → 2 (deducted 3)
 *    - Booking Bo1 created ✅
 * 5. Shipper tries to accept B2:
 *    Request:  POST /booking {"bidId": "B2"}
 *    Problem:  canAcceptBooking: 3 > 2 ❌
 *    Response: 400 BAD REQUEST
 *              {
 *                "status": 400,
 *                "message": "Transporter no longer has sufficient capacity. 
 *                            Available: 2, Required: 3. Bid automatically rejected."
 *              }
 *    Action:   Bid B2 auto-rejected (status = REJECTED)
 * 
 * Example 3: Bid exceeds remaining load capacity
 * Scenario: Load L1 needs 5 trucks, 3 already allocated
 * Request:  POST /booking {"bidId": "B5"}  (Bid offers 3 trucks)
 * Problem:  Load only needs 2 more trucks, but bid offers 3
 * Response: 400 BAD REQUEST
 *           {
 *             "status": 400,
 *             "message": "Load only needs 2 more trucks, but bid offers 3"
 *           }
 * 
 * Example 4: Bid on non-existent truck type
 * Scenario: Transporter has {"TRAILER": 5, "CONTAINER": 3}
 * Request:  POST /bid {"truckType": "FLATBED", "trucksOffered": 2}
 * Problem:  Transporter doesn't have FLATBED trucks
 * Response: 400 BAD REQUEST
 *           {
 *             "status": 400,
 *             "message": "Transporter does not have FLATBED trucks available"
 *           }
 * 
 * ═══════════════════════════════════════════════════════════════════
 * IMPLEMENTATION PATTERN
 * ═══════════════════════════════════════════════════════════════════
 * 
 * // Pattern 1: Simple message
 * if (!transporter.canBid(truckType, trucksOffered)) {
 *     throw new InsufficientCapacityException(
 *         "Transporter doesn't have enough trucks"
 *     );
 * }
 * 
 * // Pattern 2: Detailed with numbers
 * if (bid.getTrucksOffered() > remainingTrucks) {
 *     throw new InsufficientCapacityException(
 *         "trucks", 
 *         remainingTrucks, 
 *         bid.getTrucksOffered()
 *     );
 * }
 * 
 * // Pattern 3: From entity business logic
 * public void deductTrucks(String truckType, int count) {
 *     Integer current = availableTrucks.getOrDefault(truckType, 0);
 *     if (current < count) {
 *         throw new IllegalStateException("Insufficient trucks");
 *         // Note: IllegalStateException from entity, converted to 
 *         // InsufficientCapacityException in service layer
 *     }
 * }
 * 
 * ═══════════════════════════════════════════════════════════════════
 * THE TWO-PHASE VALIDATION STRATEGY
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Phase 1: Bidding (Optimistic - allows competition)
 * ─────────────────────────────────────────────────
 * Method: transporter.canBid(truckType, count)
 * Check:  trucksOffered <= availableTrucks
 * Allow:  Multiple bids with same trucks
 * Throw:  InsufficientCapacityException if transporter doesn't have trucks
 * 
 * Example:
 * T1 has 5 trucks
 * T1 bids L1: 3 trucks ✅ (3 <= 5)
 * T1 bids L2: 3 trucks ✅ (3 <= 5, same trucks!)
 * T1 bids L3: 6 trucks ❌ throw InsufficientCapacityException
 * 
 * Phase 2: Acceptance (Strict - prevents overbooking)
 * ─────────────────────────────────────────────────
 * Method: transporter.canAcceptBooking(truckType, count)
 * Check:  trucksOffered <= CURRENT availableTrucks
 * Ensure: Only allocate what's actually available NOW
 * Throw:  InsufficientCapacityException if capacity lost since bidding
 * 
 * Example (continuing above):
 * Accept L1: deductTrucks(3) → now has 2
 * Accept L2: canAcceptBooking(3)? NO! (3 > 2)
 *           ❌ throw InsufficientCapacityException
 *           Auto-reject Bid B2
 * 
 * This is THE CORE of preventing overbooking! 🎯
 * 
 * ═══════════════════════════════════════════════════════════════════
 */
public class InsufficientCapacityException extends RuntimeException {
    
    /**
     * Constructor with simple message
     * 
     * @param message The error message explaining the capacity issue
     * 
     * Usage:
     * throw new InsufficientCapacityException("Not enough trucks available");
     */
    public InsufficientCapacityException(String message) {
        super(message);
    }
    
    /**
     * Constructor with detailed capacity information
     * Creates formatted message: "Insufficient {resourceType} capacity. Available: {available}, Required: {required}"
     * 
     * @param resourceType The type of resource (e.g., "truck", "capacity")
     * @param available The amount currently available
     * @param required The amount needed
     * 
     * Usage:
     * throw new InsufficientCapacityException("truck", 2, 3);
     * 
     * Result message:
     * "Insufficient truck capacity. Available: 2, Required: 3"
     */
    public InsufficientCapacityException(String resourceType, int available, int required) {
        super(String.format(
            "Insufficient %s capacity. Available: %d, Required: %d", 
            resourceType, 
            available, 
            required
        ));
    }
}
