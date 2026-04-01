package com.javacravio.cravio.payment.service;

import com.javacravio.cravio.common.exception.BusinessException;
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
        validatePaymentRequest(request);

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

    private void validatePaymentRequest(PaymentRequest request) {
        if (request == null || request.orderId() == null) {
            throw new BusinessException("orderId is required");
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new BusinessException("amount must be greater than 0");
        }
    }

    private PaymentStatus mockGatewayStatus(double amount) {
        return amount > 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getStatus());
    }
}
