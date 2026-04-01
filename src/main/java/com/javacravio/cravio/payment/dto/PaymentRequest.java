package com.javacravio.cravio.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull Long orderId,
        @NotNull @DecimalMin("0.1") Double amount
) {
}
