package com.payment.repository;

import com.payment.entity.SeederStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Data access for seeder status tracking. The seeder container queries this
 * on startup to determine if seeding has already been performed, ensuring
 * idempotent data initialization across container restarts.
 */
@Repository
public interface SeederStatusRepository extends JpaRepository<SeederStatus, Long> {

    /** Check if a specific seeder has already completed — used for idempotency */
    Optional<SeederStatus> findBySeederNameAndCompletedTrue(String seederName);
}
