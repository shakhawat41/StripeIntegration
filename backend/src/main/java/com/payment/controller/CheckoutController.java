package com.payment.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Creates Stripe Checkout Sessions for the simplified invoice payment flow.
 * The frontend sends line items, and this endpoint creates a hosted checkout
 * page that handles card/bank input, 3D Secure, and redirects back on completion.
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * POST /api/checkout/create-session
     * Creates a Stripe Checkout Session from the provided line items + GST.
     * Returns the checkout URL for redirect.
     */
    @PostMapping("/create-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, Object> request)
            throws StripeException {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        long subtotal = ((Number) request.get("subtotal")).longValue();   // in cents
        long gst = ((Number) request.get("gst")).longValue();             // in cents
        long total = ((Number) request.get("total")).longValue();         // in cents

        // Build line items for the Stripe Checkout page
        var lineItemsBuilder = SessionCreateParams.builder();

        // Add each service item as a line item
        for (Map<String, Object> item : items) {
            String description = (String) item.get("description");
            long amount = ((Number) item.get("amount")).longValue();

            lineItemsBuilder.addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("cad")
                            .setUnitAmount(amount)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(description)
                                    .build()
                            )
                            .build()
                    )
                    .setQuantity(1L)
                    .build()
            );
        }

        // Add GST as a separate line item so it's visible on the checkout page
        lineItemsBuilder.addLineItem(
            SessionCreateParams.LineItem.builder()
                .setPriceData(
                    SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("cad")
                        .setUnitAmount(gst)
                        .setProductData(
                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName("GST (13%)")
                                .build()
                        )
                        .build()
                )
                .setQuantity(1L)
                .build()
        );

        // Create the Checkout Session
        Session session = Session.create(
            lineItemsBuilder
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/checkout")
                .setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS)
                .build()
        );

        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }
}
