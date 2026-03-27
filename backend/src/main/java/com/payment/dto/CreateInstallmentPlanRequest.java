package com.payment.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for creating a custom installment payment plan.
 * The backend validates that initialPayment + 6 equal installments = totalAmount.
 *
 * @param customerId       Stripe Customer ID
 * @param totalAmount      Full plan amount in cents
 * @param initialPayment   Upfront payment amount in cents — must be > 0 and < totalAmount
 * @param installmentDates Exactly 6 future dates for the remaining installments
 * @param paymentMethodId  Stripe PaymentMethod ID to charge for all payments
 */
public record CreateInstallmentPlanRequest(
    String customerId,
    Long totalAmount,
    Long initialPayment,
    List<LocalDate> installmentDates,
    String paymentMethodId
) {}
