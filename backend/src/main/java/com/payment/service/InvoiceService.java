package com.payment.service;

import com.payment.dto.InvoiceDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.InvoiceItem;
import com.stripe.model.PaymentIntent;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceFinalizeInvoiceParams;
import com.stripe.param.InvoiceItemCreateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.InvoicePayParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages Stripe invoice creation, finalization, and payment collection.
 *
 * The flow is: create invoice → add invoice item → finalize → pay.
 * Stripe requires an InvoiceItem to be attached before finalization,
 * and the invoice must be finalized before payment can be collected.
 *
 * Requirements: 7.2, 7.3, 7.5, 7.6
 */
@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    /**
     * Creates a Stripe Invoice, adds a line item, finalizes it, and attempts
     * payment collection. This is the full invoice lifecycle in one call.
     *
     * @param customerId      Stripe Customer ID
     * @param amount          Invoice amount in cents
     * @param description     What the invoice is for
     * @param paymentMethodId Stripe PaymentMethod ID to charge
     * @return InvoiceDTO with the resulting status
     */
    public InvoiceDTO createAndCollectInvoice(String customerId, Long amount,
                                               String description, String paymentMethodId) throws StripeException {

        // Step 1: Create a draft invoice for the customer.
        // default_payment_method tells Stripe which method to charge when we call pay().
        Invoice invoice = Invoice.create(
            InvoiceCreateParams.builder()
                .setCustomer(customerId)
                .setDefaultPaymentMethod(paymentMethodId)
                .setAutoAdvance(false)  // We control finalization manually
                .build()
        );

        // Step 2: Add a line item to the invoice.
        // Stripe invoices require at least one InvoiceItem before they can be finalized.
        InvoiceItem.create(
            InvoiceItemCreateParams.builder()
                .setCustomer(customerId)
                .setInvoice(invoice.getId())
                .setAmount(amount)
                .setCurrency("usd")
                .setDescription(description)
                .build()
        );

        // Step 3: Finalize the invoice — transitions it from "draft" to "open",
        // making it ready for payment collection.
        invoice = invoice.finalizeInvoice(
            InvoiceFinalizeInvoiceParams.builder().build()
        );

        // Step 4: Attempt to collect payment on the finalized invoice.
        // This charges the default payment method immediately.
        invoice = invoice.pay(
            InvoicePayParams.builder().build()
        );

        log.info("Created and collected invoice {} for customer {} — status={}",
                invoice.getId(), customerId, invoice.getStatus());

        return toDTO(invoice);
    }

    /**
     * Creates a PaymentIntent for invoice payment and returns the client secret.
     * Used by the frontend's custom Stripe.js payment form.
     *
     * @param customerId      Stripe Customer ID
     * @param amount          Payment amount in cents
     * @param description     Invoice description
     * @param paymentMethodId Stripe PaymentMethod ID
     * @return map with clientSecret for Stripe.js confirmation
     */
    public Map<String, String> createPaymentIntent(String customerId, Long amount,
                                                    String description, String paymentMethodId) throws StripeException {
        PaymentIntent intent = PaymentIntent.create(
            PaymentIntentCreateParams.builder()
                .setCustomer(customerId)
                .setAmount(amount)
                .setCurrency("usd")
                .setPaymentMethod(paymentMethodId)
                .setDescription(description)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                .build()
        );

        return Map.of("clientSecret", intent.getClientSecret());
    }

    /**
     * Lists all invoices for a given customer from Stripe.
     * Used on the customer detail page and invoices listing.
     *
     * @param customerId Stripe Customer ID
     */
    public List<InvoiceDTO> listInvoices(String customerId) throws StripeException {
        InvoiceCollection invoices = Invoice.list(
            InvoiceListParams.builder()
                .setCustomer(customerId)
                .build()
        );

        List<InvoiceDTO> result = new ArrayList<>();
        for (Invoice inv : invoices.getData()) {
            result.add(toDTO(inv));
        }
        return result;
    }

    /**
     * Maps a Stripe Invoice to our API response DTO.
     * Resolves the customer name from Stripe for display.
     */
    private InvoiceDTO toDTO(Invoice invoice) throws StripeException {
        // Resolve customer name — invoice.getCustomerName() may be null
        // depending on how the invoice was created
        String customerName = invoice.getCustomerName();
        if (customerName == null && invoice.getCustomer() != null) {
            Customer customer = Customer.retrieve(invoice.getCustomer());
            customerName = customer.getName();
        }

        return new InvoiceDTO(
            invoice.getId(),
            invoice.getStatus(),
            invoice.getAmountDue(),
            customerName,
            invoice.getPaymentIntent()
        );
    }
}
