package com.payment.controller;

import com.payment.entity.CustomerSubscription;
import com.payment.repository.CustomerSubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cleaning service subscription checkout, status lookup, and cancellation.
 * Plans: Weekly $100, Bi-Weekly $180, Monthly $350.
 * Uses Stripe Checkout Sessions for subscription creation (hosted payment page).
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionCheckoutController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCheckoutController.class);

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final CustomerSubscriptionRepository subscriptionRepo;

    // Cache Stripe Price IDs after first creation to avoid duplicates
    private final ConcurrentHashMap<String, String> priceIdCache = new ConcurrentHashMap<>();

    public SubscriptionCheckoutController(CustomerSubscriptionRepository subscriptionRepo) {
        this.subscriptionRepo = subscriptionRepo;
    }

    /**
     * POST /api/subscriptions/checkout
     * Creates a Stripe Checkout Session for a subscription plan.
     * Body: { "plan": "weekly|biweekly|monthly", "email": "customer@example.com" }
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createSubscriptionCheckout(@RequestBody Map<String, String> request)
            throws StripeException {

        String plan = request.get("plan");
        String email = request.get("email");

        // Resolve plan details
        String planName;
        long amountCents;
        PriceCreateParams.Recurring.Interval interval;
        long intervalCount;

        switch (plan) {
            case "weekly" -> {
                planName = "Weekly Cleaning Service";
                amountCents = 10000; // $100
                interval = PriceCreateParams.Recurring.Interval.WEEK;
                intervalCount = 1;
            }
            case "biweekly" -> {
                planName = "Bi-Weekly Cleaning Service";
                amountCents = 18000; // $180
                interval = PriceCreateParams.Recurring.Interval.WEEK;
                intervalCount = 2;
            }
            case "monthly" -> {
                planName = "Monthly Cleaning Service";
                amountCents = 35000; // $350
                interval = PriceCreateParams.Recurring.Interval.MONTH;
                intervalCount = 1;
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid plan: " + plan));
            }
        }

        // Get or create the Stripe Price for this plan
        String priceId = getOrCreatePrice(plan, planName, amountCents, interval, intervalCount);

        // Create a Checkout Session in subscription mode
        Session session = Session.create(
            SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(email)
                .setSuccessUrl(frontendUrl + "/subscription-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/subscriptions")
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
                )
                .build()
        );

        log.info("Created subscription checkout session for plan={}, email={}", plan, email);
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    /**
     * POST /api/subscriptions/confirm
     * Called after successful checkout to store the subscription locally.
     * Body: { "sessionId": "cs_xxx" }
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmSubscription(@RequestBody Map<String, String> request)
            throws StripeException {

        String sessionId = request.get("sessionId");
        Session session = Session.retrieve(sessionId);

        String subscriptionId = session.getSubscription();
        String customerId = session.getCustomer();
        String email = session.getCustomerEmail();

        // Retrieve subscription details from Stripe
        Subscription sub = Subscription.retrieve(subscriptionId);
        var item = sub.getItems().getData().get(0);
        long amount = item.getPrice().getUnitAmount();
        String productId = item.getPrice().getProduct();
        Product product = Product.retrieve(productId);

        // Store locally
        CustomerSubscription cs = new CustomerSubscription();
        cs.setCustomerEmail(email);
        cs.setStripeSubscriptionId(subscriptionId);
        cs.setStripeCustomerId(customerId);
        cs.setPlanName(product.getName());
        cs.setAmount(amount);
        cs.setStatus("active");
        cs.setCreatedAt(LocalDateTime.now());
        subscriptionRepo.save(cs);

        log.info("Confirmed subscription {} for {}", subscriptionId, email);

        return ResponseEntity.ok(Map.of(
            "subscriptionId", subscriptionId,
            "planName", product.getName(),
            "amount", amount,
            "status", "active"
        ));
    }

    /**
     * GET /api/subscriptions/status?email=xxx
     * Returns the active subscription for a customer, or empty if none.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(@RequestParam String email) {
        var sub = subscriptionRepo.findByCustomerEmailAndStatus(email, "active");
        if (sub.isEmpty()) {
            return ResponseEntity.ok(Map.of("hasSubscription", false));
        }
        CustomerSubscription cs = sub.get();
        return ResponseEntity.ok(Map.of(
            "hasSubscription", true,
            "subscriptionId", cs.getStripeSubscriptionId(),
            "planName", cs.getPlanName(),
            "amount", cs.getAmount(),
            "status", cs.getStatus(),
            "createdAt", cs.getCreatedAt().toString()
        ));
    }

    /**
     * POST /api/subscriptions/cancel
     * Cancels an active subscription in Stripe and updates local record.
     * Body: { "email": "customer@example.com" }
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelSubscription(@RequestBody Map<String, String> request)
            throws StripeException {

        String email = request.get("email");
        var sub = subscriptionRepo.findByCustomerEmailAndStatus(email, "active");
        if (sub.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active subscription found"));
        }

        CustomerSubscription cs = sub.get();

        // Cancel in Stripe
        Subscription stripeSub = Subscription.retrieve(cs.getStripeSubscriptionId());
        stripeSub.cancel(SubscriptionCancelParams.builder().build());

        // Update local record
        cs.setStatus("canceled");
        subscriptionRepo.save(cs);

        log.info("Canceled subscription {} for {}", cs.getStripeSubscriptionId(), email);
        return ResponseEntity.ok(Map.of("status", "canceled"));
    }

    /**
     * Creates a Stripe Product + Price for a plan if not already cached.
     * Uses in-memory cache to avoid creating duplicates on each request.
     */
    private String getOrCreatePrice(String planKey, String planName, long amountCents,
                                     PriceCreateParams.Recurring.Interval interval, long intervalCount)
            throws StripeException {

        if (priceIdCache.containsKey(planKey)) {
            return priceIdCache.get(planKey);
        }

        Product product = Product.create(
            ProductCreateParams.builder()
                .setName(planName)
                .putMetadata("app", "stripe-poc")
                .putMetadata("plan", planKey)
                .build()
        );

        Price price = Price.create(
            PriceCreateParams.builder()
                .setProduct(product.getId())
                .setUnitAmount(amountCents)
                .setCurrency("cad")
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(interval)
                        .setIntervalCount(intervalCount)
                        .build()
                )
                .build()
        );

        priceIdCache.put(planKey, price.getId());
        log.info("Created Stripe product/price for plan={}, priceId={}", planKey, price.getId());
        return price.getId();
    }
}
