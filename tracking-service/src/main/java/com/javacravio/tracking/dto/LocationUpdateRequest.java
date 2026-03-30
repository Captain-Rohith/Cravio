package com.javacravio.tracking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(
        @NotNull Long orderId,
        @NotNull Long deliveryPartnerId,
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude
) {
}

