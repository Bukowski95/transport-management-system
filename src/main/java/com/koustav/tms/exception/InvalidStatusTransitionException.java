package com.koustav.tms.exception;

/**
 * Exception thrown when attempting an invalid state transition
 * 
 * ═══════════════════════════════════════════════════════════════════
 * RATIONALE: Why This Exception Exists
 * ═══════════════════════════════════════════════════════════════════
 * 
 * The TMS has entities with STATE MACHINES (status fields):
 * 
 * Load States:    POSTED → OPEN_FOR_BIDS → BOOKED → (CANCELLED)
 * Bid States:     PENDING → ACCEPTED/REJECTED
 * Booking States: CONFIRMED → COMPLETED/CANCELLED
 * 
 * Not all transitions are valid! For example:
 * ❌ Can't cancel a load that's already BOOKED
 * ❌ Can't bid on a load that's CANCELLED
 * ❌ Can't accept a bid that's already ACCEPTED
 * 
 * This exception enforces the state machine rules.
 * 
 * HTTP Status Code: 400 BAD REQUEST
 * 
 * Why 400?
 * - The request is technically valid (correct format)
 * - But it violates business rules about current state
 * - Client needs to check state before attempting action
 * 
 * ═══════════════════════════════════════════════════════════════════
 * WHEN TO USE
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Use this exception when:
 * ✅ Attempting action on wrong status (cancel BOOKED load)
 * ✅ Status transition not allowed by business rules
 * ✅ Entity is in terminal state (can't modify COMPLETED booking)
 * 
 * Don't use this when:
 * ❌ Resource doesn't exist → Use ResourceNotFoundException
 * ❌ Insufficient capacity → Use InsufficientCapacityException
 * ❌ Validation errors → Let @Valid handle it
 * 
 * ═══════════════════════════════════════════════════════════════════
 * EXAMPLES FROM TMS
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Example 1: Cancel a BOOKED load
 * Request:  PATCH /load/L1/cancel
 * Problem:  Load status = BOOKED
 * Rule:     Can only cancel POSTED or OPEN_FOR_BIDS loads
 * Response: 400 BAD REQUEST
 *           {
 *             "status": 400,
 *             "message": "Cannot cancel a load with status BOOKED"
 *           }
 * 
 * Example 2: Bid on CANCELLED load
 * Request:  POST /bid {"loadId": "L1", "trucksOffered": 3}
 * Problem:  Load L1 has status = CANCELLED
 * Rule:     Can only bid on POSTED or OPEN_FOR_BIDS loads
 * Response: 400 BAD REQUEST
 *           {
 *             "status": 400,
 *             "message": "Cannot bid on a load with status CANCELLED"
 *           }
 * 
 * Example 3: Accept already ACCEPTED bid
 * Request:  POST /booking {"bidId": "B1"}
 * Problem:  Bid B1 status = ACCEPTED
 * Rule:     Can only accept PENDING bids
 * Response: 400 BAD REQUEST
 *           {
 *             "status": 400,
 *             "message": "Bid is not in PENDING state. Current state: ACCEPTED"
 *           }
 * 
 * Example 4: Cancel already CANCELLED booking
 * Request:  PATCH /booking/Bo1/cancel
 * Problem:  Booking already has status = CANCELLED
 * Response: 400 BAD REQUEST
 *           {
 *             "status": 400,
 *             "message": "Booking is already cancelled"
 *           }
 * 
 * ═══════════════════════════════════════════════════════════════════
 * IMPLEMENTATION PATTERN
 * ═══════════════════════════════════════════════════════════════════
 * 
 * // Pattern 1: Check status before action
 * if (load.getStatus() == LoadStatus.BOOKED || load.getStatus() == LoadStatus.CANCELLED) {
 *     throw new InvalidStatusTransitionException(
 *         "Cannot cancel a load with status " + load.getStatus()
 *     );
 * }
 * 
 * // Pattern 2: With current status and attempted action
 * if (bid.getStatus() != BidStatus.PENDING) {
 *     throw new InvalidStatusTransitionException(
 *         bid.getStatus().toString(),
 *         "accept bid"
 *     );
 * }
 * 
 * ═══════════════════════════════════════════════════════════════════
 * STATE TRANSITION RULES (Reference)
 * ═══════════════════════════════════════════════════════════════════
 * 
 * LOAD:
 * ✅ POSTED → OPEN_FOR_BIDS (first bid received)
 * ✅ OPEN_FOR_BIDS → BOOKED (all trucks allocated)
 * ✅ POSTED/OPEN_FOR_BIDS → CANCELLED (shipper cancels)
 * ❌ BOOKED → CANCELLED (can't cancel booked load)
 * ❌ CANCELLED → anything (terminal state)
 * 
 * BID:
 * ✅ PENDING → ACCEPTED (shipper accepts)
 * ✅ PENDING → REJECTED (shipper rejects)
 * ❌ ACCEPTED → PENDING (can't undo acceptance)
 * ❌ REJECTED → PENDING (can't undo rejection)
 * 
 * BOOKING:
 * ✅ CONFIRMED → CANCELLED (either party cancels)
 * ✅ CONFIRMED → COMPLETED (delivery done - optional in assignment)
 * ❌ CANCELLED → CONFIRMED (can't undo cancellation)
 * ❌ COMPLETED → anything (terminal state)
 * 
 * ═══════════════════════════════════════════════════════════════════
 */
public class InvalidStatusTransitionException extends RuntimeException {
    
    /**
     * Constructor with simple message
     * 
     * @param message The error message explaining why transition is invalid
     * 
     * Usage:
     * throw new InvalidStatusTransitionException("Cannot cancel a BOOKED load");
     */
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
    
    /**
     * Constructor with current status and attempted action
     * Creates formatted message: "Cannot perform '{action}' when status is '{status}'"
     * 
     * @param currentStatus The current status of the entity
     * @param attemptedAction The action being attempted
     * 
     * Usage:
     * throw new InvalidStatusTransitionException("BOOKED", "cancel load");
     * 
     * Result message:
     * "Cannot perform 'cancel load' when status is 'BOOKED'"
     */
    public InvalidStatusTransitionException(String currentStatus, String attemptedAction) {
        super(String.format(
            "Cannot perform '%s' when status is '%s'", 
            attemptedAction, 
            currentStatus
        ));
    }
}