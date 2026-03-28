package com.payment.service;

import com.payment.dto.SubscriptionDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages Stripe subscriptions on behalf of the Business Owner.
 *
 * Subscriptions are created with a monthly billing interval using the Stripe SDK.
 * This service also supports creating PaymentIntents for custom UI confirmation,
 * cancelling subscriptions, and listing subscriptions per customer.
 *
 * Requirements: 5.2, 5.3, 5.5, 5.6
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    /**
     * Creates a Stripe Subscription with monthly billing for the given customer and price.
     * The payment method is set as the default for the subscription so Stripe charges it
     * automatically on each billing cycle.
     *
     * @param customerId      Stripe Customer ID
     * @param priceId         Stripe Price ID (must be a recurring price with monthly interval)
     * @param paymentMethodId Stripe PaymentMethod ID to use for recurring charges
     * @return SubscriptionDTO with ID, status, period dates, and product info
     */
    public SubscriptionDTO createSubscription(String customerId, String priceId, String paymentMethodId)
            throws StripeException {

        // Create the subscription in Stripe with the specified price and payment method.
        // default_payment_method ensures this payment method is charged each cycle.
        Subscription subscription = Subscription.create(
            SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build()
                )
                .setDefaultPaymentMethod(paymentMethodId)
                .build()
        );

        log.info("Created subscription {} for customer {} with price {}",
                subscription.getId(), customerId, priceId);

        return toDTO(subscription);
    }

    /**
     * Creates a PaymentIntent for custom UI payment confirmation.
     * The frontend uses the returned client secret with Stripe.js to confirm
     * the payment through custom card input fields rather than Stripe Checkout.
     *
     * @param customerId      Stripe Customer ID
     * @param priceId         Stripe Price ID — used to determine the amount
     * @param paymentMethodId Stripe PaymentMethod ID
     * @return a map containing the clientSecret for Stripe.js confirmation
     */
    public Map<String, String> createPaymentIntent(String customerId, String priceId, String paymentMethodId)
            throws StripeException {

        // Retrieve the price to get the unit amount for the PaymentIntent
        Price price = Price.retrieve(priceId);

        PaymentIntent intent = PaymentIntent.create(
            PaymentIntentCreateParams.builder()
                .setCustomer(customerId)
                .setAmount(price.getUnitAmount())
                .setCurrency(price.getCurrency())
                .setPaymentMethod(paymentMethodId)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                .build()
        );

        log.info("Created PaymentIntent {} for subscription setup, customer {}",
                intent.getId(), customerId);

        return Map.of("clientSecret", intent.getClientSecret());
    }

    /**
     * Cancels an active Stripe Subscription immediately.
     * Returns the updated subscription with "canceled" status.
     * Requirement: 5.5
     *
     * @param subscriptionId Stripe Subscription ID (e.g. "sub_xxx")
     * @return SubscriptionDTO with updated canceled status
     */
    public SubscriptionDTO cancelSubscription(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);

        // Cancel immediately — Stripe also supports cancel-at-period-end,
        // but the requirement specifies immediate cancellation
        subscription = subscription.cancel(
            SubscriptionCancelParams.builder().build()
        );

        log.info("Cancelled subscription {}", subscriptionId);
        return toDTO(subscription);
    }

    /**
     * Lists all subscriptions for a given customer.
     * Used on the customer detail page and the subscriptions list page.
     *
     * @param customerId Stripe Customer ID
     * @return list of SubscriptionDTOs
     */
    public List<SubscriptionDTO> listSubscriptions(String customerId) throws StripeException {
        // Fetch subscriptions from Stripe filtered by customer
        SubscriptionCollection subscriptions = Subscription.list(
            SubscriptionListParams.builder()
                .setCustomer(customerId)
                .build()
        );

        List<SubscriptionDTO> result = new ArrayList<>();
        for (Subscription sub : subscriptions.getData()) {
            result.add(toDTO(sub));
        }
        return result;
    }

    /**
     * Maps a Stripe Subscription object to our API response DTO.
     * Resolves the product name from the first subscription item's price.
     * Converts Unix timestamps to LocalDateTime for JSON serialization.
     */
    private SubscriptionDTO toDTO(Subscription subscription) throws StripeException {
        // A subscription can have multiple items, but our use case is single-product.
        // Grab the first item's price to get the product name and amount.
        var item = subscription.getItems().getData().get(0);
        String priceId = item.getPrice().getId();
        Price price = Price.retrieve(priceId);

        // Resolve the human-readable product name from the price's product reference
        String productName = "";
        if (price.getProduct() != null) {
            Product product = Product.retrieve(price.getProduct());
            productName = product.getName();
        }

        // Convert Stripe's Unix epoch seconds to LocalDateTime
        LocalDateTime periodStart = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(subscription.getCurrentPeriodStart()),
            ZoneId.systemDefault()
        );
        LocalDateTime periodEnd = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(subscription.getCurrentPeriodEnd()),
            ZoneId.systemDefault()
        );

        return new SubscriptionDTO(
            subscription.getId(),
            subscription.getStatus(),
            productName,
            price.getUnitAmount(),
            periodStart,
            periodEnd
        );
    }
}
