package com.javacravio.cravio.order.service;

import com.javacravio.cravio.order.dto.OrderResponse;
import com.javacravio.cravio.order.dto.PlaceOrderRequest;
import com.javacravio.cravio.order.model.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    OrderResponse getOrder(Long orderId);

    List<OrderResponse> getCustomerOrders(Long customerId);

    OrderResponse updateStatus(Long orderId, OrderStatus status);

    OrderResponse assignDeliveryPartner(Long orderId, Long deliveryPartnerId);
}

