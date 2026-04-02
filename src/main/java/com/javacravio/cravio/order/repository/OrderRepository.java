package com.javacravio.cravio.order.repository;

import com.javacravio.cravio.order.model.Order;
import com.javacravio.cravio.order.model.OrderStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);

    List<Order> findByRestaurantId(Long restaurantId);

    List<Order> findByRestaurantIdInAndDeliveryPartnerIdIsNullAndStatusIn(
            List<Long> restaurantIds,
            Collection<OrderStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Order o
            set o.deliveryPartnerId = :deliveryPartnerId,
                o.status = :claimedStatus
            where o.id = :orderId
              and o.deliveryPartnerId is null
              and o.status in :claimableStatuses
            """)
    int claimOrderIfAvailable(
            @Param("orderId") Long orderId,
            @Param("deliveryPartnerId") Long deliveryPartnerId,
            @Param("claimedStatus") OrderStatus claimedStatus,
            @Param("claimableStatuses") Collection<OrderStatus> claimableStatuses
    );
}
