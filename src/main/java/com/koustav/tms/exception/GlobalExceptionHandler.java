package com.koustav.tms.exception;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@ControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle 404 - Resource Not Found
     * 
     * Catches: ResourceNotFoundException
     * Returns: 404 NOT FOUND
     * 
     * When: Entity not found in database (Load, Bid, Booking, Transporter)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            extractPath(request),
            LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle 400 - Bad Request (Invalid Status Transition)
     * 
     * Catches: InvalidStatusTransitionException
     * Returns: 400 BAD REQUEST
     * 
     * When: Attempting invalid state transition (cancel BOOKED load, bid on CANCELLED load, etc.)
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransitionException(
            InvalidStatusTransitionException ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            extractPath(request),
            LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle 400 - Bad Request (Insufficient Capacity)
     * 
     * Catches: InsufficientCapacityException
     * Returns: 400 BAD REQUEST
     * 
     * When: Capacity constraints violated (not enough trucks, overbidding, etc.)
     */
    @ExceptionHandler(InsufficientCapacityException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCapacityException(
            InsufficientCapacityException ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            extractPath(request),
            LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle 409 - Conflict (Optimistic Locking / Concurrent Modification)
     * 
     * Catches: ConflictException, OptimisticLockException
     * Returns: 409 CONFLICT
     * 
     * When: Concurrent modification detected via @Version field
     */
    @ExceptionHandler({ConflictException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleConflictException(
            Exception ex,
            WebRequest request) {
        
        // Convert OptimisticLockException to user-friendly message
        String message = ex instanceof ConflictException
            ? ex.getMessage()
            : "Concurrent modification detected. The resource was modified by another " +
              "transaction. Please refresh and try again.";
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            message,
            extractPath(request),
            LocalDateTime.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
    
    /**
     * Handle 400 - Bad Request (Validation Errors)
     * 
     * Catches: MethodArgumentNotValidException (from @Valid annotations)
     * Returns: 400 BAD REQUEST with validation error details
     * 
     * When: Request body fails @Valid validation (@NotNull, @Min, @Size, etc.)
     * 
     * Example:
     * Request body: {"trucksOffered": -5, "proposedRate": null}
     * 
     * Response:
     * {
     *   "status": 400,
     *   "message": "Validation failed",
     *   "validationErrors": {
     *     "trucksOffered": "must be greater than 0",
     *     "proposedRate": "must not be null"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        // Extract field-level validation errors
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            validationErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            extractPath(request),
            LocalDateTime.now()
        );
        error.setValidationErrors(validationErrors);
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle 500 - Internal Server Error (Catch-all)
     * 
     * Catches: All other exceptions not handled above
     * Returns: 500 INTERNAL SERVER ERROR
     * 
     * When: Unexpected errors (NPE, IllegalStateException from entity, database errors, etc.)
     * 
     * NOTE: In production, you should NOT expose ex.getMessage() to users
     * (security risk). But for assignment/development, it's helpful for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.getMessage(),
            extractPath(request),
            LocalDateTime.now()
        );
        
        // Log the full stack trace for debugging
        // In production, use proper logging framework (SLF4J, Logback)
        System.err.println("=== INTERNAL SERVER ERROR ===");
        ex.printStackTrace();
        System.err.println("=============================");
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Extract clean request path from WebRequest
     * 
     * @param request The web request
     * @return Clean path (e.g., "/load/123" instead of "uri=/load/123")
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.replace("uri=", "");
    }
}
