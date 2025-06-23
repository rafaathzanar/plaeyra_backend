//package com.zanar.playera.service;
//
//import com.stripe.Stripe;
//import com.stripe.exception.StripeException;
//import com.stripe.model.PaymentIntent;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class StripeService {
//
//    @Value("${stripe.secret.key}")
//    private String secretKey;
//
//    public StripeService() {
//        // Use the secretKey for Stripe initialization
//        Stripe.apiKey = secretKey;
//    }
//
//    // Method to create payment intent
//    public PaymentIntent createPaymentIntent(int amount, String currency, String description) throws StripeException {
//        Map<String, Object> params = new HashMap<>();
//        params.put("amount", amount); // amount in the smallest unit of currency (e.g., cents for USD)
//        params.put("currency", currency);
//        params.put("description", description);
//        params.put("payment_method_types", java.util.Collections.singletonList("card")); // Accept credit card payments
//
//        return PaymentIntent.create(params); // Return the created payment intent
//    }
//}
