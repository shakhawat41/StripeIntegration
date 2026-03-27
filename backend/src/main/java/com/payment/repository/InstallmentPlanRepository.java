package com.payment.repository;

import com.payment.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access for installment plans. Supports listing plans by customer
 * for the customer detail page and installment plans page.
 */
@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {

    /** Returns all plans for a given Stripe customer */
    List<InstallmentPlan> findByStripeCustomerId(String stripeCustomerId);
}
