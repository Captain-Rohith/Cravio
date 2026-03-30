package com.javacravio.cravio.order.dto;

import com.javacravio.cravio.order.model.OrderStatus;

import java.util.List;

public record OrderResponse(
        Long orderId,
        Long customerId,
        Long restaurantId,
        Long deliveryPartnerId,
        double totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items
) {
}

