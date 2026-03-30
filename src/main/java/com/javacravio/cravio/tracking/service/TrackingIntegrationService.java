package com.javacravio.cravio.tracking.service;

import com.javacravio.cravio.tracking.dto.LocationUpdateRequest;

public interface TrackingIntegrationService {

    void forwardLocationUpdate(LocationUpdateRequest request);
}

