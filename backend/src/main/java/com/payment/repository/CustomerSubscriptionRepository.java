package com.payment.repository;

import com.payment.entity.CustomerSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerSubscriptionRepository extends JpaRepository<CustomerSubscription, Long> {
    Optional<CustomerSubscription> findByCustomerEmailAndStatus(String customerEmail, String status);
    Optional<CustomerSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
