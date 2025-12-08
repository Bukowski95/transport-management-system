package com.koustav.tms.exception;

/**
 * Exception thrown when optimistic locking detects concurrent modification
 * 
 * ═══════════════════════════════════════════════════════════════════
 * RATIONALE: Why This Exception Exists
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Optimistic locking prevents the "lost update" problem in concurrent scenarios.
 * 
 * THE PROBLEM:
 * Two shippers try to accept bids for the same transporter simultaneously:
 * 
 * Thread 1: Accept Bid B1 (3 trucks)     Thread 2: Accept Bid B2 (3 trucks)
 * ├─ Read T1: 5 trucks available         ├─ Read T1: 5 trucks available
 * ├─ Check: 3 <= 5 ✅                    ├─ Check: 3 <= 5 ✅
 * ├─ Deduct: 5 - 3 = 2                   ├─ Deduct: 5 - 3 = 2
 * ├─ Save: availableTrucks = 2           ├─ Save: availableTrucks = 2
 * └─ SUCCESS                              └─ SUCCESS
 * 
 * RESULT: Both save "2 trucks" instead of 2 and -1!
 * Total allocated: 6 trucks from a transporter with only 5!
 * 
 * THE SOLUTION: @Version field (Optimistic Locking)
 * Entity has a version number that increments on each update.
 * 
 * Thread 1:                               Thread 2:
 * ├─ Read: trucks=5, version=1           ├─ Read: trucks=5, version=1
 * ├─ Deduct: trucks=2                    ├─ Deduct: trucks=2
 * ├─ Save: WHERE version=1               ├─ (waiting...)
 * │  → SUCCESS! version becomes 2        │
 * └─ COMMIT                               ├─ Save: WHERE version=1
 *                                         │  → FAIL! version is now 2
 *                                         └─ OptimisticLockException
 * 
 * Thread 2's transaction rolls back, this exception is thrown.
 * 
 * HTTP Status Code: 409 CONFLICT
 * 
 * Why 409?
 * - 409 = "The request conflicts with the current state"
 * - Standard HTTP code for concurrent modification conflicts
 * - Tells client: "Someone else modified this, please retry"
 * - Not 400 (bad request) - the request was fine
 * - Not 500 (server error) - system worked as designed
 * 
 * ═══════════════════════════════════════════════════════════════════
 * WHEN TO USE
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Use this exception when:
 * ✅ OptimisticLockException caught from JPA
 * ✅ Concurrent modification detected
 * ✅ Version mismatch in @Version field
 * 
 * Don't use this when:
 * ❌ Simple capacity issue → Use InsufficientCapacityException
 * ❌ Status violation → Use InvalidStatusTransitionException
 * ❌ Resource not found → Use ResourceNotFoundException
 * 
 * ═══════════════════════════════════════════════════════════════════
 * EXAMPLES FROM TMS
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Example 1: Concurrent bid acceptance on same transporter
 * Timeline:
 * 10:00:00.000 - Shipper S1 accepts Bid B1 (T1, 3 trucks)
 * 10:00:00.001 - Shipper S2 accepts Bid B2 (T1, 3 trucks) [concurrent!]
 * 
 * Thread 1 (S1):                         Thread 2 (S2):
 * ├─ Fetch T1: version=1, trucks=5       ├─ Fetch T1: version=1, trucks=5
 * ├─ Validate capacity ✅                ├─ Validate capacity ✅
 * ├─ Deduct: trucks=2                    ├─ Deduct: trucks=2
 * ├─ UPDATE WHERE version=1              ├─ (blocked, waiting)
 * │  → SUCCESS, version=2                │
 * ├─ Create Booking Bo1 ✅               │
 * └─ COMMIT                               ├─ UPDATE WHERE version=1
 *                                         │  → FAIL! (version is 2)
 *                                         ├─ OptimisticLockException
 *                                         └─ → ConflictException
 * 
 * Response to S1: 201 Created (Booking Bo1)
 * Response to S2: 409 Conflict
 *                 {
 *                   "status": 409,
 *                   "message": "Another transaction modified this resource. 
 *                               Please retry."
 *                 }
 * 
 * USER ACTION: S2 retries → Will see T1 now has only 2 trucks → 
 *              InsufficientCapacityException on retry
 * 
 * Example 2: Concurrent load updates (less common but possible)
 * Scenario: Two shippers try to modify same load simultaneously
 * Result:   One succeeds, other gets 409 Conflict
 * 
 * ═══════════════════════════════════════════════════════════════════
 * IMPLEMENTATION PATTERN
 * ═══════════════════════════════════════════════════════════════════
 * 
 * In BookingService:
 * 
 * @Transactional
 * public BookingResponse acceptBid(UUID bidId) {
 *     try {
 *         // ... business logic ...
 *         
 *         transporter.deductTrucks(truckType, count);
 *         transporterRepository.save(transporter);  // ← Version check here!
 *         
 *         // ... more logic ...
 *         
 *         return response;
 *         
 *     } catch (OptimisticLockException e) {
 *         // JPA threw this when version mismatch detected
 *         // Convert to our domain exception
 *         throw new ConflictException(
 *             "Transporter",
 *             "Another transaction modified this transporter. Please retry."
 *         );
 *     }
 * }
 * 
 * ═══════════════════════════════════════════════════════════════════
 * ENTITIES WITH OPTIMISTIC LOCKING
 * ═══════════════════════════════════════════════════════════════════
 * 
 * In your TMS, these entities have @Version:
 * 
 * 1. Load (@Version Long version)
 *    Why? Prevents concurrent bid acceptances from over-allocating same load
 *    
 * 2. Transporter (@Version Long version)
 *    Why? Prevents concurrent bookings from over-allocating same transporter
 * 
 * When save() is called, Hibernate:
 * 1. Reads current version from database
 * 2. Compares with entity's version
 * 3. If match: Update + increment version
 * 4. If mismatch: Throw OptimisticLockException → ConflictException
 * 
 * ═══════════════════════════════════════════════════════════════════
 * CLIENT RETRY PATTERN
 * ═══════════════════════════════════════════════════════════════════
 * 
 * When client receives 409:
 * 
 * 1. Retry the request (resource state has changed)
 * 2. Refresh data before retry (get updated capacity)
 * 3. User might see different result (capacity might be gone)
 * 
 * Example client code:
 * 
 * function acceptBid(bidId) {
 *     try {
 *         const response = await post('/booking', {bidId});
 *         return response; // Success
 *     } catch (error) {
 *         if (error.status === 409) {
 *             // Conflict - someone else modified it
 *             // Refresh data and let user retry
 *             await refreshData();
 *             alert("Resource was modified. Please try again.");
 *         }
 *     }
 * }
 * 
 * ═══════════════════════════════════════════════════════════════════
 */
public class ConflictException extends RuntimeException {
    
    /**
     * Constructor with simple message
     * 
     * @param message The error message explaining the conflict
     * 
     * Usage:
     * throw new ConflictException("Concurrent modification detected");
     */
    public ConflictException(String message) {
        super(message);
    }
    
    /**
     * Constructor with resource and reason
     * Creates formatted message: "Conflict updating {resource}: {reason}"
     * 
     * @param resource The resource that had the conflict (e.g., "Transporter", "Load")
     * @param reason Explanation of what happened
     * 
     * Usage:
     * throw new ConflictException(
     *     "Transporter", 
     *     "Another transaction modified capacity. Please retry."
     * );
     * 
     * Result message:
     * "Conflict updating Transporter: Another transaction modified capacity. Please retry."
     */
    public ConflictException(String resource, String reason) {
        super(String.format("Conflict updating %s: %s", resource, reason));
    }
}
