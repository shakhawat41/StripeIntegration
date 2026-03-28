package com.payment.controller;

import com.payment.entity.InstallmentPlanRecord;
import com.payment.repository.InstallmentPlanRecordRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two-phase installment plan controller.
 * Phase 1: Merchant creates plan → customer pays first installment via Stripe Checkout.
 * Phase 2: After first payment, a Stripe Subscription is created for remaining installments.
 */
@RestController
@RequestMapping("/api/installments")
public class InstallmentCheckoutController {

    private static final Logger log = LoggerFactory.getLogger(InstallmentCheckoutController.class);

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final InstallmentPlanRecordRepository planRepo;

    public InstallmentCheckoutController(InstallmentPlanRecordRepository planRepo) {
        this.planRepo = planRepo;
    }

    /**
     * POST /api/installments/create — Merchant creates an installment plan.
     * Calculates the per-installment amount and stores the plan.
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPlan(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("customerEmail");
        String description = (String) request.get("description");
        long totalCents = ((Number) request.get("totalAmount")).longValue();
        long firstCents = ((Number) request.get("firstPayment")).longValue();
        int totalInstallments = ((Number) request.get("totalInstallments")).intValue();
        String frequency = (String) request.get("frequency");

        // Calculate remaining installment amount
        long remaining = totalCents - firstCents;
        int remainingCount = totalInstallments - 1;
        long perInstallment = remaining / remainingCount;
        // Last installment absorbs rounding
        long lastInstallment = remaining - (perInstallment * (remainingCount - 1));

        InstallmentPlanRecord plan = new InstallmentPlanRecord();
        plan.setCustomerEmail(email);
        plan.setDescription(description);
        plan.setTotalAmountCents(totalCents);
        plan.setFirstPaymentCents(firstCents);
        plan.setRemainingInstallmentCents(perInstallment);
        plan.setTotalInstallments(totalInstallments);
        plan.setFrequency(frequency);
        plan.setStatus("pending");
        plan.setInstallmentsPaid(0);
        plan.setCreatedAt(LocalDateTime.now());
        planRepo.save(plan);

        log.info("Created installment plan #{} for {} — total={}, first={}, {}x{}",
                plan.getId(), email, totalCents, firstCents, remainingCount, perInstallment);

        Map<String, Object> response = new HashMap<>();
        response.put("planId", plan.getId());
        response.put("customerEmail", email);
        response.put("totalAmount", totalCents);
        response.put("firstPayment", firstCents);
        response.put("remainingInstallment", perInstallment);
        response.put("lastInstallment", lastInstallment);
        response.put("totalInstallments", totalInstallments);
        response.put("status", "pending");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/installments/{planId}/pay-first — Customer pays the first installment.
     * Creates a Stripe Checkout Session in payment mode for the first amount.
     */
    @PostMapping("/{planId}/pay-first")
    public ResponseEntity<Map<String, String>> payFirstInstallment(@PathVariable Long planId)
            throws StripeException {

        InstallmentPlanRecord plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        // Create Checkout Session for the first payment
        Session session = Session.create(
            SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(plan.getCustomerEmail())
                // CRITICAL: must create a Stripe Customer so we can attach a subscription later
                .setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS)
                // Save the payment method for future subscription charges
                .setPaymentIntentData(
                    SessionCreateParams.PaymentIntentData.builder()
                        .setSetupFutureUsage(SessionCreateParams.PaymentIntentData.SetupFutureUsage.OFF_SESSION)
                        .build()
                )
                .setSuccessUrl(frontendUrl + "/installment-success?plan_id=" + planId + "&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/pay/installment/" + planId)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("cad")
                                .setUnitAmount(plan.getFirstPaymentCents())
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(plan.getDescription() + " — First Installment (1/" + plan.getTotalInstallments() + ")")
                                        .build()
                                )
                                .build()
                        )
                        .setQuantity(1L)
                        .build()
                )
                .build()
        );

        // Store the session ID so we can match it on confirmation
        plan.setFirstPaymentSessionId(session.getId());
        planRepo.save(plan);

        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    /**
     * POST /api/installments/confirm-first — Called after first payment succeeds.
     * Creates a Stripe Subscription for the remaining installments.
     */
    @PostMapping("/confirm-first")
    public ResponseEntity<Map<String, Object>> confirmFirstPayment(@RequestBody Map<String, String> request)
            throws StripeException {

        String sessionId = request.get("sessionId");
        Long planId = Long.parseLong(request.get("planId"));

        InstallmentPlanRecord plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        // Retrieve the checkout session to get the Stripe Customer ID
        Session session = Session.retrieve(sessionId);
        String customerId = session.getCustomer();

        if (customerId == null) {
            log.error("No Stripe Customer found on session {}", sessionId);
            return ResponseEntity.badRequest().body(Map.of("error", (Object) "No customer found on checkout session"));
        }

        // Get the payment method from the completed payment intent
        // so we can set it as default for the subscription
        String paymentIntentId = session.getPaymentIntent();
        com.stripe.model.PaymentIntent pi = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
        String paymentMethodId = pi.getPaymentMethod();

        // Set this payment method as the customer's default for invoices (subscriptions)
        com.stripe.model.Customer customer = com.stripe.model.Customer.retrieve(customerId);
        customer.update(com.stripe.param.CustomerUpdateParams.builder()
                .setInvoiceSettings(com.stripe.param.CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build())
                .build());

        plan.setStripeCustomerId(customerId);
        plan.setInstallmentsPaid(1);
        plan.setStatus("first_paid");

        // Phase 2: Create a subscription for the remaining installments
        int remainingCount = plan.getTotalInstallments() - 1;

        // Create a product and recurring price for the remaining installments
        Product product = Product.create(
            ProductCreateParams.builder()
                .setName(plan.getDescription() + " — Installment")
                .putMetadata("planId", plan.getId().toString())
                .build()
        );

        // Resolve frequency to Stripe interval
        PriceCreateParams.Recurring.Interval interval;
        long intervalCount;
        switch (plan.getFrequency()) {
            case "weekly" -> { interval = PriceCreateParams.Recurring.Interval.WEEK; intervalCount = 1; }
            case "biweekly" -> { interval = PriceCreateParams.Recurring.Interval.WEEK; intervalCount = 2; }
            default -> { interval = PriceCreateParams.Recurring.Interval.MONTH; intervalCount = 1; }
        }

        Price price = Price.create(
            PriceCreateParams.builder()
                .setProduct(product.getId())
                .setUnitAmount(plan.getRemainingInstallmentCents())
                .setCurrency("cad")
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(interval)
                        .setIntervalCount(intervalCount)
                        .build()
                )
                .build()
        );

        // Create subscription with cancel_after N-1 cycles
        // Using subscription_data to set the iteration count isn't directly available,
        // so we use cancel_at to set an end date, or subscription schedules.
        // Simplest approach: create subscription, then update with cancel_at_period_end after N-1 invoices.
        // For POC, we'll track installments_paid and cancel via webhook when count reaches total.
        Subscription subscription = Subscription.create(
            SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .setDefaultPaymentMethod(paymentMethodId)
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(price.getId())
                        .build()
                )
                .putMetadata("planId", plan.getId().toString())
                .putMetadata("remainingInstallments", String.valueOf(remainingCount))
                .build()
        );

        plan.setStripeSubscriptionId(subscription.getId());
        plan.setStatus("active");
        planRepo.save(plan);

        log.info("Phase 2: Created subscription {} for plan #{}, {} remaining installments",
                subscription.getId(), plan.getId(), remainingCount);

        Map<String, Object> response = new HashMap<>();
        response.put("planId", plan.getId());
        response.put("status", "active");
        response.put("subscriptionId", subscription.getId());
        response.put("installmentsPaid", 1);
        response.put("remainingInstallments", remainingCount);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/installments — List all installment plans (merchant view).
     */
    @GetMapping
    public ResponseEntity<List<InstallmentPlanRecord>> listPlans() {
        return ResponseEntity.ok(planRepo.findAllByOrderByCreatedAtDesc());
    }

    /**
     * GET /api/installments/{planId} — Get plan details.
     */
    @GetMapping("/{planId}")
    public ResponseEntity<InstallmentPlanRecord> getPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(
            planRepo.findById(planId).orElseThrow(() -> new IllegalArgumentException("Plan not found"))
        );
    }

    /**
     * GET /api/installments/customer?email=xxx — Customer view of their plans.
     */
    @GetMapping("/customer")
    public ResponseEntity<List<InstallmentPlanRecord>> getCustomerPlans(@RequestParam String email) {
        return ResponseEntity.ok(planRepo.findByCustomerEmail(email));
    }
}
