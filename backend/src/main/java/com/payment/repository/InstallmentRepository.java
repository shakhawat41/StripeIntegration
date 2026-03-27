package com.payment.repository;

import com.payment.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for individual installment records within a plan.
 * Most queries go through InstallmentPlan's OneToMany relationship,
 * but this repository enables direct updates (e.g., marking an installment
 * as succeeded/failed after a webhook event).
 */
@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {
}
