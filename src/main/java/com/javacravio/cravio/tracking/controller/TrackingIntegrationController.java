package com.javacravio.cravio.tracking.controller;

import com.javacravio.cravio.common.dto.ApiResponse;
import com.javacravio.cravio.tracking.dto.CustomerLocationUpdateRequest;
import com.javacravio.cravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.cravio.tracking.dto.TrackingEventResponse;
import com.javacravio.cravio.tracking.service.TrackingIntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingIntegrationController {

    private final TrackingIntegrationService trackingIntegrationService;

    public TrackingIntegrationController(TrackingIntegrationService trackingIntegrationService) {
        this.trackingIntegrationService = trackingIntegrationService;
    }

    @PostMapping("/location")
    @PreAuthorize("hasAnyRole('DELIVERY_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @Valid @RequestBody LocationUpdateRequest request,
            Authentication authentication) {
        trackingIntegrationService.forwardLocationUpdate(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Location forwarded", null));
    }

    @PostMapping("/customer-location")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> updateCustomerLocation(
            @Valid @RequestBody CustomerLocationUpdateRequest request,
            Authentication authentication) {
        trackingIntegrationService.forwardCustomerLocationUpdate(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Customer location forwarded", null));
    }

    @GetMapping("/orders/{orderId}/latest")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DELIVERY_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TrackingEventResponse>> getLatestLocation(
            @PathVariable Long orderId,
            Authentication authentication) {
        TrackingEventResponse latest = trackingIntegrationService.getLatestLocationByOrderId(orderId, authentication.getName());
        String message = latest == null ? "No tracking location available yet" : "Latest tracking location";
        return ResponseEntity.ok(ApiResponse.success(message, latest));
    }

    @GetMapping("/orders/{orderId}/customer/latest")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DELIVERY_PARTNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TrackingEventResponse>> getLatestCustomerLocation(
            @PathVariable Long orderId,
            Authentication authentication) {
        TrackingEventResponse latest = trackingIntegrationService.getLatestCustomerLocationByOrderId(orderId, authentication.getName());
        String message = latest == null ? "No customer location available yet" : "Latest customer location";
        return ResponseEntity.ok(ApiResponse.success(message, latest));
    }
}

