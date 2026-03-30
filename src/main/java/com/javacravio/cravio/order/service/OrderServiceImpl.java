package com.javacravio.cravio.order.service;

import com.javacravio.cravio.common.exception.NotFoundException;
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
import com.javacravio.cravio.restaurant.repository.MenuItemRepository;
import com.javacravio.cravio.restaurant.repository.RestaurantRepository;
import com.javacravio.cravio.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            UserRepository userRepository,
            PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        if (!userRepository.existsById(request.customerId())) {
            throw new NotFoundException("Customer not found");
        }
        if (!restaurantRepository.existsById(request.restaurantId())) {
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
    public OrderResponse assignDeliveryPartner(Long orderId, Long deliveryPartnerId) {
        if (!userRepository.existsById(deliveryPartnerId)) {
            throw new NotFoundException("Delivery partner not found");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setDeliveryPartnerId(deliveryPartnerId);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        Order saved = orderRepository.save(order);
        return toResponse(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(item.getMenuItemId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
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
}

