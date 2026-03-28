package com.payment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps a customer (by email) to their active Stripe subscription.
 * Allows looking up subscription status without querying Stripe every time.
 */
@Entity
@Table(name = "customer_subscriptions")
public class CustomerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Customer email — used as the lookup key */
    private String customerEmail;

    /** Stripe Subscription ID (sub_xxx) */
    private String stripeSubscriptionId;

    /** Stripe Customer ID (cus_xxx) */
    private String stripeCustomerId;

    /** Plan name: weekly, biweekly, or monthly */
    private String planName;

    /** Amount in cents */
    private Long amount;

    /** Subscription status: active, canceled, past_due */
    private String status;

    private LocalDateTime createdAt;

    public CustomerSubscription() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
