package com.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Represents a single scheduled installment within an InstallmentPlan.
 * Each plan has exactly 6 installments, each processed via a separate Stripe PaymentIntent
 * on its scheduled date.
 */
@Entity
@Table(name = "installments")
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Back-reference to the parent plan */
    @ManyToOne
    @JoinColumn(name = "installment_plan_id")
    private InstallmentPlan installmentPlan;

    /** Position in the sequence (1–6) — determines payment order */
    private int sequenceNumber;

    /** Installment amount in cents — equal to (totalAmount - initialPayment) / 6 */
    private Long amount;

    /** The date this installment is scheduled to be charged */
    private LocalDate scheduledDate;

    /** Stripe PaymentIntent ID — set when the payment is created/attempted */
    private String paymentIntentId;

    /** Payment outcome: pending (not yet charged), succeeded, or failed */
    private String status;

    /** Stripe error code if the payment failed — null when succeeded or pending */
    private String stripeErrorCode;

    public Installment() {
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InstallmentPlan getInstallmentPlan() {
        return installmentPlan;
    }

    public void setInstallmentPlan(InstallmentPlan installmentPlan) {
        this.installmentPlan = installmentPlan;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStripeErrorCode() {
        return stripeErrorCode;
    }

    public void setStripeErrorCode(String stripeErrorCode) {
        this.stripeErrorCode = stripeErrorCode;
    }
}
