package com.javacravio.cravio.restaurant.service;

import com.javacravio.cravio.common.exception.NotFoundException;
import com.javacravio.cravio.common.utils.H3Utils;
import com.javacravio.cravio.restaurant.dto.MenuItemRequest;
import com.javacravio.cravio.restaurant.dto.MenuItemResponse;
import com.javacravio.cravio.restaurant.dto.RestaurantRequest;
import com.javacravio.cravio.restaurant.dto.RestaurantResponse;
import com.javacravio.cravio.restaurant.model.MenuItem;
import com.javacravio.cravio.restaurant.model.Restaurant;
import com.javacravio.cravio.restaurant.repository.MenuItemRepository;
import com.javacravio.cravio.restaurant.repository.RestaurantRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final H3Utils h3Utils;

    public RestaurantServiceImpl(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository, H3Utils h3Utils) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.h3Utils = h3Utils;
    }

    @Override
    @CacheEvict(value = "nearby-restaurants", allEntries = true)
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.name());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3Index(h3Utils.toCell(request.latitude(), request.longitude()));
        Restaurant saved = restaurantRepository.save(restaurant);
        return new RestaurantResponse(saved.getId(), saved.getName(), saved.getLatitude(), saved.getLongitude());
    }

    @Override
    @Cacheable(value = "nearby-restaurants", key = "#latitude + ':' + #longitude")
    public List<RestaurantResponse> nearbyRestaurants(double latitude, double longitude) {
        String userCell = h3Utils.toCell(latitude, longitude);
        return restaurantRepository.findByH3IndexIn(h3Utils.nearbyCells(userCell, 2)).stream()
                .map(r -> new RestaurantResponse(r.getId(), r.getName(), r.getLatitude(), r.getLongitude()))
                .toList();
    }

    @Override
    @CacheEvict(value = "restaurant-menu", key = "#restaurantId")
    public MenuItemResponse addMenuItem(Long restaurantId, MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(request.name());
        item.setPrice(request.price());
        MenuItem saved = menuItemRepository.save(item);
        return new MenuItemResponse(saved.getId(), saved.getName(), saved.getPrice());
    }

    @Override
    @Cacheable(value = "restaurant-menu", key = "#restaurantId")
    public List<MenuItemResponse> getMenu(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new NotFoundException("Restaurant not found");
        }
        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(item -> new MenuItemResponse(item.getId(), item.getName(), item.getPrice()))
                .toList();
    }
}

