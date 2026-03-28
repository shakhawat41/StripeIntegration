package com.payment.controller;

import com.payment.dto.CreateSubscriptionRequest;
import com.payment.dto.SubscriptionDTO;
import com.payment.service.SubscriptionService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for managing monthly subscriptions.
 * All subscription operations delegate to Stripe via SubscriptionService.
 *
 * Requirements: 5.1, 5.2, 5.5
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * POST /api/subscriptions — Create a monthly subscription for a customer.
     * Requirement: 5.2
     */
    @PostMapping
    public ResponseEntity<SubscriptionDTO> createSubscription(@RequestBody CreateSubscriptionRequest request)
            throws StripeException {
        SubscriptionDTO subscription = subscriptionService.createSubscription(
            request.customerId(),
            request.priceId(),
            request.paymentMethodId()
        );
        return ResponseEntity.ok(subscription);
    }

    /**
     * POST /api/subscriptions/create-intent — Create a PaymentIntent/SetupIntent
     * for custom UI payment confirmation via Stripe.js.
     * Returns a client secret the frontend uses to confirm payment.
     * Requirement: 5.2
     */
    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody CreateSubscriptionRequest request)
            throws StripeException {
        Map<String, String> result = subscriptionService.createPaymentIntent(
            request.customerId(),
            request.priceId(),
            request.paymentMethodId()
        );
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/subscriptions/{subscriptionId} — Cancel an active subscription.
     * Requirement: 5.5
     */
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionDTO> cancelSubscription(@PathVariable String subscriptionId)
            throws StripeException {
        SubscriptionDTO subscription = subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(subscription);
    }

    /**
     * GET /api/subscriptions?customerId={id} — List subscriptions for a customer.
     * Requirement: 5.1
     */
    @GetMapping
    public ResponseEntity<List<SubscriptionDTO>> listSubscriptions(@RequestParam String customerId)
            throws StripeException {
        List<SubscriptionDTO> subscriptions = subscriptionService.listSubscriptions(customerId);
        return ResponseEntity.ok(subscriptions);
    }
}
