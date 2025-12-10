package com.koustav.tms.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/test/path");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException and return 404")
    void handleResourceNotFoundException_Returns404() {
        // Arrange
        UUID testId = UUID.randomUUID();
        ResourceNotFoundException exception = new ResourceNotFoundException(
            "Load", "loadId", testId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleResourceNotFoundException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Load"));
        assertEquals("/test/path", response.getBody().getPath());
    }

    @Test
    @DisplayName("Should handle InvalidStatusTransitionException and return 400")
    void handleInvalidStatusTransitionException_Returns400() {
        // Arrange
        InvalidStatusTransitionException exception = new InvalidStatusTransitionException(
            "Can't cancel a BOOKED load"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleInvalidStatusTransitionException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Can't cancel a BOOKED load"));
    }

    @Test
    @DisplayName("Should handle InsufficientCapacityException and return 400")
    void handleInsufficientCapacityException_Returns400() {
        // Arrange
        InsufficientCapacityException exception = new InsufficientCapacityException(
            "Transporter doesn't have enough trucks"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleInsufficientCapacityException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("doesn't have enough trucks"));
    }

    @Test
    @DisplayName("Should handle ConflictException and return 409")
    void handleConflictException_Returns409() {
        // Arrange
        ConflictException exception = new ConflictException(
            "Transporter", "Another transaction modified the resource"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleConflictException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Another transaction"));
    }

    @Test
    @DisplayName("Should handle OptimisticLockException and return 409 with custom message")
    void handleOptimisticLockException_Returns409() {
        // Arrange
        OptimisticLockException exception = new OptimisticLockException("Version mismatch");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleConflictException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Concurrent modification detected"));
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException and return 400 with validation errors")
    void handleValidationException_Returns400WithValidationErrors() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("bidRequest", "proposedRate", "must be positive");
        FieldError fieldError2 = new FieldError("bidRequest", "trucksOffered", "must not be null");

        when(bindingResult.getFieldErrors())
            .thenReturn(java.util.List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            null, bindingResult
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleValidationException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals(2, response.getBody().getValidationErrors().size());
        assertEquals("must be positive", response.getBody().getValidationErrors().get("proposedRate"));
        assertEquals("must not be null", response.getBody().getValidationErrors().get("trucksOffered"));
    }

    @Test
    @DisplayName("Should handle generic Exception and return 500")
    void handleGlobalException_Returns500() {
        // Arrange
        RuntimeException exception = new RuntimeException("Unexpected error occurred");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleGlobalException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("An unexpected error occurred"));
        assertTrue(response.getBody().getMessage().contains("Unexpected error occurred"));
    }

    @Test
    @DisplayName("Should extract clean path from WebRequest")
    void extractPath_Success() {
        // Arrange
        when(webRequest.getDescription(false)).thenReturn("uri=/load/123/cancel");
        ResourceNotFoundException exception = new ResourceNotFoundException(
            "Load", "loadId", UUID.randomUUID()
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleResourceNotFoundException(exception, webRequest);

        // Assert
        assertEquals("/load/123/cancel", response.getBody().getPath());
    }

    @Test
    @DisplayName("Should include timestamp in error response")
    void errorResponse_IncludesTimestamp() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException(
            "Load", "loadId", UUID.randomUUID()
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler
            .handleResourceNotFoundException(exception, webRequest);

        // Assert
        assertNotNull(response.getBody().getTimestamp());
    }
}
