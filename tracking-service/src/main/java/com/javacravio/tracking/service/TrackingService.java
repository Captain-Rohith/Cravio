package com.javacravio.tracking.service;

import com.javacravio.tracking.dto.CustomerLocationUpdateRequest;
import com.javacravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.tracking.dto.TrackingEvent;
import com.javacravio.tracking.util.H3Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackingService {

    private final H3Service h3Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, TrackingEvent> latestByOrderId = new ConcurrentHashMap<>();
    private final Map<Long, TrackingEvent> latestCustomerByOrderId = new ConcurrentHashMap<>();

    public TrackingService(
            H3Service h3Service,
            SimpMessagingTemplate messagingTemplate) {
        this.h3Service = h3Service;
        this.messagingTemplate = messagingTemplate;
    }

    public void processLocationUpdate(LocationUpdateRequest request) {
        String h3Index = h3Service.toCell(request.latitude(), request.longitude());

        TrackingEvent event = new TrackingEvent(
                request.orderId(),
                request.deliveryPartnerId(),
                null,
                request.latitude(),
                request.longitude(),
                h3Index
        );

        latestByOrderId.put(request.orderId(), event);
        messagingTemplate.convertAndSend("/topic/orders/" + request.orderId(), event);
    }

    public TrackingEvent getLatestForOrder(Long orderId) {
        return latestByOrderId.get(orderId);
    }

    public void processCustomerLocationUpdate(CustomerLocationUpdateRequest request) {
        String h3Index = h3Service.toCell(request.latitude(), request.longitude());

        TrackingEvent event = new TrackingEvent(
                request.orderId(),
                null,
                request.customerId(),
                request.latitude(),
                request.longitude(),
                h3Index
        );

        latestCustomerByOrderId.put(request.orderId(), event);
        messagingTemplate.convertAndSend("/topic/orders/" + request.orderId() + "/customer", event);
    }

    public TrackingEvent getLatestCustomerForOrder(Long orderId) {
        return latestCustomerByOrderId.get(orderId);
    }
}

