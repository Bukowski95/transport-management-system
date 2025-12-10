package com.koustav.tms.exception;


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
