package com.payment.service;

import com.payment.dto.CustomerDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.param.CustomerListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches customer data from Stripe and enriches it with local information.
 *
 * Customers live in Stripe as the source of truth — this service queries the
 * Stripe API and maps responses to our CustomerDTO, including the simplified
 * payment method types resolved by PaymentMethodService.
 *
 * Requirements: 4.3, 4.4, 8.1
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final PaymentMethodService paymentMethodService;

    public CustomerService(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    /**
     * Lists all customers from Stripe, enriched with their attached payment method types.
     * Stripe's Customer.list API is paginated — we fetch up to 100 customers per call,
     * which is sufficient for the seeded test data scenario.
     *
     * @return list of CustomerDTOs with payment method type summaries
     */
    public List<CustomerDTO> listCustomers() throws StripeException {
        // Fetch customers from Stripe — limit to 100 which covers our test data set
        CustomerCollection customers = Customer.list(
            CustomerListParams.builder()
                .setLimit(100L)
                .build()
        );

        List<CustomerDTO> result = new ArrayList<>();
        for (Customer customer : customers.getData()) {
            // Skip deleted customers — Stripe soft-deletes and may still return them
            if (Boolean.TRUE.equals(customer.getDeleted())) {
                continue;
            }

            // Resolve the simplified payment method types (ach, debit, credit) for this customer
            List<String> paymentMethodTypes = paymentMethodService.getPaymentMethodTypes(customer.getId());

            result.add(new CustomerDTO(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                paymentMethodTypes
            ));
        }

        log.info("Fetched {} customers from Stripe", result.size());
        return result;
    }

    /**
     * Retrieves a single customer by their Stripe Customer ID.
     * Used by the customer detail page to display the customer's information
     * alongside their subscriptions, invoices, plans, and transactions.
     *
     * @param customerId Stripe Customer ID (e.g. "cus_xxx")
     * @return CustomerDTO with payment method types
     * @throws StripeException if the customer doesn't exist or Stripe API fails
     */
    public CustomerDTO getCustomer(String customerId) throws StripeException {
        // Retrieve the specific customer from Stripe by ID
        Customer customer = Customer.retrieve(customerId);

        // Resolve payment method types for this customer
        List<String> paymentMethodTypes = paymentMethodService.getPaymentMethodTypes(customerId);

        return new CustomerDTO(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            paymentMethodTypes
        );
    }
}
