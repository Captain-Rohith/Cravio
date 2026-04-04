package com.javacravio.cravio.order.dto;

import com.javacravio.cravio.order.model.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long customerId,
        Long restaurantId,
        Long deliveryPartnerId,
        String deliveryAddress,
        Double deliveryLatitude,
        Double deliveryLongitude,
        Double restaurantLatitude,
        Double restaurantLongitude,
        double totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items
) {
}

