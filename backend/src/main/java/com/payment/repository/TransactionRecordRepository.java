package com.payment.repository;

import com.payment.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access for transaction records stored in PostgreSQL.
 * Provides chronological listing and customer-scoped filtering for the
 * transaction history feature.
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    /** Returns all transactions newest-first — used by the transaction history page */
    List<TransactionRecord> findAllByOrderByTimestampDesc();

    /** Returns transactions for a specific customer newest-first — used on the customer detail page */
    List<TransactionRecord> findByStripeCustomerIdOrderByTimestampDesc(String stripeCustomerId);
}
