package com.payment.dto;

/**
 * Request body for creating a new monthly subscription.
 *
 * @param customerId      Stripe Customer ID to subscribe
 * @param priceId         Stripe Price ID for the subscription product (created in Stripe Dashboard)
 * @param paymentMethodId Stripe PaymentMethod ID to charge for recurring payments
 */
public record CreateSubscriptionRequest(
    String customerId,
    String priceId,
    String paymentMethodId
) {}
