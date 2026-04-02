package com.javacravio.cravio.order.repository;

import com.javacravio.cravio.order.model.Order;
import com.javacravio.cravio.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);

    List<Order> findByRestaurantId(Long restaurantId);

    List<Order> findByRestaurantIdInAndDeliveryPartnerIdIsNullAndStatusIn(
            List<Long> restaurantIds,
            Collection<OrderStatus> statuses
    );
}
