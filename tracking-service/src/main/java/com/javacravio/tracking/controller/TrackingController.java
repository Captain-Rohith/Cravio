package com.javacravio.tracking.controller;

import com.javacravio.tracking.dto.LocationUpdateRequest;
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
}

