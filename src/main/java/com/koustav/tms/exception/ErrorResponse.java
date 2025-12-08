package com.koustav.tms.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response structure for all API errors
 * 
 * ═══════════════════════════════════════════════════════════════════
 * RATIONALE: Why This Class Exists
 * ═══════════════════════════════════════════════════════════════════
 * 
 * REST APIs should return CONSISTENT error format across all endpoints.
 * 
 * Bad API (inconsistent errors):
 * GET /load/invalid    → "Load not found"  (just a string)
 * POST /bid (invalid)  → {"error": "Bad request"}  (different format)
 * POST /booking        → {"message": "Conflict", "code": 409}  (another format!)
 * 
 * Good API (consistent errors):
 * All errors return:
 * {
 *   "status": 404,
 *   "message": "Load not found",
 *   "path": "/load/invalid",
 *   "timestamp": "2024-12-07T18:30:00"
 * }
 * 
 * Benefits:
 * ✅ Clients can handle errors uniformly
 * ✅ Easy to parse and display
 * ✅ Professional API design
 * ✅ Includes debugging info (path, timestamp)
 * 
 * ═══════════════════════════════════════════════════════════════════
 * STRUCTURE EXPLANATION
 * ═══════════════════════════════════════════════════════════════════
 * 
 * status (int):
 * - HTTP status code (404, 400, 409, 500, etc.)
 * - Tells client what type of error occurred
 * - Same as HTTP response status
 * 
 * message (String):
 * - Human-readable error description
 * - What went wrong and possibly how to fix
 * - Example: "Load not found with loadId: '123'"
 * 
 * path (String):
 * - The request path that caused the error
 * - Helpful for debugging
 * - Example: "/load/123" or "/booking"
 * 
 * timestamp (LocalDateTime):
 * - When the error occurred
 * - Useful for correlating with logs
 * - ISO format: "2024-12-07T18:30:00"
 * 
 * validationErrors (Map<String, String>) - OPTIONAL:
 * - Only present for validation errors (@Valid failures)
 * - Maps field name to error message
 * - Example: {"trucksOffered": "must be positive"}
 * 
 * @JsonInclude(NON_NULL):
 * - Don't include null fields in JSON
 * - If validationErrors is null, don't show it
 * - Cleaner JSON output
 * 
 * ═══════════════════════════════════════════════════════════════════
 * EXAMPLE RESPONSES
 * ═══════════════════════════════════════════════════════════════════
 * 
 * 1. Resource Not Found (404):
 * {
 *   "status": 404,
 *   "message": "Load not found with loadId: '550e8400-...'",
 *   "path": "/load/550e8400-...",
 *   "timestamp": "2024-12-07T18:30:00"
 * }
 * 
 * 2. Invalid Status Transition (400):
 * {
 *   "status": 400,
 *   "message": "Cannot cancel a load with status BOOKED",
 *   "path": "/load/L1/cancel",
 *   "timestamp": "2024-12-07T18:31:00"
 * }
 * 
 * 3. Insufficient Capacity (400):
 * {
 *   "status": 400,
 *   "message": "Insufficient truck capacity. Available: 2, Required: 3",
 *   "path": "/booking",
 *   "timestamp": "2024-12-07T18:32:00"
 * }
 * 
 * 4. Concurrent Modification (409):
 * {
 *   "status": 409,
 *   "message": "Conflict updating Transporter: Another transaction modified...",
 *   "path": "/booking",
 *   "timestamp": "2024-12-07T18:33:00"
 * }
 * 
 * 5. Validation Error (400) - with validationErrors:
 * {
 *   "status": 400,
 *   "message": "Validation failed",
 *   "path": "/bid",
 *   "timestamp": "2024-12-07T18:34:00",
 *   "validationErrors": {
 *     "trucksOffered": "must be greater than 0",
 *     "proposedRate": "must not be null"
 *   }
 * }
 * 
 * ═══════════════════════════════════════════════════════════════════
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't serialize null fields
public class ErrorResponse {
    
    /**
     * HTTP status code (404, 400, 409, 500, etc.)
     */
    private int status;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * The request path that caused the error
     */
    private String path;
    
    /**
     * When the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Validation errors (only for @Valid failures)
     * Maps field name to error message
     * null if not a validation error
     */
    private Map<String, String> validationErrors;
    
    /**
     * Constructor for simple errors (no validation errors)
     * 
     * @param status HTTP status code
     * @param message Error message
     * @param path Request path
     * @param timestamp When error occurred
     */
    public ErrorResponse(int status, String message, String path, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
        this.validationErrors = null;  // Will not be included in JSON
    }
}
