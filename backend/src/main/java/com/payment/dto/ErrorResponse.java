package com.payment.dto;

import java.time.LocalDateTime;

/**
 * Standardized error response returned by all API endpoints on failure.
 * The global exception handler (@ControllerAdvice) maps exceptions to this format.
 *
 * @param errorCode Stripe error code (e.g. "card_declined") or application code (e.g. "VALIDATION_ERROR")
 * @param message   Human-readable error description
 * @param timestamp When the error occurred — useful for correlating with logs
 */
public record ErrorResponse(
    String errorCode,
    String message,
    LocalDateTime timestamp
) {}
