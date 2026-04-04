package com.javacravio.tracking.controller;

import com.javacravio.tracking.dto.CustomerLocationUpdateRequest;
import com.javacravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.tracking.dto.TrackingEvent;
import com.javacravio.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(@Valid @RequestBody LocationUpdateRequest request) {
        trackingService.processLocationUpdate(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/customer-location")
    public ResponseEntity<Void> updateCustomerLocation(@Valid @RequestBody CustomerLocationUpdateRequest request) {
        trackingService.processCustomerLocationUpdate(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/orders/{orderId}/latest")
    public ResponseEntity<TrackingEvent> getLatestLocation(@PathVariable Long orderId) {
        TrackingEvent latest = trackingService.getLatestForOrder(orderId);
        if (latest == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(latest);
    }

    @GetMapping("/orders/{orderId}/customer/latest")
    public ResponseEntity<TrackingEvent> getLatestCustomerLocation(@PathVariable Long orderId) {
        TrackingEvent latest = trackingService.getLatestCustomerForOrder(orderId);
        if (latest == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(latest);
    }
}

