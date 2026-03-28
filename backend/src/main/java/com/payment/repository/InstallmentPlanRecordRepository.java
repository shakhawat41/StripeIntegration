package com.payment.repository;

import com.payment.entity.InstallmentPlanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentPlanRecordRepository extends JpaRepository<InstallmentPlanRecord, Long> {
    List<InstallmentPlanRecord> findAllByOrderByCreatedAtDesc();
    Optional<InstallmentPlanRecord> findByFirstPaymentSessionId(String sessionId);
    Optional<InstallmentPlanRecord> findByStripeSubscriptionId(String subscriptionId);
    List<InstallmentPlanRecord> findByCustomerEmail(String email);
}
