package com.koustav.tms.exception;

/**
 * Exception thrown when a requested resource is not found in the database
 * 
 * ═══════════════════════════════════════════════════════════════════
 * RATIONALE: Why This Exception Exists
 * ═══════════════════════════════════════════════════════════════════
 * 
 * In REST APIs, when a client requests a resource by ID (Load, Bid, Booking, 
 * Transporter), there are two possibilities:
 * 1. Resource exists → Return it (200 OK)
 * 2. Resource doesn't exist → This exception (404 NOT FOUND)
 * 
 * HTTP Status Code: 404 NOT FOUND
 * 
 * This follows REST conventions:
 * - 404 = "The resource you're looking for doesn't exist"
 * - Different from 400 (bad request format) or 401 (unauthorized)
 * 
 * ═══════════════════════════════════════════════════════════════════
 * WHEN TO USE
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Use this exception when:
 * ✅ findById() returns Optional.empty()
 * ✅ User requests non-existent load/bid/booking/transporter
 * ✅ Foreign key reference is invalid (bid references non-existent load)
 * 
 * Don't use this when:
 * ❌ Resource exists but user can't access it → Use 403 Forbidden
 * ❌ Request format is wrong → Use 400 Bad Request
 * ❌ Business rule violated → Use InvalidStatusTransitionException
 * 
 * ═══════════════════════════════════════════════════════════════════
 * EXAMPLES FROM TMS
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Example 1: Get non-existent load
 * Request:  GET /load/550e8400-e29b-41d4-a716-446655440000
 * Problem:  Load with this UUID doesn't exist in database
 * Response: 404 NOT FOUND
 *           {
 *             "status": 404,
 *             "message": "Load not found with loadId: '550e8400...'"
 *           }
 * 
 * Example 2: Accept non-existent bid
 * Request:  POST /booking {"bidId": "non-existent-id"}
 * Problem:  Bid doesn't exist
 * Response: 404 NOT FOUND
 *           {
 *             "status": 404,
 *             "message": "Bid not found with bidId: 'non-existent-id'"
 *           }
 * 
 * Example 3: Cancel non-existent booking
 * Request:  PATCH /booking/abc123/cancel
 * Problem:  Booking doesn't exist
 * Response: 404 NOT FOUND
 * 
 * ═══════════════════════════════════════════════════════════════════
 * IMPLEMENTATION PATTERN
 * ═══════════════════════════════════════════════════════════════════
 * 
 * Common usage in service layer:
 * 
 * // Pattern 1: Simple message
 * Load load = loadRepository.findById(loadId)
 *     .orElseThrow(() -> new ResourceNotFoundException("Load not found"));
 * 
 * // Pattern 2: Detailed message with field info
 * Bid bid = bidRepository.findById(bidId)
 *     .orElseThrow(() -> new ResourceNotFoundException("Bid", "bidId", bidId));
 * 
 * ═══════════════════════════════════════════════════════════════════
 */
public class ResourceNotFoundException extends RuntimeException {
    
    /**
     * Constructor with simple message
     * 
     * @param message The error message to display
     * 
     * Usage:
     * throw new ResourceNotFoundException("Load not found");
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Constructor with detailed field information
     * Creates a formatted message: "{resourceName} not found with {fieldName}: '{fieldValue}'"
     * 
     * @param resourceName The type of resource (e.g., "Load", "Bid", "Booking")
     * @param fieldName The field used to search (e.g., "loadId", "bidId")
     * @param fieldValue The value that was searched for
     * 
     * Usage:
     * throw new ResourceNotFoundException("Load", "loadId", loadId);
     * 
     * Result message:
     * "Load not found with loadId: '550e8400-e29b-41d4-a716-446655440000'"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
