package com.payment.service;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Routes verified Stripe webhook events to the appropriate handlers.
 *
 * Supported event types:
 * - invoice.paid: An invoice payment succeeded
 * - payment_intent.succeeded: A PaymentIntent completed successfully
 * - payment_intent.payment_failed: A PaymentIntent failed
 * - customer.subscription.updated: A subscription status changed
 *
 * Each handler updates local records (transactions, installment statuses)
 * to keep the local database in sync with Stripe's state.
 *
 * Requirements: 2.3, 2.4
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final TransactionService transactionService;

    public WebhookService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Routes a verified webhook event to the appropriate handler based on event type.
     * Only processes known event types — unknown types are logged and ignored.
     */
    public void processEvent(Event event) {
        String eventType = event.getType();
        log.info("Processing webhook event: type={}, id={}", eventType, event.getId());

        switch (eventType) {
            case "invoice.paid" -> handleInvoicePaid(event);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            default -> log.info("Ignoring unhandled webhook event type: {}", eventType);
        }
    }

    /**
     * Handles invoice.paid events — records a successful invoice payment transaction.
     */
    private void handleInvoicePaid(Event event) {
        // Deserialize the event data to access invoice fields
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            // Invoice events contain the full invoice object
            if (obj instanceof com.stripe.model.Invoice invoice) {
                transactionService.recordTransaction(
                    invoice.getId(),
                    "succeeded",
                    invoice.getAmountPaid(),
                    invoice.getCurrency(),
                    "card",  // Default — webhook doesn't always include method type
                    invoice.getCustomerName(),
                    invoice.getCustomer(),
                    "Invoice payment (webhook)"
                );
                log.info("Recorded invoice.paid transaction for invoice {}", invoice.getId());
            }
        }
    }

    /**
     * Handles payment_intent.succeeded events — records a successful payment transaction.
     */
    private void handlePaymentIntentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (obj instanceof PaymentIntent pi) {
                transactionService.recordTransaction(
                    pi.getId(),
                    "succeeded",
                    pi.getAmount(),
                    pi.getCurrency(),
                    resolvePaymentMethodType(pi),
                    null,  // Customer name not always available on PaymentIntent
                    pi.getCustomer(),
                    "Payment succeeded (webhook)"
                );
                log.info("Recorded payment_intent.succeeded for {}", pi.getId());
            }
        }
    }

    /**
     * Handles payment_intent.payment_failed events — records a failed payment transaction.
     */
    private void handlePaymentIntentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (obj instanceof PaymentIntent pi) {
                transactionService.recordTransaction(
                    pi.getId(),
                    "failed",
                    pi.getAmount(),
                    pi.getCurrency(),
                    resolvePaymentMethodType(pi),
                    null,
                    pi.getCustomer(),
                    "Payment failed (webhook)"
                );
                log.info("Recorded payment_intent.payment_failed for {}", pi.getId());
            }
        }
    }

    /**
     * Handles customer.subscription.updated events — logs the status change.
     * Subscription state is always fetched live from Stripe when displayed,
     * so we just log the update for observability.
     */
    private void handleSubscriptionUpdated(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (obj instanceof com.stripe.model.Subscription sub) {
                log.info("Subscription {} updated — new status: {}", sub.getId(), sub.getStatus());
            }
        }
    }

    /**
     * Attempts to resolve the payment method type from a PaymentIntent.
     * Falls back to "card" if the payment method type isn't available.
     */
    private String resolvePaymentMethodType(PaymentIntent pi) {
        if (pi.getPaymentMethodTypes() != null && !pi.getPaymentMethodTypes().isEmpty()) {
            String type = pi.getPaymentMethodTypes().get(0);
            if ("us_bank_account".equals(type)) return "ach";
            return "card";
        }
        return "card";
    }
}
