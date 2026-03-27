package com.payment.dto;

import java.time.LocalDateTime;

/**
 * API response representing a recorded payment transaction.
 * Used by the transaction history page and customer detail page.
 *
 * @param stripeTransactionId Stripe PaymentIntent or Invoice ID
 * @param status              Payment outcome: succeeded, failed, or pending
 * @param amount              Transaction amount in cents
 * @param paymentMethodType   Payment method category: ach, debit, or credit
 * @param customerName        Customer display name
 * @param description         Human-readable context, e.g. "Subscription payment"
 * @param timestamp           When the payment operation completed
 */
public record TransactionDTO(
    String stripeTransactionId,
    String status,
    Long amount,
    String paymentMethodType,
    String customerName,
    String description,
    LocalDateTime timestamp
) {}
