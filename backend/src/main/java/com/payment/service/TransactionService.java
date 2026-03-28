package com.payment.service;

import com.payment.dto.TransactionDTO;
import com.payment.entity.TransactionRecord;
import com.payment.repository.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the local transaction history stored in PostgreSQL.
 *
 * Every completed payment operation (subscription, installment, invoice) is
 * recorded here so the Business Owner can view a unified transaction history
 * without querying Stripe's API each time. This also enables fast filtering
 * by customer and chronological ordering.
 *
 * Requirements: 9.2, 9.3, 9.5
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRecordRepository transactionRecordRepository;

    public TransactionService(TransactionRecordRepository transactionRecordRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
    }

    /**
     * Records a completed payment operation to the local database.
     * Called by other services (SubscriptionService, InstallmentPlanService,
     * InvoiceService) after a Stripe payment completes or fails.
     *
     * @param stripeTransactionId Stripe PaymentIntent or Invoice ID
     * @param status              Payment outcome: succeeded, failed, or pending
     * @param amount              Amount in cents
     * @param currency            ISO 4217 currency code (e.g. "usd")
     * @param paymentMethodType   Simplified type: ach, debit, or credit
     * @param customerName        Customer display name
     * @param stripeCustomerId    Stripe Customer ID for filtering
     * @param description         Human-readable context (e.g. "Subscription payment")
     * @return the saved TransactionRecord
     */
    public TransactionRecord recordTransaction(
            String stripeTransactionId,
            String status,
            Long amount,
            String currency,
            String paymentMethodType,
            String customerName,
            String stripeCustomerId,
            String description) {

        TransactionRecord record = new TransactionRecord();
        record.setStripeTransactionId(stripeTransactionId);
        record.setStatus(status);
        record.setAmount(amount);
        record.setCurrency(currency);
        record.setPaymentMethodType(paymentMethodType);
        record.setCustomerName(customerName);
        record.setStripeCustomerId(stripeCustomerId);
        record.setDescription(description);
        record.setTimestamp(LocalDateTime.now());

        TransactionRecord saved = transactionRecordRepository.save(record);
        log.info("Recorded transaction {} — status={}, amount={}, customer={}",
                stripeTransactionId, status, amount, customerName);
        return saved;
    }

    /**
     * Returns all transactions ordered by timestamp descending (newest first).
     * Used by the transaction history page.
     * Requirement: 9.3
     */
    public List<TransactionDTO> getAllTransactions() {
        return transactionRecordRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Returns transactions for a specific customer, ordered by timestamp descending.
     * Used on the customer detail page to show that customer's payment history.
     * Requirement: 9.5
     *
     * @param customerId Stripe Customer ID
     */
    public List<TransactionDTO> getTransactionsByCustomer(String customerId) {
        return transactionRecordRepository.findByStripeCustomerIdOrderByTimestampDesc(customerId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Maps a JPA entity to the API response DTO.
     * Keeps the entity's internal fields (id, currency, stripeCustomerId) out of the API response.
     */
    private TransactionDTO toDTO(TransactionRecord record) {
        return new TransactionDTO(
                record.getStripeTransactionId(),
                record.getStatus(),
                record.getAmount(),
                record.getPaymentMethodType(),
                record.getCustomerName(),
                record.getDescription(),
                record.getTimestamp()
        );
    }
}
