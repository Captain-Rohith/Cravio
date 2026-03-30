package com.javacravio.cravio.tracking.controller;

import com.javacravio.cravio.common.dto.ApiResponse;
import com.javacravio.cravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.cravio.tracking.service.TrackingIntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<ApiResponse<Void>> updateLocation(@Valid @RequestBody LocationUpdateRequest request) {
        trackingIntegrationService.forwardLocationUpdate(request);
        return ResponseEntity.ok(ApiResponse.success("Location forwarded", null));
    }
}

