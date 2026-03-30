package com.javacravio.cravio.restaurant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record MenuItemRequest(@NotBlank String name, @DecimalMin("0.0") double price) {
}

