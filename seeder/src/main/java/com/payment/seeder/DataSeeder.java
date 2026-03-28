package com.payment.seeder;

import com.payment.seeder.entity.SeederStatus;
import com.payment.seeder.repository.SeederStatusRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Seeds test data into Stripe and PostgreSQL on startup.
 *
 * Idempotency: checks the seeder_status table before doing any work.
 * If already seeded, exits immediately. Existing Stripe entities are
 * reused by searching for metadata tag app=stripe-payment-integration.
 *
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SEEDER_NAME = "initial-seed";
    private static final String APP_TAG = "stripe-payment-integration";

    private final SeederStatusRepository seederStatusRepository;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    public DataSeeder(SeederStatusRepository seederStatusRepository) {
        this.seederStatusRepository = seederStatusRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Idempotency check — skip if already seeded (Requirement 3.5)
        if (seederStatusRepository.findBySeederNameAndCompletedTrue(SEEDER_NAME).isPresent()) {
            log.info("Seeder '{}' already completed — skipping", SEEDER_NAME);
            return;
        }

        Stripe.apiKey = stripeApiKey;
        log.info("Starting test data seeding...");

        // Create or reuse test customers with different payment method types
        String customer1Id = findOrCreateCustomer("Alice Johnson", "alice@example.com");
        String customer2Id = findOrCreateCustomer("Bob Smith", "bob@example.com");
        String customer3Id = findOrCreateCustomer("Carol Williams", "carol@example.com");

        // Attach all three payment method types to every customer (Requirement 3.3, 3.4)
        for (String custId : List.of(customer1Id, customer2Id, customer3Id)) {
            attachTestPaymentMethods(custId);
        }

        // Create or reuse test products and prices (Requirement 3.2)
        findOrCreateProduct("Monthly Service Plan", 2999L);  // $29.99/month
        findOrCreateProduct("Premium Support", 4999L);       // $49.99/month

        // Mark seeding as complete
        SeederStatus status = new SeederStatus();
        status.setSeederName(SEEDER_NAME);
        status.setCompleted(true);
        status.setExecutedAt(LocalDateTime.now());
        seederStatusRepository.save(status);

        log.info("Test data seeding completed successfully");
    }

    /**
     * Finds an existing customer by email with our app tag, or creates a new one.
     * Reuses existing entities to maintain idempotency (Requirement 3.5).
     */
    private String findOrCreateCustomer(String name, String email) throws StripeException {
        // Search for existing customer with our metadata tag
        CustomerCollection existing = Customer.list(
            CustomerListParams.builder().setEmail(email).setLimit(1L).build()
        );

        for (Customer c : existing.getData()) {
            if (c.getMetadata() != null && APP_TAG.equals(c.getMetadata().get("app"))) {
                log.info("Reusing existing customer: {} ({})", name, c.getId());
                return c.getId();
            }
        }

        // Create new customer with metadata tag for future lookups
        Customer customer = Customer.create(
            CustomerCreateParams.builder()
                .setName(name)
                .setEmail(email)
                .putMetadata("app", APP_TAG)
                .build()
        );
        log.info("Created customer: {} ({})", name, customer.getId());
        return customer.getId();
    }

    /**
     * Attaches all three payment method types to a customer:
     * 1. Credit card (Visa) via pm_card_visa
     * 2. Debit card (Visa Debit) via pm_card_visa_debit
     * 3. US bank account (ACH) via test bank account details
     *
     * Uses the PaymentMethod API (not the legacy Sources API) so methods
     * show up correctly in PaymentMethod.list() calls.
     * Skips types that already exist on the customer (idempotency).
     */
    private void attachTestPaymentMethods(String customerId) throws StripeException {
        // Check existing payment methods to avoid duplicates
        boolean hasCredit = false;
        boolean hasDebit = false;
        boolean hasAch = false;

        PaymentMethodCollection existingCards = PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build()
        );
        for (PaymentMethod pm : existingCards.getData()) {
            if ("debit".equals(pm.getCard().getFunding())) hasDebit = true;
            if ("credit".equals(pm.getCard().getFunding())) hasCredit = true;
        }

        PaymentMethodCollection existingBank = PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.US_BANK_ACCOUNT)
                .build()
        );
        if (!existingBank.getData().isEmpty()) hasAch = true;

        // Attach credit card (Visa) if not already present
        // Using raw params because the Stripe SDK's typed builders don't expose
        // a direct token-to-PaymentMethod path — this is the recommended test approach
        if (!hasCredit) {
            PaymentMethod creditCard = PaymentMethod.create(
                Map.of("type", "card", "card", Map.of("token", "tok_visa"))
            );
            creditCard.attach(
                PaymentMethodAttachParams.builder().setCustomer(customerId).build()
            );
            log.info("Attached credit card (Visa) to customer {}", customerId);
        }

        // Attach debit card (Visa Debit) if not already present
        if (!hasDebit) {
            PaymentMethod debitCard = PaymentMethod.create(
                Map.of("type", "card", "card", Map.of("token", "tok_visa_debit"))
            );
            debitCard.attach(
                PaymentMethodAttachParams.builder().setCustomer(customerId).build()
            );
            log.info("Attached debit card (Visa Debit) to customer {}", customerId);
        }

        // Attach US bank account (ACH) if not already present
        // Uses Stripe's test bank account details for sandbox
        if (!hasAch) {
            PaymentMethod bankAccount = PaymentMethod.create(
                Map.of(
                    "type", "us_bank_account",
                    "us_bank_account", Map.of(
                        "account_holder_type", "individual",
                        "account_number", "000123456789",
                        "routing_number", "110000000",
                        "account_type", "checking"
                    ),
                    "billing_details", Map.of("name", "Test Customer")
                )
            );
            bankAccount.attach(
                PaymentMethodAttachParams.builder().setCustomer(customerId).build()
            );
            log.info("Attached US bank account (ACH) to customer {}", customerId);
        }
    }

    /**
     * Finds or creates a recurring product with a monthly price.
     */
    private void findOrCreateProduct(String name, Long unitAmount) throws StripeException {
        // Search for existing product with our metadata tag
        ProductCollection existing = Product.list(
            ProductListParams.builder().setLimit(100L).build()
        );

        for (Product p : existing.getData()) {
            if (p.getMetadata() != null && APP_TAG.equals(p.getMetadata().get("app"))
                    && name.equals(p.getName())) {
                log.info("Reusing existing product: {} ({})", name, p.getId());
                return;
            }
        }

        // Create product
        Product product = Product.create(
            ProductCreateParams.builder()
                .setName(name)
                .putMetadata("app", APP_TAG)
                .build()
        );

        // Create monthly recurring price for the product
        Price.create(
            PriceCreateParams.builder()
                .setProduct(product.getId())
                .setUnitAmount(unitAmount)
                .setCurrency("usd")
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build()
                )
                .build()
        );

        log.info("Created product: {} ({}) with monthly price ${}", name, product.getId(), unitAmount / 100.0);
    }
}
