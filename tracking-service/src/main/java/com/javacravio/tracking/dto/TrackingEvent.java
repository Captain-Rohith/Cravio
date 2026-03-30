package com.javacravio.tracking.dto;

public record TrackingEvent(
        Long orderId,
        Long deliveryPartnerId,
        double latitude,
        double longitude,
        String h3Index
) {
}

