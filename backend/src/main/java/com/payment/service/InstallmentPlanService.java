package com.payment.service;

import com.payment.dto.CreateInstallmentPlanRequest;
import com.payment.dto.InstallmentDTO;
import com.payment.dto.InstallmentPlanDTO;
import com.payment.entity.Installment;
import com.payment.entity.InstallmentPlan;
import com.payment.repository.InstallmentPlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages custom installment payment plans.
 *
 * An installment plan splits a total amount into an initial payment (processed
 * immediately) followed by 6 equal installments on specified future dates.
 * Each payment is processed via a separate Stripe PaymentIntent.
 *
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.6, 6.7
 */
@Service
public class InstallmentPlanService {

    private static final Logger log = LoggerFactory.getLogger(InstallmentPlanService.class);

    private final InstallmentPlanRepository installmentPlanRepository;

    public InstallmentPlanService(InstallmentPlanRepository installmentPlanRepository) {
        this.installmentPlanRepository = installmentPlanRepository;
    }

    /**
     * Validates the installment plan math and business rules.
     * Requirement: 6.2, 6.7
     *
     * Rules enforced:
     * - Exactly 6 installment dates
     * - All dates must be in the future
     * - initialPayment must be > 0 and < totalAmount
     * - (totalAmount - initialPayment) must be evenly divisible by 6
     * - initialPayment + (installmentAmount × 6) must equal totalAmount
     *
     * @throws IllegalArgumentException if any validation rule fails
     */
    public void validatePlan(CreateInstallmentPlanRequest request) {
        if (request.installmentDates() == null || request.installmentDates().size() != 6) {
            throw new IllegalArgumentException("Installment plan must have exactly 6 installment dates");
        }

        // All installment dates must be in the future to prevent scheduling past payments
        for (LocalDate date : request.installmentDates()) {
            if (!date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("All installment dates must be in the future. Invalid date: " + date);
            }
        }

        if (request.initialPayment() <= 0) {
            throw new IllegalArgumentException("Initial payment must be greater than 0");
        }

        if (request.initialPayment() >= request.totalAmount()) {
            throw new IllegalArgumentException("Initial payment must be less than the total amount");
        }

        // The remaining amount after the initial payment must divide evenly into 6 installments.
        // No remainder allowed — this prevents rounding issues with cent amounts.
        long remaining = request.totalAmount() - request.initialPayment();
        if (remaining % 6 != 0) {
            throw new IllegalArgumentException(
                "Initial payment plus 6 equal installments must equal total amount. "
                + "The remaining amount (" + remaining + " cents) is not evenly divisible by 6."
            );
        }
    }

    /**
     * Creates an installment plan: validates the math, processes the initial payment
     * via Stripe, and creates 6 installment records in the database.
     * Requirement: 6.1, 6.3, 6.4
     *
     * @param request the plan creation request with amounts, dates, and payment method
     * @return the created plan with all installment details
     */
    @Transactional
    public InstallmentPlanDTO createPlan(CreateInstallmentPlanRequest request) throws StripeException {
        // Step 1: Validate the plan math before touching Stripe
        validatePlan(request);

        // Resolve customer name from Stripe for display purposes
        Customer customer = Customer.retrieve(request.customerId());
        String customerName = customer.getName();

        long installmentAmount = (request.totalAmount() - request.initialPayment()) / 6;

        // Step 2: Create and confirm a PaymentIntent for the initial payment
        PaymentIntent initialIntent = PaymentIntent.create(
            PaymentIntentCreateParams.builder()
                .setCustomer(request.customerId())
                .setAmount(request.initialPayment())
                .setCurrency("usd")
                .setPaymentMethod(request.paymentMethodId())
                .setConfirm(true)  // Process the initial payment immediately
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build()
                )
                .build()
        );

        String initialStatus = initialIntent.getStatus();
        // Map Stripe's "succeeded" status; anything else is treated as failed
        String normalizedStatus = "succeeded".equals(initialStatus) ? "succeeded" : "failed";

        // Step 3: Persist the plan to the database
        InstallmentPlan plan = new InstallmentPlan();
        plan.setStripeCustomerId(request.customerId());
        plan.setCustomerName(customerName);
        plan.setTotalAmount(request.totalAmount());
        plan.setInitialPaymentAmount(request.initialPayment());
        plan.setInitialPaymentIntentId(initialIntent.getId());
        plan.setInitialPaymentStatus(normalizedStatus);
        plan.setPaymentMethodId(request.paymentMethodId());
        plan.setCreatedAt(LocalDateTime.now());

        // Step 4: Create 6 installment records only if the initial payment succeeded.
        // Each installment starts as "pending" and will be processed on its scheduled date.
        if ("succeeded".equals(normalizedStatus)) {
            List<LocalDate> dates = request.installmentDates();
            for (int i = 0; i < 6; i++) {
                Installment installment = new Installment();
                installment.setInstallmentPlan(plan);
                installment.setSequenceNumber(i + 1);
                installment.setAmount(installmentAmount);
                installment.setScheduledDate(dates.get(i));
                installment.setStatus("pending");
                plan.getInstallments().add(installment);
            }
        }

        InstallmentPlan saved = installmentPlanRepository.save(plan);
        log.info("Created installment plan {} for customer {} — initial payment {}",
                saved.getId(), request.customerId(), normalizedStatus);

        return toDTO(saved);
    }

    /**
     * Creates a PaymentIntent for the initial payment and returns the client secret.
     * The frontend uses this with Stripe.js to confirm payment through custom card input.
     *
     * @param customerId      Stripe Customer ID
     * @param amount          Initial payment amount in cents
     * @param paymentMethodId Stripe PaymentMethod ID
     * @return map with clientSecret for Stripe.js confirmation
     */
    public Map<String, String> createInitialPaymentIntent(String customerId, Long amount, String paymentMethodId)
            throws StripeException {
        PaymentIntent intent = PaymentIntent.create(
            PaymentIntentCreateParams.builder()
                .setCustomer(customerId)
                .setAmount(amount)
                .setCurrency("usd")
                .setPaymentMethod(paymentMethodId)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                .build()
        );

        return Map.of("clientSecret", intent.getClientSecret());
    }

    /**
     * Lists all installment plans for a given customer.
     * Used on the customer detail page and installment plans listing.
     *
     * @param customerId Stripe Customer ID
     */
    public List<InstallmentPlanDTO> listPlans(String customerId) {
        return installmentPlanRepository.findByStripeCustomerId(customerId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Retrieves a single installment plan by its local database ID.
     * Returns the plan with all installment statuses.
     *
     * @param planId local database ID
     * @throws IllegalArgumentException if the plan doesn't exist
     */
    public InstallmentPlanDTO getPlan(Long planId) {
        InstallmentPlan plan = installmentPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Installment plan not found: " + planId));
        return toDTO(plan);
    }

    /**
     * Maps a JPA entity to the API response DTO, including all child installments.
     */
    private InstallmentPlanDTO toDTO(InstallmentPlan plan) {
        List<InstallmentDTO> installmentDTOs = plan.getInstallments().stream()
                .map(i -> new InstallmentDTO(
                    i.getSequenceNumber(),
                    i.getAmount(),
                    i.getScheduledDate(),
                    i.getStatus(),
                    i.getStripeErrorCode()
                ))
                .toList();

        return new InstallmentPlanDTO(
            plan.getId(),
            plan.getStripeCustomerId(),
            plan.getCustomerName(),
            plan.getTotalAmount(),
            plan.getInitialPaymentAmount(),
            plan.getInitialPaymentStatus(),
            installmentDTOs
        );
    }
}
