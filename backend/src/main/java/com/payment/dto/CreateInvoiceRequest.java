package com.payment.dto;

/**
 * Request body for creating and collecting an invoice payment.
 *
 * @param customerId      Stripe Customer ID to invoice
 * @param amount          Invoice amount in cents
 * @param description     Human-readable description of what the invoice is for
 * @param paymentMethodId Stripe PaymentMethod ID to charge
 */
public record CreateInvoiceRequest(
    String customerId,
    Long amount,
    String description,
    String paymentMethodId
) {}
