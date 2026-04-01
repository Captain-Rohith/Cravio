package com.javacravio.cravio.restaurant.controller;

import com.javacravio.cravio.common.dto.ApiResponse;
import com.javacravio.cravio.restaurant.dto.MenuItemRequest;
import com.javacravio.cravio.restaurant.dto.MenuItemResponse;
import com.javacravio.cravio.restaurant.dto.RestaurantRequest;
import com.javacravio.cravio.restaurant.dto.RestaurantResponse;
import com.javacravio.cravio.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> create(@Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created", restaurantService.createRestaurant(request)));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getById(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant fetched", restaurantService.getRestaurantById(restaurantId)));
    }

    @PutMapping("/{restaurantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> update(
            @PathVariable Long restaurantId,
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated", restaurantService.updateRestaurant(restaurantId, request)));
    }

    @DeleteMapping("/{restaurantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long restaurantId) {
        restaurantService.deleteRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Restaurant deleted", null));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return ResponseEntity.ok(ApiResponse.success("Restaurants fetched", restaurantService.nearbyRestaurants(latitude, longitude)));
    }

    @PostMapping("/{restaurantId}/menu")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item added", restaurantService.addMenuItem(restaurantId, request)));
    }

    @GetMapping("/{restaurantId}/menu/{menuItemId}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId) {
        return ResponseEntity.ok(ApiResponse.success("Menu item fetched", restaurantService.getMenuItem(restaurantId, menuItemId)));
    }

    @PutMapping("/{restaurantId}/menu/{menuItemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Menu item updated", restaurantService.updateMenuItem(restaurantId, menuItemId, request)));
    }

    @DeleteMapping("/{restaurantId}/menu/{menuItemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId) {
        restaurantService.deleteMenuItem(restaurantId, menuItemId);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted", null));
    }

    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> menu(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Menu fetched", restaurantService.getMenu(restaurantId)));
    }
}


