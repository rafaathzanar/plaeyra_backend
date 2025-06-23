//package com.zanar.playera.controller;
//
//import com.stripe.exception.StripeException;
//import com.stripe.model.PaymentIntent;
//import com.zanar.playera.request.PaymentRequest;
//import com.zanar.playera.service.StripeService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/payment")
//public class PaymentController {
//
//    @Autowired
//    private StripeService stripeService;
//
//    @PostMapping("/create-payment-intent")
//    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentRequest paymentRequest) {
//        try {
//            // Calculate amount in smallest unit (e.g., cents for USD)
//            int amount = paymentRequest.getAmount() * 100;
//
//            // Create a PaymentIntent using StripeService
//            PaymentIntent paymentIntent = stripeService.createPaymentIntent(amount, "lkr", paymentRequest.getDescription());
//
//            // Return the client secret of the payment intent (needed for frontend)
//            return ResponseEntity.ok(paymentIntent.getClientSecret());
//        } catch (StripeException e) {
//            return ResponseEntity.badRequest().body("Error occurred: " + e.getMessage());
//        }
//    }
//}
