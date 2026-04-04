package com.javacravio.cravio.tracking.dto;

public record TrackingEventResponse(
        Long orderId,
        Long deliveryPartnerId,
        Long customerId,
        double latitude,
        double longitude,
        String h3Index
) {
}
