package com.javacravio.cravio.payment.controller;

import com.javacravio.cravio.common.dto.ApiResponse;
import com.javacravio.cravio.payment.dto.PaymentRequest;
import com.javacravio.cravio.payment.dto.PaymentResponse;
import com.javacravio.cravio.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> process(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed", paymentService.processPayment(request)));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> byOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Payment fetched", paymentService.getPaymentByOrderId(orderId)));
    }
}

