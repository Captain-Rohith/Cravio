package com.javacravio.cravio.tracking.service;

import com.javacravio.cravio.tracking.dto.CustomerLocationUpdateRequest;
import com.javacravio.cravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.cravio.tracking.dto.TrackingEventResponse;

public interface TrackingIntegrationService {

    void forwardLocationUpdate(LocationUpdateRequest request, String principalEmail);

    void forwardCustomerLocationUpdate(CustomerLocationUpdateRequest request, String principalEmail);

    TrackingEventResponse getLatestLocationByOrderId(Long orderId, String principalEmail);

    TrackingEventResponse getLatestCustomerLocationByOrderId(Long orderId, String principalEmail);
}

