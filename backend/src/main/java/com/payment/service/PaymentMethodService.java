package com.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.PaymentMethodListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves and categorizes payment methods attached to Stripe customers.
 *
 * Stripe stores payment methods by type (card, us_bank_account, etc.).
 * This service queries each supported type and maps them to our simplified
 * categories: "ach", "debit", and "credit". This abstraction keeps the rest
 * of the application decoupled from Stripe's internal type taxonomy.
 *
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6
 */
@Service
public class PaymentMethodService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodService.class);

    /**
     * Returns the simplified payment method type categories attached to a customer.
     * Queries Stripe for both card and us_bank_account types, then classifies
     * cards as "debit" or "credit" based on the card's funding field.
     *
     * @param customerId Stripe Customer ID (e.g. "cus_xxx")
     * @return list of type strings: "ach", "debit", "credit"
     */
    public List<String> getPaymentMethodTypes(String customerId) throws StripeException {
        List<String> types = new ArrayList<>();

        // Query ACH (US bank account) payment methods for this customer
        PaymentMethodCollection bankMethods = PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.US_BANK_ACCOUNT)
                .build()
        );
        if (!bankMethods.getData().isEmpty()) {
            types.add("ach");
        }

        // Query card payment methods — Stripe groups debit and credit under "card"
        // We distinguish them using the card.funding field ("debit" vs "credit")
        PaymentMethodCollection cardMethods = PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build()
        );
        for (PaymentMethod pm : cardMethods.getData()) {
            String funding = pm.getCard().getFunding();
            if ("debit".equals(funding) && !types.contains("debit")) {
                types.add("debit");
            } else if ("credit".equals(funding) && !types.contains("credit")) {
                types.add("credit");
            }
        }

        return types;
    }

    /**
     * Retrieves all PaymentMethod objects attached to a customer, across all
     * supported types. Used when the frontend needs to present a payment method
     * selector dropdown.
     *
     * @param customerId Stripe Customer ID
     * @return list of Stripe PaymentMethod objects
     */
    public List<PaymentMethod> getAllPaymentMethods(String customerId) throws StripeException {
        List<PaymentMethod> allMethods = new ArrayList<>();

        // Fetch US bank account (ACH) methods
        allMethods.addAll(PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.US_BANK_ACCOUNT)
                .build()
        ).getData());

        // Fetch card methods (covers both debit and credit)
        allMethods.addAll(PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build()
        ).getData());

        return allMethods;
    }

    /**
     * Resolves a specific payment method by ID and returns its simplified type.
     * Used to determine the payment method category when recording transactions.
     *
     * @param paymentMethodId Stripe PaymentMethod ID (e.g. "pm_xxx")
     * @return simplified type: "ach", "debit", or "credit"
     * @throws IllegalArgumentException if the payment method type is unsupported
     */
    public String resolvePaymentMethodType(String paymentMethodId) throws StripeException {
        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
        String type = pm.getType();

        // Map Stripe's internal types to our simplified categories
        if ("us_bank_account".equals(type)) {
            return "ach";
        } else if ("card".equals(type)) {
            // Cards are further classified by their funding source
            String funding = pm.getCard().getFunding();
            if ("debit".equals(funding)) {
                return "debit";
            } else if ("credit".equals(funding)) {
                return "credit";
            }
            // Prepaid or unknown card funding — treat as credit by default
            return "credit";
        }

        // Unsupported payment method type — Requirements 8.6
        throw new IllegalArgumentException(
            "Unsupported payment method type: " + type
            + ". Supported types are: bank account (ACH), debit card, credit card."
        );
    }
}
