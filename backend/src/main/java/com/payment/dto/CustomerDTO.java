package com.payment.dto;

import java.util.List;

/**
 * API response representing a Stripe customer with their attached payment methods.
 * Used by the customer list and customer detail endpoints.
 *
 * @param id                 Stripe Customer ID (e.g. "cus_xxx")
 * @param name               Customer display name
 * @param email              Customer email address
 * @param paymentMethodTypes List of attached payment method categories: "ach", "debit", "credit"
 */
public record CustomerDTO(
    String id,
    String name,
    String email,
    List<String> paymentMethodTypes
) {}
