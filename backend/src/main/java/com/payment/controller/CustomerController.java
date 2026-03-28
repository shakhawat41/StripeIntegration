package com.payment.controller;

import com.payment.dto.*;
import com.payment.service.*;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for viewing customers and their associated data.
 * Customer data lives in Stripe — this controller aggregates it with
 * local transaction history and installment plans.
 *
 * Requirements: 4.3, 4.4, 4.6
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final InstallmentPlanService installmentPlanService;
    private final TransactionService transactionService;
    private final PaymentMethodService paymentMethodService;

    public CustomerController(CustomerService customerService,
                              SubscriptionService subscriptionService,
                              InvoiceService invoiceService,
                              InstallmentPlanService installmentPlanService,
                              TransactionService transactionService,
                              PaymentMethodService paymentMethodService) {
        this.customerService = customerService;
        this.subscriptionService = subscriptionService;
        this.invoiceService = invoiceService;
        this.installmentPlanService = installmentPlanService;
        this.transactionService = transactionService;
        this.paymentMethodService = paymentMethodService;
    }

    /**
     * GET /api/customers — List all customers with payment method summary.
     * Requirement: 4.3
     */
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> listCustomers() throws StripeException {
        return ResponseEntity.ok(customerService.listCustomers());
    }

    /**
     * GET /api/customers/{customerId} — Customer detail with subscriptions,
     * invoices, installment plans, and transaction history.
     * Requirement: 4.4
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerDetail(@PathVariable String customerId)
            throws StripeException {
        CustomerDTO customer = customerService.getCustomer(customerId);
        List<SubscriptionDTO> subscriptions = subscriptionService.listSubscriptions(customerId);
        List<InvoiceDTO> invoices = invoiceService.listInvoices(customerId);
        List<InstallmentPlanDTO> plans = installmentPlanService.listPlans(customerId);
        List<TransactionDTO> transactions = transactionService.getTransactionsByCustomer(customerId);

        // Aggregate all customer-related data into a single response
        // so the frontend can render the full detail page in one request
        Map<String, Object> detail = Map.of(
            "customer", customer,
            "subscriptions", subscriptions,
            "invoices", invoices,
            "installmentPlans", plans,
            "transactions", transactions
        );

        return ResponseEntity.ok(detail);
    }

    /**
     * GET /api/customers/{customerId}/payment-methods — List payment methods
     * attached to a customer. Returns id, type, and a display label.
     * Used by frontend forms to populate the payment method dropdown.
     * Requirement: 8.2
     */
    @GetMapping("/{customerId}/payment-methods")
    public ResponseEntity<List<Map<String, String>>> getPaymentMethods(@PathVariable String customerId)
            throws StripeException {
        var methods = paymentMethodService.getAllPaymentMethods(customerId);
        var result = methods.stream().map(pm -> {
            String type = pm.getType();
            String label;
            String simpleType;
            if ("us_bank_account".equals(type)) {
                simpleType = "ach";
                label = "Bank account ending in " + pm.getUsBankAccount().getLast4();
            } else if ("card".equals(type)) {
                simpleType = pm.getCard().getFunding(); // "debit" or "credit"
                label = pm.getCard().getBrand() + " ending in " + pm.getCard().getLast4();
            } else {
                simpleType = type;
                label = type;
            }
            return Map.of("id", pm.getId(), "type", simpleType, "label", label);
        }).toList();
        return ResponseEntity.ok(result);
    }
}
