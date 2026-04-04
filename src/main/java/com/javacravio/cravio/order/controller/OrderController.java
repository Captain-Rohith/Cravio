package com.javacravio.cravio.order.controller;

import com.javacravio.cravio.common.dto.ApiResponse;
import com.javacravio.cravio.order.dto.NearbyOrderResponse;
import com.javacravio.cravio.order.dto.OrderResponse;
import com.javacravio.cravio.order.dto.PlaceOrderRequest;
import com.javacravio.cravio.order.model.OrderStatus;
import com.javacravio.cravio.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed", orderService.placeOrder(request)));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DELIVERY_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Order fetched", orderService.getOrder(orderId)));
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> byCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Orders fetched", orderService.getCustomerOrders(customerId)));
    }

    @PatchMapping("/customers/{customerId}/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelByCustomer(
            @PathVariable Long customerId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancelByCustomer(customerId, orderId)));
    }

    @GetMapping("/restaurants/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> byRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant orders fetched", orderService.getRestaurantOrders(restaurantId)));
    }

    @GetMapping("/available/nearby")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<List<NearbyOrderResponse>>> nearbyAvailableOrders(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return ResponseEntity.ok(ApiResponse.success(
                "Nearby available orders fetched",
                orderService.getNearbyAvailableOrders(latitude, longitude)
        ));
    }

    @GetMapping("/delivery/accepted")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> acceptedOrders(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Accepted delivery orders fetched",
                orderService.getAcceptedOrdersByDeliveryPartner(authentication.getName())
        ));
    }

    @PatchMapping("/{orderId}/claim")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    public ResponseEntity<ApiResponse<OrderResponse>> claimOrder(
            @PathVariable Long orderId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order claimed",
                orderService.claimOrder(orderId, authentication.getName(), latitude, longitude)
        ));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated", orderService.updateStatus(orderId, status)));
    }

    @PatchMapping("/restaurants/{restaurantId}/{orderId}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatusByRestaurant(
            @PathVariable Long restaurantId,
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated",
                orderService.updateStatusByRestaurant(restaurantId, orderId, status)
        ));
    }

}
