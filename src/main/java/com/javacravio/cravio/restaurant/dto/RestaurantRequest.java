package com.javacravio.cravio.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RestaurantRequest(
        @NotBlank String name,
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude
) {
}

