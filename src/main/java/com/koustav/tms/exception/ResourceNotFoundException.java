package com.koustav.tms.exception;


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
