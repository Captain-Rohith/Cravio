package com.javacravio.cravio.order.service;

import com.javacravio.cravio.common.exception.BusinessException;
import com.javacravio.cravio.common.exception.NotFoundException;
import com.javacravio.cravio.common.utils.H3Utils;
import com.javacravio.cravio.order.dto.NearbyOrderResponse;
import com.javacravio.cravio.order.dto.OrderItemResponse;
import com.javacravio.cravio.order.dto.OrderResponse;
import com.javacravio.cravio.order.dto.PlaceOrderItemRequest;
import com.javacravio.cravio.order.dto.PlaceOrderRequest;
import com.javacravio.cravio.order.model.Order;
import com.javacravio.cravio.order.model.OrderItem;
import com.javacravio.cravio.order.model.OrderStatus;
import com.javacravio.cravio.order.repository.OrderItemRepository;
import com.javacravio.cravio.order.repository.OrderRepository;
import com.javacravio.cravio.payment.dto.PaymentRequest;
import com.javacravio.cravio.payment.dto.PaymentResponse;
import com.javacravio.cravio.payment.model.PaymentStatus;
import com.javacravio.cravio.payment.service.PaymentService;
import com.javacravio.cravio.restaurant.model.MenuItem;
import com.javacravio.cravio.restaurant.model.Restaurant;
import com.javacravio.cravio.restaurant.repository.MenuItemRepository;
import com.javacravio.cravio.restaurant.repository.RestaurantRepository;
import com.javacravio.cravio.user.model.Role;
import com.javacravio.cravio.user.model.User;
import com.javacravio.cravio.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Set<OrderStatus> DELIVERY_CLAIMABLE_STATUSES = Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final H3Utils h3Utils;
    private final int deliveryDiscoveryRingSize;
    private final double deliveryDiscoveryRadiusKm;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            UserRepository userRepository,
            PaymentService paymentService,
            H3Utils h3Utils,
            @Value("${cravio.delivery.discovery-ring-size:2}") int deliveryDiscoveryRingSize,
            @Value("${cravio.delivery.discovery-radius-km:5.0}") double deliveryDiscoveryRadiusKm) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.h3Utils = h3Utils;
        this.deliveryDiscoveryRingSize = Math.max(0, deliveryDiscoveryRingSize);
        this.deliveryDiscoveryRadiusKm = Math.max(0.1, deliveryDiscoveryRadiusKm);
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        if (userRepository.findById(request.customerId()).isEmpty()) {
            throw new NotFoundException("Customer not found");
        }
        if (restaurantRepository.findById(request.restaurantId()).isEmpty()) {
            throw new NotFoundException("Restaurant not found");
        }

        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setRestaurantId(request.restaurantId());
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setTotalAmount(0);
        Order savedOrder = orderRepository.save(order);

        double total = 0;
        List<OrderItem> persistedItems = new ArrayList<>();
        for (PlaceOrderItemRequest itemRequest : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.menuItemId())
                    .orElseThrow(() -> new NotFoundException("Menu item not found: " + itemRequest.menuItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setMenuItemId(menuItem.getId());
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setUnitPrice(menuItem.getPrice());
            persistedItems.add(orderItemRepository.save(orderItem));
            total += menuItem.getPrice() * itemRequest.quantity();
        }

        savedOrder.setTotalAmount(total);
        PaymentResponse paymentResponse = paymentService.processPayment(new PaymentRequest(savedOrder.getId(), total));
        savedOrder.setStatus(paymentResponse.status() == PaymentStatus.SUCCESS ? OrderStatus.CONFIRMED : OrderStatus.CANCELLED);

        return toResponse(orderRepository.save(savedOrder), persistedItems);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order, orderItemRepository.findByOrderId(orderId));
    }

    @Override
    public List<OrderResponse> getCustomerOrders(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(order -> toResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @Override
    public List<OrderResponse> getRestaurantOrders(Long restaurantId) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new NotFoundException("Restaurant not found");
        }
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(order -> toResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @Override
    public List<NearbyOrderResponse> getNearbyAvailableOrders(double latitude, double longitude) {
        String currentCell = h3Utils.toCell(latitude, longitude);
        Set<String> nearbyCells = h3Utils.nearbyCells(currentCell, deliveryDiscoveryRingSize);

        List<Restaurant> nearbyRestaurants = restaurantRepository.findByH3IndexIn(nearbyCells);
        if (nearbyRestaurants.isEmpty()) {
            return List.of();
        }

        List<Restaurant> radiusFilteredRestaurants = nearbyRestaurants.stream()
                .filter(restaurant -> isWithinDiscoveryRadius(
                        latitude,
                        longitude,
                        restaurant.getLatitude(),
                        restaurant.getLongitude()
                ))
                .toList();

        if (radiusFilteredRestaurants.isEmpty()) {
            return List.of();
        }

        Map<Long, Restaurant> nearbyRestaurantsById = radiusFilteredRestaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, Function.identity()));

        List<Long> nearbyRestaurantIds = radiusFilteredRestaurants.stream()
                .map(Restaurant::getId)
                .toList();

        return orderRepository.findByRestaurantIdInAndDeliveryPartnerIdIsNullAndStatusIn(nearbyRestaurantIds, DELIVERY_CLAIMABLE_STATUSES)
                .stream()
                .map(order -> {
                    Restaurant pickupRestaurant = nearbyRestaurantsById.get(order.getRestaurantId());
                    return toNearbyOrderResponse(order, orderItemRepository.findByOrderId(order.getId()), pickupRestaurant);
                })
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        return toResponse(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    @Override
    @Transactional
    public OrderResponse updateStatusByRestaurant(Long restaurantId, Long orderId, OrderStatus status) {
        if (restaurantRepository.findById(restaurantId).isEmpty()) {
            throw new NotFoundException("Restaurant not found");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!restaurantId.equals(order.getRestaurantId())) {
            throw new BusinessException("Order does not belong to this restaurant");
        }

        order.setStatus(status);
        Order saved = orderRepository.save(order);
        return toResponse(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    @Override
    @Transactional
    public OrderResponse cancelByCustomer(Long customerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!customerId.equals(order.getCustomerId())) {
            throw new BusinessException("Order does not belong to this customer");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return toResponse(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    @Override
    @Transactional
    public OrderResponse assignDeliveryPartner(Long orderId, Long deliveryPartnerId) {
        if (userRepository.findById(deliveryPartnerId).isEmpty()) {
            throw new NotFoundException("Delivery partner not found");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setDeliveryPartnerId(deliveryPartnerId);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        Order saved = orderRepository.save(order);
        return toResponse(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    @Override
    @Transactional
    public OrderResponse claimOrder(Long orderId, String deliveryPartnerEmail, double latitude, double longitude) {
        User deliveryPartner = userRepository.findByEmail(deliveryPartnerEmail)
                .orElseThrow(() -> new NotFoundException("Delivery partner not found"));

        if (deliveryPartner.getRole() != Role.DELIVERY_PARTNER) {
            throw new BusinessException("Only delivery partners can claim orders");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getDeliveryPartnerId() != null) {
            throw new BusinessException("Order is already claimed");
        }

        if (!DELIVERY_CLAIMABLE_STATUSES.contains(order.getStatus())) {
            throw new BusinessException("Order is not available for claiming in current status: " + order.getStatus());
        }

        Restaurant pickupRestaurant = restaurantRepository.findById(order.getRestaurantId())
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

        if (!isWithinDiscoveryRadius(latitude, longitude, pickupRestaurant.getLatitude(), pickupRestaurant.getLongitude())) {
            throw new BusinessException("Order is outside delivery partner radius");
        }

        String deliveryPartnerCell = h3Utils.toCell(latitude, longitude);
        Set<String> allowedCells = h3Utils.nearbyCells(deliveryPartnerCell, deliveryDiscoveryRingSize);
        if (!allowedCells.contains(pickupRestaurant.getH3Index())) {
            throw new BusinessException("Order is outside delivery partner vicinity");
        }

        int updatedRows = orderRepository.claimOrderIfAvailable(
                orderId,
                deliveryPartner.getId(),
                OrderStatus.OUT_FOR_DELIVERY,
                DELIVERY_CLAIMABLE_STATUSES
        );

        if (updatedRows == 0) {
            Order latest = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NotFoundException("Order not found"));

            if (latest.getDeliveryPartnerId() != null) {
                throw new BusinessException("Order was claimed by another delivery partner");
            }

            if (!DELIVERY_CLAIMABLE_STATUSES.contains(latest.getStatus())) {
                throw new BusinessException("Order is not available for claiming in current status: " + latest.getStatus());
            }

            throw new BusinessException("Order is no longer available for claiming");
        }

        Order saved = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = toOrderItemResponses(items);
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getDeliveryPartnerId(),
                order.getTotalAmount(),
                order.getStatus(),
                itemResponses
        );
    }

    private NearbyOrderResponse toNearbyOrderResponse(Order order, List<OrderItem> items, Restaurant restaurant) {
        if (restaurant == null) {
            throw new NotFoundException("Restaurant not found for order: " + order.getId());
        }
        return new NearbyOrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                restaurant.getName(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                order.getDeliveryPartnerId(),
                order.getTotalAmount(),
                order.getStatus(),
                toOrderItemResponses(items)
        );
    }

    private List<OrderItemResponse> toOrderItemResponses(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderItemResponse(item.getMenuItemId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
    }

    private boolean isWithinDiscoveryRadius(
            double originLatitude,
            double originLongitude,
            double targetLatitude,
            double targetLongitude) {
        return distanceKm(originLatitude, originLongitude, targetLatitude, targetLongitude) <= deliveryDiscoveryRadiusKm;
    }

    private double distanceKm(
            double originLatitude,
            double originLongitude,
            double targetLatitude,
            double targetLongitude) {
        double lat1Rad = Math.toRadians(originLatitude);
        double lon1Rad = Math.toRadians(originLongitude);
        double lat2Rad = Math.toRadians(targetLatitude);
        double lon2Rad = Math.toRadians(targetLongitude);

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}

