package com.javacravio.cravio.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlaceOrderRequest(
        @NotNull Long customerId,
        @NotNull Long restaurantId,
        @NotBlank String deliveryAddress,
        @Min(-90) @Max(90) Double deliveryLatitude,
        @Min(-180) @Max(180) Double deliveryLongitude,
        @Valid @NotEmpty List<PlaceOrderItemRequest> items
) {
}

