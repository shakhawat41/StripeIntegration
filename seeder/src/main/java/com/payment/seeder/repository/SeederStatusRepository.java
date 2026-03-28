package com.payment.seeder.repository;

import com.payment.seeder.entity.SeederStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeederStatusRepository extends JpaRepository<SeederStatus, Long> {
    Optional<SeederStatus> findBySeederNameAndCompletedTrue(String seederName);
}
