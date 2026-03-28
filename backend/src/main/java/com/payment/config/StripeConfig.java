package com.payment.config;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Stripe SDK configuration and startup validation.
 *
 * On application startup this class:
 * 1. Reads the Stripe API key and webhook secret from application properties
 * 2. Sets the global Stripe.apiKey used by all SDK calls
 * 3. Validates connectivity by retrieving the connected Stripe account
 * 4. Fails fast with a descriptive error if the key is missing or invalid
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    // Stripe test/live secret key — injected from STRIPE_API_KEY env var via application.yml
    @Value("${stripe.api-key}")
    private String apiKey;

    // Webhook signing secret — used by WebhookController to verify inbound Stripe events
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * Returns the webhook signing secret for use in webhook signature verification.
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }

    /**
     * Initializes the Stripe SDK and validates API key connectivity at startup.
     *
     * Why fail-fast? If the API key is missing or invalid, no payment operations will work.
     * Catching this at startup prevents confusing runtime errors later.
     */
    @PostConstruct
    public void initStripe() {
        // Guard: ensure the API key was actually provided (not blank or placeholder)
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Stripe API key is not configured. Set the STRIPE_API_KEY environment variable. "
                + "See .env.example for details."
            );
        }

        // Set the global API key used by every Stripe SDK call in the application
        Stripe.apiKey = apiKey;
        log.info("Stripe API key configured successfully");

        // Validate connectivity by retrieving the connected account.
        // This confirms the key is valid and the Stripe API is reachable.
        try {
            Account account = Account.retrieve();
            log.info("Stripe connectivity verified — connected account: {}", account.getId());
        } catch (StripeException e) {
            // The key is set but Stripe rejected it or the API is unreachable.
            // Fail startup so the operator can fix the configuration immediately.
            throw new IllegalStateException(
                "Failed to validate Stripe API key on startup. "
                + "Ensure STRIPE_API_KEY is a valid test key from the Stripe Dashboard. "
                + "Stripe error: " + e.getMessage(),
                e
            );
        }
    }
}
