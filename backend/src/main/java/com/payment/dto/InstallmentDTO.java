package com.payment.dto;

import java.time.LocalDate;

/**
 * API response representing a single installment within a plan.
 *
 * @param sequenceNumber Position in the plan (1–6)
 * @param amount         Installment amount in cents — equal to (totalAmount - initialPayment) / 6
 * @param scheduledDate  The date this installment is scheduled to be charged
 * @param status         Payment outcome: pending, succeeded, or failed
 * @param stripeErrorCode Stripe error code if payment failed — null otherwise
 */
public record InstallmentDTO(
    int sequenceNumber,
    Long amount,
    LocalDate scheduledDate,
    String status,
    String stripeErrorCode
) {}
