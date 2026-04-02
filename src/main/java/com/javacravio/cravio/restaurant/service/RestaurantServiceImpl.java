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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public RestaurantResponse createRestaurant(RestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.name());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3Index(h3Utils.toCell(request.latitude(), request.longitude()));
        return toRestaurantResponse(restaurantRepository.save(restaurant));
    }

    @Override
    public RestaurantResponse getRestaurantById(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        return toRestaurantResponse(restaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(Long restaurantId, RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

        restaurant.setName(request.name());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setH3Index(h3Utils.toCell(request.latitude(), request.longitude()));

        return toRestaurantResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long restaurantId) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new NotFoundException("Restaurant not found");
        }
        menuItemRepository.deleteByRestaurantId(restaurantId);
        restaurantRepository.deleteById(restaurantId);
    }

    @Override
    public List<RestaurantResponse> nearbyRestaurants(double latitude, double longitude) {
        String userCell = h3Utils.toCell(latitude, longitude);
        return restaurantRepository.findByH3IndexIn(h3Utils.nearbyCells(userCell, 2)).stream()
                .map(this::toRestaurantResponse)
                .toList();
    }

    @Override
    public MenuItemResponse addMenuItem(Long restaurantId, MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(request.name());
        item.setPrice(request.price());
        return toMenuItemResponse(menuItemRepository.save(item));
    }

    @Override
    public MenuItemResponse getMenuItem(Long restaurantId, Long menuItemId) {
        return toMenuItemResponse(getMenuItemEntity(restaurantId, menuItemId));
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(Long restaurantId, Long menuItemId, MenuItemRequest request) {
        MenuItem item = getMenuItemEntity(restaurantId, menuItemId);
        item.setName(request.name());
        item.setPrice(request.price());
        return toMenuItemResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long restaurantId, Long menuItemId) {
        MenuItem item = getMenuItemEntity(restaurantId, menuItemId);
        menuItemRepository.delete(item);
    }

    @Override
    public List<MenuItemResponse> getMenu(Long restaurantId) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new NotFoundException("Restaurant not found");
        }
        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::toMenuItemResponse)
                .toList();
    }

    private MenuItem getMenuItemEntity(Long restaurantId, Long menuItemId) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new NotFoundException("Restaurant not found");
        }
        return menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
                .orElseThrow(() -> new NotFoundException("Menu item not found"));
    }

    private RestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        return new RestaurantResponse(restaurant.getId(), restaurant.getName(), restaurant.getLatitude(), restaurant.getLongitude());
    }

    private MenuItemResponse toMenuItemResponse(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getName(), item.getPrice());
    }
}

