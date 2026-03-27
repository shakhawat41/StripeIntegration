package com.payment.dto;

/**
 * API response representing a Stripe invoice.
 * Used when creating invoices and listing them on the customer detail page.
 *
 * @param id              Stripe Invoice ID (e.g. "in_xxx")
 * @param status          Invoice state: paid, open, void
 * @param amount          Invoice amount in cents
 * @param customerName    Customer display name
 * @param paymentIntentId Stripe PaymentIntent ID associated with this invoice's payment
 */
public record InvoiceDTO(
    String id,
    String status,
    Long amount,
    String customerName,
    String paymentIntentId
) {}
