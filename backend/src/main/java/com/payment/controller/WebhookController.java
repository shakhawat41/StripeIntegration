package com.payment.controller;

import com.payment.config.StripeConfig;
import com.payment.service.WebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives and verifies Stripe webhook events.
 *
 * Stripe sends POST requests to this endpoint when payment events occur.
 * The raw request body and Stripe-Signature header are used to verify
 * the event's authenticity before processing. This prevents spoofed events
 * from being acted upon.
 *
 * Requirements: 2.3, 2.4
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final StripeConfig stripeConfig;
    private final WebhookService webhookService;

    public WebhookController(StripeConfig stripeConfig, WebhookService webhookService) {
        this.stripeConfig = stripeConfig;
        this.webhookService = webhookService;
    }

    /**
     * POST /api/webhooks/stripe — Stripe webhook receiver.
     *
     * Verifies the webhook signature using the signing secret, then delegates
     * event processing to WebhookService. Returns 400 if signature is invalid,
     * 200 if the event was processed (or ignored) successfully.
     *
     * @param payload   Raw request body — must be the exact bytes Stripe sent
     * @param sigHeader Stripe-Signature header containing the HMAC signature
     * Requirement: 2.3, 2.4
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            // Verify the webhook signature using the signing secret.
            // This ensures the event actually came from Stripe and wasn't tampered with.
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            // Signature mismatch — reject the event and log the reason for debugging.
            // This could indicate a misconfigured webhook secret or a spoofed request.
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
        }

        // Signature verified — delegate to the service for event routing and processing
        webhookService.processEvent(event);
        return ResponseEntity.ok("Event processed");
    }
}
