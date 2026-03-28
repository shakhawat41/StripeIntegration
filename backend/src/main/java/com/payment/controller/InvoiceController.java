package com.payment.controller;

import com.payment.dto.CreateInvoiceRequest;
import com.payment.dto.InvoiceDTO;
import com.payment.service.InvoiceService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for invoice creation and payment collection.
 * Requirements: 7.1, 7.2
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * POST /api/invoices — Create an invoice, finalize it, and collect payment.
     * Requirement: 7.2
     */
    @PostMapping
    public ResponseEntity<InvoiceDTO> createAndCollect(@RequestBody CreateInvoiceRequest request)
            throws StripeException {
        InvoiceDTO invoice = invoiceService.createAndCollectInvoice(
            request.customerId(), request.amount(), request.description(), request.paymentMethodId()
        );
        return ResponseEntity.ok(invoice);
    }

    /**
     * POST /api/invoices/create-payment-intent — Create a PaymentIntent for custom
     * UI payment via Stripe.js. Returns client secret.
     * Requirement: 7.2
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody CreateInvoiceRequest request)
            throws StripeException {
        Map<String, String> result = invoiceService.createPaymentIntent(
            request.customerId(), request.amount(), request.description(), request.paymentMethodId()
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/invoices?customerId={id} — List invoices for a customer.
     * Requirement: 7.1
     */
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> listInvoices(@RequestParam String customerId)
            throws StripeException {
        List<InvoiceDTO> invoices = invoiceService.listInvoices(customerId);
        return ResponseEntity.ok(invoices);
    }
}
