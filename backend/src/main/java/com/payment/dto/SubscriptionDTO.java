package com.payment.dto;

import java.time.LocalDateTime;

/**
 * API response representing a Stripe subscription.
 * Returned when creating, listing, or cancelling subscriptions.
 *
 * @param id                 Stripe Subscription ID (e.g. "sub_xxx")
 * @param status             Subscription state: active, canceled, past_due
 * @param productName        Display name of the subscribed product
 * @param amount             Recurring charge amount in cents
 * @param currentPeriodStart Start of the current billing period
 * @param currentPeriodEnd   End of the current billing period / next billing date
 */
public record SubscriptionDTO(
    String id,
    String status,
    String productName,
    Long amount,
    LocalDateTime currentPeriodStart,
    LocalDateTime currentPeriodEnd
) {}
