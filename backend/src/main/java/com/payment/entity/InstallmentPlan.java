package com.payment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a custom installment payment plan where a total amount is split into
 * an initial payment followed by 6 equal installments on specified dates.
 * The plan is created locally and each payment is processed via Stripe PaymentIntents.
 */
@Entity
@Table(name = "installment_plans")
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stripe Customer ID — identifies which customer this plan belongs to */
    private String stripeCustomerId;

    private String customerName;

    /** Total plan amount in cents — must equal initialPaymentAmount + (6 × installment amount) */
    private Long totalAmount;

    /** Upfront payment amount in cents — processed immediately when the plan is created */
    private Long initialPaymentAmount;

    /** Stripe PaymentIntent ID for the initial payment */
    private String initialPaymentIntentId;

    /** Outcome of the initial payment: succeeded or failed */
    private String initialPaymentStatus;

    /** Stripe PaymentMethod ID used for all payments in this plan */
    private String paymentMethodId;

    private LocalDateTime createdAt;

    /**
     * The 6 scheduled installments that follow the initial payment.
     * Cascade ALL ensures installments are persisted/removed with the plan.
     */
    @OneToMany(mappedBy = "installmentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Installment> installments = new ArrayList<>();

    public InstallmentPlan() {
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getInitialPaymentAmount() {
        return initialPaymentAmount;
    }

    public void setInitialPaymentAmount(Long initialPaymentAmount) {
        this.initialPaymentAmount = initialPaymentAmount;
    }

    public String getInitialPaymentIntentId() {
        return initialPaymentIntentId;
    }

    public void setInitialPaymentIntentId(String initialPaymentIntentId) {
        this.initialPaymentIntentId = initialPaymentIntentId;
    }

    public String getInitialPaymentStatus() {
        return initialPaymentStatus;
    }

    public void setInitialPaymentStatus(String initialPaymentStatus) {
        this.initialPaymentStatus = initialPaymentStatus;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Installment> getInstallments() {
        return installments;
    }

    public void setInstallments(List<Installment> installments) {
        this.installments = installments;
    }
}
