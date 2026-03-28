package com.payment.controller;

import com.payment.dto.ErrorResponse;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

/**
 * Global exception handler that maps exceptions to standardized ErrorResponse DTOs.
 * Ensures all API errors return a consistent JSON structure with error code,
 * message, and timestamp — regardless of where the exception originated.
 *
 * Requirements: 5.6, 6.7, 7.5, 8.6
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Stripe SDK exceptions — maps the Stripe error code and message
     * to our ErrorResponse format. Returns 422 since the request was understood
     * but Stripe couldn't process it (e.g., card declined, invalid parameters).
     */
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ErrorResponse> handleStripeException(StripeException ex) {
        log.error("Stripe API error: code={}, message={}", ex.getCode(), ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            ex.getCode() != null ? ex.getCode() : "STRIPE_ERROR",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Handles validation errors (e.g., installment plan math, unsupported payment method).
     * Returns 400 Bad Request since the client sent invalid data.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Catch-all for unhandled exceptions — returns a generic 500 error.
     * Logs the full stack trace for debugging while returning a safe message to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);

        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
