package com.koustav.tms.exception;


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
