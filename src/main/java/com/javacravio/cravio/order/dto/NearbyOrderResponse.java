package com.javacravio.cravio.order.dto;

import com.javacravio.cravio.order.model.OrderStatus;

import java.util.List;

public record NearbyOrderResponse(
        Long orderId,
        Long customerId,
        Long restaurantId,
        String restaurantName,
        double pickupLatitude,
        double pickupLongitude,
        Long deliveryPartnerId,
        double totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items
) {
}
