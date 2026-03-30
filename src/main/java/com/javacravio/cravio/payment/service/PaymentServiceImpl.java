package com.javacravio.cravio.payment.service;

import com.javacravio.cravio.common.exception.NotFoundException;
import com.javacravio.cravio.payment.dto.PaymentRequest;
import com.javacravio.cravio.payment.dto.PaymentResponse;
import com.javacravio.cravio.payment.model.Payment;
import com.javacravio.cravio.payment.model.PaymentStatus;
import com.javacravio.cravio.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentRepository.findByOrderId(request.orderId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setOrderId(request.orderId());
                    payment.setAmount(request.amount());
                    payment.setStatus(mockGatewayStatus(request.amount()));
                    return toResponse(paymentRepository.save(payment));
                });
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    private PaymentStatus mockGatewayStatus(double amount) {
        return amount > 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getStatus());
    }
}

