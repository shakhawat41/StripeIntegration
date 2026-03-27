package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the PaymentProcessing Spring Boot application.
 * This application serves as a Business Owner's portal for managing customers,
 * subscriptions, invoices, and installment payment plans via Stripe integration.
 */
@SpringBootApplication
public class PaymentProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessingApplication.class, args);
    }
}
