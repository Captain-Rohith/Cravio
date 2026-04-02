package com.javacravio.cravio.order.service;

import com.javacravio.cravio.common.exception.BusinessException;
import com.javacravio.cravio.common.exception.NotFoundException;
import com.javacravio.cravio.common.utils.H3Utils;
import com.javacravio.cravio.order.model.Order;
import com.javacravio.cravio.order.model.OrderItem;
import com.javacravio.cravio.order.model.OrderStatus;
import com.javacravio.cravio.order.repository.OrderItemRepository;
import com.javacravio.cravio.order.repository.OrderRepository;
import com.javacravio.cravio.payment.service.PaymentService;
import com.javacravio.cravio.restaurant.model.Restaurant;
import com.javacravio.cravio.restaurant.repository.MenuItemRepository;
import com.javacravio.cravio.restaurant.repository.RestaurantRepository;
import com.javacravio.cravio.user.model.Role;
import com.javacravio.cravio.user.model.User;
import com.javacravio.cravio.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private H3Utils h3Utils;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void configureDiscoveryDefaults() {
        ReflectionTestUtils.setField(orderService, "deliveryDiscoveryRingSize", 2);
        ReflectionTestUtils.setField(orderService, "deliveryDiscoveryRadiusKm", 5.0);
    }

    @Test
    void getNearbyAvailableOrdersShouldReturnUnassignedOrdersInVicinity() {
        Restaurant restaurant = new Restaurant();
        ReflectionTestUtils.setField(restaurant, "id", 10L);
        restaurant.setName("Spice Hub");
        restaurant.setLatitude(12.97);
        restaurant.setLongitude(77.59);
        restaurant.setH3Index("cell-restaurant");

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 101L);
        order.setCustomerId(1L);
        order.setRestaurantId(10L);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(540.0);

        OrderItem item = new OrderItem();
        item.setOrderId(101L);
        item.setMenuItemId(501L);
        item.setQuantity(2);
        item.setUnitPrice(270.0);

        when(h3Utils.toCell(12.9716, 77.5946)).thenReturn("cell-driver");
        when(h3Utils.nearbyCells("cell-driver", anyInt())).thenReturn(Set.of("cell-driver", "cell-restaurant"));
        when(restaurantRepository.findByH3IndexIn(anySet())).thenReturn(List.of(restaurant));
        when(orderRepository.findByRestaurantIdInAndDeliveryPartnerIdIsNullAndStatusIn(List.of(10L), Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING)))
                .thenReturn(List.of(order));
        when(orderItemRepository.findByOrderId(101L)).thenReturn(List.of(item));

        var nearby = orderService.getNearbyAvailableOrders(12.9716, 77.5946);

        assertEquals(1, nearby.size());
        assertEquals(101L, nearby.getFirst().orderId());
        assertEquals("Spice Hub", nearby.getFirst().restaurantName());
    }

    @Test
    void claimOrderShouldAssignOrderToLoggedInDeliveryPartner() {
        User deliveryPartner = new User();
        ReflectionTestUtils.setField(deliveryPartner, "id", 99L);
        deliveryPartner.setEmail("rider@cravio.com");
        deliveryPartner.setRole(Role.DELIVERY_PARTNER);

        Restaurant restaurant = new Restaurant();
        ReflectionTestUtils.setField(restaurant, "id", 10L);
        restaurant.setH3Index("cell-restaurant");
        restaurant.setLatitude(12.9720);
        restaurant.setLongitude(77.5940);

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 101L);
        order.setRestaurantId(10L);
        order.setStatus(OrderStatus.CONFIRMED);

        Order claimedOrder = new Order();
        ReflectionTestUtils.setField(claimedOrder, "id", 101L);
        claimedOrder.setRestaurantId(10L);
        claimedOrder.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        claimedOrder.setDeliveryPartnerId(99L);

        when(userRepository.findByEmail("rider@cravio.com")).thenReturn(Optional.of(deliveryPartner));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order), Optional.of(claimedOrder));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(h3Utils.toCell(12.9716, 77.5946)).thenReturn("cell-driver");
        when(h3Utils.nearbyCells("cell-driver", anyInt())).thenReturn(Set.of("cell-driver", "cell-restaurant"));
        when(orderRepository.claimOrderIfAvailable(
                eq(101L),
                eq(99L),
                eq(OrderStatus.OUT_FOR_DELIVERY),
                eq(Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING))
        )).thenReturn(1);
        when(orderItemRepository.findByOrderId(101L)).thenReturn(List.of());

        var claimed = orderService.claimOrder(101L, "rider@cravio.com", 12.9716, 77.5946);

        assertEquals(99L, claimed.deliveryPartnerId());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, claimed.status());
        verify(orderRepository).claimOrderIfAvailable(
                eq(101L),
                eq(99L),
                eq(OrderStatus.OUT_FOR_DELIVERY),
                eq(Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING))
        );
    }

    @Test
    void claimOrderShouldRejectWhenOutsideVicinity() {
        User deliveryPartner = new User();
        deliveryPartner.setRole(Role.DELIVERY_PARTNER);

        Restaurant restaurant = new Restaurant();
        restaurant.setH3Index("cell-restaurant");
        restaurant.setLatitude(12.9720);
        restaurant.setLongitude(77.5940);

        Order order = new Order();
        order.setRestaurantId(10L);
        order.setStatus(OrderStatus.CONFIRMED);

        when(userRepository.findByEmail("rider@cravio.com")).thenReturn(Optional.of(deliveryPartner));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(h3Utils.toCell(12.9716, 77.5946)).thenReturn("cell-driver");
        when(h3Utils.nearbyCells("cell-driver", anyInt())).thenReturn(Set.of("cell-driver"));

        assertThrows(BusinessException.class,
                () -> orderService.claimOrder(101L, "rider@cravio.com", 12.9716, 77.5946));
    }

    @Test
    void claimOrderShouldThrowWhenUserIsNotDeliveryPartner() {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@cravio.com")).thenReturn(Optional.of(admin));

        assertThrows(BusinessException.class,
                () -> orderService.claimOrder(101L, "admin@cravio.com", 12.9716, 77.5946));
    }

    @Test
    void claimOrderShouldThrowWhenOrderMissing() {
        User deliveryPartner = new User();
        deliveryPartner.setRole(Role.DELIVERY_PARTNER);
        when(userRepository.findByEmail("rider@cravio.com")).thenReturn(Optional.of(deliveryPartner));
        when(orderRepository.findById(101L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.claimOrder(101L, "rider@cravio.com", 12.9716, 77.5946));
    }

    @Test
    void claimOrderShouldThrowWhenAnotherPartnerClaimsFirst() {
        User deliveryPartner = new User();
        ReflectionTestUtils.setField(deliveryPartner, "id", 99L);
        deliveryPartner.setRole(Role.DELIVERY_PARTNER);

        Restaurant restaurant = new Restaurant();
        ReflectionTestUtils.setField(restaurant, "id", 10L);
        restaurant.setH3Index("cell-restaurant");
        restaurant.setLatitude(12.9720);
        restaurant.setLongitude(77.5940);

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 101L);
        order.setRestaurantId(10L);
        order.setStatus(OrderStatus.CONFIRMED);

        Order alreadyClaimed = new Order();
        ReflectionTestUtils.setField(alreadyClaimed, "id", 101L);
        alreadyClaimed.setRestaurantId(10L);
        alreadyClaimed.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        alreadyClaimed.setDeliveryPartnerId(55L);

        when(userRepository.findByEmail("rider@cravio.com")).thenReturn(Optional.of(deliveryPartner));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order), Optional.of(alreadyClaimed));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(h3Utils.toCell(12.9716, 77.5946)).thenReturn("cell-driver");
        when(h3Utils.nearbyCells("cell-driver", anyInt())).thenReturn(Set.of("cell-driver", "cell-restaurant"));
        when(orderRepository.claimOrderIfAvailable(
                eq(101L),
                eq(99L),
                eq(OrderStatus.OUT_FOR_DELIVERY),
                eq(Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING))
        )).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> orderService.claimOrder(101L, "rider@cravio.com", 12.9716, 77.5946));
    }

    @Test
    void getNearbyAvailableOrdersShouldExcludeRestaurantsOutsideRadius() {
        Restaurant farRestaurant = new Restaurant();
        ReflectionTestUtils.setField(farRestaurant, "id", 20L);
        farRestaurant.setName("Far Away Kitchen");
        farRestaurant.setLatitude(13.2000);
        farRestaurant.setLongitude(77.9000);
        farRestaurant.setH3Index("cell-restaurant-far");

        Order farOrder = new Order();
        ReflectionTestUtils.setField(farOrder, "id", 202L);
        farOrder.setCustomerId(2L);
        farOrder.setRestaurantId(20L);
        farOrder.setStatus(OrderStatus.CONFIRMED);

        when(h3Utils.toCell(12.9716, 77.5946)).thenReturn("cell-driver");
        when(h3Utils.nearbyCells("cell-driver", anyInt())).thenReturn(Set.of("cell-driver", "cell-restaurant-far"));
        when(restaurantRepository.findByH3IndexIn(anySet())).thenReturn(List.of(farRestaurant));
        when(orderRepository.findByRestaurantIdInAndDeliveryPartnerIdIsNullAndStatusIn(List.of(20L), Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING)))
                .thenReturn(List.of(farOrder));

        var nearby = orderService.getNearbyAvailableOrders(12.9716, 77.5946);

        assertEquals(0, nearby.size());
    }

    @Test
    void claimOrderShouldRejectWhenOutsideRadiusEvenIfVicinityMatches() {
        User deliveryPartner = new User();
        ReflectionTestUtils.setField(deliveryPartner, "id", 99L);
        deliveryPartner.setRole(Role.DELIVERY_PARTNER);

        Restaurant farRestaurant = new Restaurant();
        ReflectionTestUtils.setField(farRestaurant, "id", 10L);
        farRestaurant.setH3Index("cell-restaurant");
        farRestaurant.setLatitude(13.2000);
        farRestaurant.setLongitude(77.9000);

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 101L);
        order.setRestaurantId(10L);
        order.setStatus(OrderStatus.CONFIRMED);

        when(userRepository.findByEmail("rider@cravio.com")).thenReturn(Optional.of(deliveryPartner));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(farRestaurant));
        when(h3Utils.toCell(12.9716, 77.5946)).thenReturn("cell-driver");
        when(h3Utils.nearbyCells("cell-driver", anyInt())).thenReturn(Set.of("cell-driver", "cell-restaurant"));

        assertThrows(BusinessException.class,
                () -> orderService.claimOrder(101L, "rider@cravio.com", 12.9716, 77.5946));
    }
}
