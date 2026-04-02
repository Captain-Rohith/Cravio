package com.javacravio.cravio.restaurant.service;

import com.javacravio.cravio.common.exception.NotFoundException;
import com.javacravio.cravio.common.utils.H3Utils;
import com.javacravio.cravio.restaurant.dto.MenuItemRequest;
import com.javacravio.cravio.restaurant.dto.RestaurantRequest;
import com.javacravio.cravio.restaurant.model.MenuItem;
import com.javacravio.cravio.restaurant.model.Restaurant;
import com.javacravio.cravio.restaurant.repository.MenuItemRepository;
import com.javacravio.cravio.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private H3Utils h3Utils;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    @Test
    void updateRestaurantShouldUpdateLocationAndH3Index() {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Old");
        restaurant.setLatitude(10.0);
        restaurant.setLongitude(20.0);

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(h3Utils.toCell(12.9, 77.5)).thenReturn("12345");
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = restaurantService.updateRestaurant(1L, new RestaurantRequest("New Name", 12.9, 77.5));

        assertEquals("New Name", response.name());
        assertEquals(12.9, response.latitude());
        assertEquals(77.5, response.longitude());
        assertEquals("12345", restaurant.getH3Index());
    }

    @Test
    void deleteRestaurantShouldDeleteMenuItemsBeforeRestaurant() {
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(new Restaurant()));

        restaurantService.deleteRestaurant(10L);

        InOrder inOrder = inOrder(menuItemRepository, restaurantRepository);
        inOrder.verify(menuItemRepository).deleteByRestaurantId(10L);
        inOrder.verify(restaurantRepository).deleteById(10L);
    }

    @Test
    void updateMenuItemShouldThrowWhenMenuItemMissing() {
        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(new Restaurant()));
        when(menuItemRepository.findByIdAndRestaurantId(9L, 5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> restaurantService.updateMenuItem(5L, 9L, new MenuItemRequest("Paneer", 199.0)));
    }
}

