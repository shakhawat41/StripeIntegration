package com.payment.controller;

import com.payment.dto.CreateInstallmentPlanRequest;
import com.payment.dto.InstallmentPlanDTO;
import com.payment.service.InstallmentPlanService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for managing custom installment payment plans.
 * Plans split a total amount into an initial payment + 6 equal installments.
 *
 * Requirements: 6.1, 6.5
 */
@RestController
@RequestMapping("/api/installment-plans")
public class InstallmentPlanController {

    private final InstallmentPlanService installmentPlanService;

    public InstallmentPlanController(InstallmentPlanService installmentPlanService) {
        this.installmentPlanService = installmentPlanService;
    }

    /**
     * POST /api/installment-plans — Create a new installment plan.
     * Validates math, processes initial payment, creates 6 installment records.
     * Requirement: 6.1
     */
    @PostMapping
    public ResponseEntity<InstallmentPlanDTO> createPlan(@RequestBody CreateInstallmentPlanRequest request)
            throws StripeException {
        InstallmentPlanDTO plan = installmentPlanService.createPlan(request);
        return ResponseEntity.ok(plan);
    }

    /**
     * POST /api/installment-plans/confirm-initial — Create a PaymentIntent for the
     * initial payment and return the client secret for Stripe.js confirmation.
     * Requirement: 6.1
     */
    @PostMapping("/confirm-initial")
    public ResponseEntity<Map<String, String>> confirmInitialPayment(@RequestBody Map<String, Object> request)
            throws StripeException {
        // Extract fields from the generic map — this endpoint has a simpler body
        // than the full plan creation since it only needs amount info
        String customerId = (String) request.get("customerId");
        Long amount = ((Number) request.get("amount")).longValue();
        String paymentMethodId = (String) request.get("paymentMethodId");

        Map<String, String> result = installmentPlanService.createInitialPaymentIntent(
            customerId, amount, paymentMethodId
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/installment-plans?customerId={id} — List plans for a customer.
     * Requirement: 6.5
     */
    @GetMapping
    public ResponseEntity<List<InstallmentPlanDTO>> listPlans(@RequestParam String customerId) {
        List<InstallmentPlanDTO> plans = installmentPlanService.listPlans(customerId);
        return ResponseEntity.ok(plans);
    }

    /**
     * GET /api/installment-plans/{planId} — Plan detail with installment statuses.
     * Requirement: 6.5
     */
    @GetMapping("/{planId}")
    public ResponseEntity<InstallmentPlanDTO> getPlan(@PathVariable Long planId) {
        InstallmentPlanDTO plan = installmentPlanService.getPlan(planId);
        return ResponseEntity.ok(plan);
    }
}
