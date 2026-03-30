package com.javacravio.cravio.payment.dto;

import com.javacravio.cravio.payment.model.PaymentStatus;

public record PaymentResponse(Long paymentId, Long orderId, double amount, PaymentStatus status) {
}

