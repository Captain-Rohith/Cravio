package com.javacravio.cravio.order.service;

import com.javacravio.cravio.order.dto.NearbyOrderResponse;
import com.javacravio.cravio.order.dto.OrderResponse;
import com.javacravio.cravio.order.dto.PlaceOrderRequest;
import com.javacravio.cravio.order.model.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    OrderResponse getOrder(Long orderId);

    List<OrderResponse> getCustomerOrders(Long customerId);

    List<OrderResponse> getRestaurantOrders(Long restaurantId);

    List<NearbyOrderResponse> getNearbyAvailableOrders(double latitude, double longitude);

    OrderResponse updateStatus(Long orderId, OrderStatus status);

    OrderResponse updateStatusByRestaurant(Long restaurantId, Long orderId, OrderStatus status);

    OrderResponse cancelByCustomer(Long customerId, Long orderId);

    OrderResponse assignDeliveryPartner(Long orderId, Long deliveryPartnerId);

    OrderResponse claimOrder(Long orderId, String deliveryPartnerEmail, double latitude, double longitude);
}
