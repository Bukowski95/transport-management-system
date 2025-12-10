package com.koustav.tms.exception;


public class InvalidStatusTransitionException extends RuntimeException {
    
    public InvalidStatusTransitionException(String currentStatus, String attemptedAction) {
        super(String.format(
            "Cannot perform '%s' when status is '%s'", 
            attemptedAction, 
            currentStatus
        ));
    }
}