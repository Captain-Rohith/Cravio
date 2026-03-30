package com.javacravio.cravio.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderItemRequest(@NotNull Long menuItemId, @Min(1) int quantity) {
}

