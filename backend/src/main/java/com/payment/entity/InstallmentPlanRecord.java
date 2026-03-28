package com.payment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores merchant-created installment plans.
 * Phase 1: one-time payment for the custom first amount.
 * Phase 2: subscription for the remaining equal installments.
 */
@Entity
@Table(name = "installment_plan_records")
public class InstallmentPlanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerEmail;
    private String description;
    private Long totalAmountCents;
    private Long firstPaymentCents;
    private Long remainingInstallmentCents;
    private int totalInstallments;
    private String frequency; // weekly, biweekly, monthly

    /** Stripe Checkout Session ID for the first payment */
    private String firstPaymentSessionId;
    /** Stripe Customer ID (set after first payment) */
    private String stripeCustomerId;
    /** Stripe Subscription ID for remaining installments */
    private String stripeSubscriptionId;

    /** pending, first_paid, active, completed, canceled */
    private String status;
    private int installmentsPaid;
    private LocalDateTime createdAt;

    public InstallmentPlanRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getTotalAmountCents() { return totalAmountCents; }
    public void setTotalAmountCents(Long totalAmountCents) { this.totalAmountCents = totalAmountCents; }
    public Long getFirstPaymentCents() { return firstPaymentCents; }
    public void setFirstPaymentCents(Long firstPaymentCents) { this.firstPaymentCents = firstPaymentCents; }
    public Long getRemainingInstallmentCents() { return remainingInstallmentCents; }
    public void setRemainingInstallmentCents(Long remainingInstallmentCents) { this.remainingInstallmentCents = remainingInstallmentCents; }
    public int getTotalInstallments() { return totalInstallments; }
    public void setTotalInstallments(int totalInstallments) { this.totalInstallments = totalInstallments; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getFirstPaymentSessionId() { return firstPaymentSessionId; }
    public void setFirstPaymentSessionId(String firstPaymentSessionId) { this.firstPaymentSessionId = firstPaymentSessionId; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getInstallmentsPaid() { return installmentsPaid; }
    public void setInstallmentsPaid(int installmentsPaid) { this.installmentsPaid = installmentsPaid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
