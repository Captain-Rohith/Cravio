package com.javacravio.cravio.payment.service;

import com.javacravio.cravio.payment.dto.PaymentRequest;
import com.javacravio.cravio.payment.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentByOrderId(Long orderId);
}

