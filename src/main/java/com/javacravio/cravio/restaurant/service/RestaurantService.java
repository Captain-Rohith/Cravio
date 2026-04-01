package com.javacravio.cravio.restaurant.service;

import com.javacravio.cravio.restaurant.dto.MenuItemRequest;
import com.javacravio.cravio.restaurant.dto.MenuItemResponse;
import com.javacravio.cravio.restaurant.dto.RestaurantRequest;
import com.javacravio.cravio.restaurant.dto.RestaurantResponse;

import java.util.List;

public interface RestaurantService {

    RestaurantResponse createRestaurant(RestaurantRequest request);

    RestaurantResponse getRestaurantById(Long restaurantId);

    RestaurantResponse updateRestaurant(Long restaurantId, RestaurantRequest request);

    void deleteRestaurant(Long restaurantId);

    List<RestaurantResponse> nearbyRestaurants(double latitude, double longitude);

    MenuItemResponse addMenuItem(Long restaurantId, MenuItemRequest request);

    MenuItemResponse getMenuItem(Long restaurantId, Long menuItemId);

    MenuItemResponse updateMenuItem(Long restaurantId, Long menuItemId, MenuItemRequest request);

    void deleteMenuItem(Long restaurantId, Long menuItemId);

    List<MenuItemResponse> getMenu(Long restaurantId);
}

