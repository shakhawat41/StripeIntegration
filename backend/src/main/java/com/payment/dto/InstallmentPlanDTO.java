package com.payment.dto;

import java.util.List;

/**
 * API response representing an installment payment plan with its child installments.
 * Used on the customer detail page and installment plans listing.
 *
 * @param id                    Local database ID for the plan
 * @param customerId            Stripe Customer ID
 * @param customerName          Customer display name
 * @param totalAmount           Full plan amount in cents
 * @param initialPaymentAmount  Upfront payment amount in cents
 * @param initialPaymentStatus  Outcome of the initial payment: succeeded or failed
 * @param installments          The 6 scheduled installments with their statuses
 */
public record InstallmentPlanDTO(
    Long id,
    String customerId,
    String customerName,
    Long totalAmount,
    Long initialPaymentAmount,
    String initialPaymentStatus,
    List<InstallmentDTO> installments
) {}
