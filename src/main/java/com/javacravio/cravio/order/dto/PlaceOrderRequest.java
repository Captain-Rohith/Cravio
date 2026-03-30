package com.javacravio.cravio.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlaceOrderRequest(
        @NotNull Long customerId,
        @NotNull Long restaurantId,
        @Valid @NotEmpty List<PlaceOrderItemRequest> items
) {
}

