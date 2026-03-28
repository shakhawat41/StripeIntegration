package com.payment.seeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone Spring Boot application that seeds test data into Stripe
 * and PostgreSQL. Runs once as a short-lived Docker container.
 * Requirements: 3.1, 3.2
 */
@SpringBootApplication
public class SeederApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeederApplication.class, args);
    }
}
