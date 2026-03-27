package com.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Stores the result of every completed payment operation (subscription payment,
 * installment payment, or invoice collection). This local record provides a
 * queryable transaction history independent of Stripe's API, enabling fast
 * filtering by customer and chronological ordering.
 */
@Entity
@Table(name = "transaction_records")
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stripe PaymentIntent ID or Invoice ID — links this record back to Stripe */
    private String stripeTransactionId;

    /** Payment outcome: succeeded, failed, or pending */
    private String status;

    /** Amount in cents — Stripe uses smallest currency unit to avoid floating-point issues */
    private Long amount;

    /** ISO 4217 currency code, e.g. "usd" */
    private String currency;

    /** Payment method category used: ach, debit, or credit */
    private String paymentMethodType;

    private String customerName;

    /** Stripe Customer ID — used for filtering transactions by customer */
    private String stripeCustomerId;

    /** Human-readable context, e.g. "Subscription payment", "Invoice #X" */
    private String description;

    /** When the payment operation completed */
    private LocalDateTime timestamp;

    public TransactionRecord() {
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStripeTransactionId() {
        return stripeTransactionId;
    }

    public void setStripeTransactionId(String stripeTransactionId) {
        this.stripeTransactionId = stripeTransactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
