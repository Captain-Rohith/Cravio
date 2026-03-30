package com.javacravio.cravio.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(@NotNull Long orderId, @DecimalMin("0.1") double amount) {
}

