package com.javacravio.cravio.restaurant.service;

import com.javacravio.cravio.restaurant.dto.MenuItemRequest;
import com.javacravio.cravio.restaurant.dto.MenuItemResponse;
import com.javacravio.cravio.restaurant.dto.RestaurantRequest;
import com.javacravio.cravio.restaurant.dto.RestaurantResponse;

import java.util.List;

public interface RestaurantService {

    RestaurantResponse createRestaurant(RestaurantRequest request);

    List<RestaurantResponse> nearbyRestaurants(double latitude, double longitude);

    MenuItemResponse addMenuItem(Long restaurantId, MenuItemRequest request);

    List<MenuItemResponse> getMenu(Long restaurantId);
}

