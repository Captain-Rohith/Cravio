package com.javacravio.tracking.service;

import com.javacravio.tracking.dto.CustomerLocationUpdateRequest;
import com.javacravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.tracking.dto.TrackingEvent;
import com.javacravio.tracking.util.H3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private H3Service h3Service;

    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        trackingService = new TrackingService(h3Service, messagingTemplate);
    }

    @Test
    void processLocationUpdatePublishesEventToOrderTopic() {
        LocationUpdateRequest request = new LocationUpdateRequest(100L, 200L, 12.34, 56.78);

        when(h3Service.toCell(12.34, 56.78)).thenReturn("123456789");

        trackingService.processLocationUpdate(request);

        verify(messagingTemplate).convertAndSend(eq("/topic/orders/100"), any(TrackingEvent.class));
    }

    @Test
    void processLocationUpdateStoresLatestLocationForOrder() {
        LocationUpdateRequest request = new LocationUpdateRequest(100L, 200L, 12.34, 56.78);

        when(h3Service.toCell(12.34, 56.78)).thenReturn("123456789");

        trackingService.processLocationUpdate(request);

        TrackingEvent latest = trackingService.getLatestForOrder(100L);
        assertNotNull(latest);
        assertEquals(100L, latest.orderId());
        assertEquals(200L, latest.deliveryPartnerId());
        assertNull(latest.customerId());
        assertEquals(12.34, latest.latitude());
        assertEquals(56.78, latest.longitude());
    }

    @Test
    void processCustomerLocationUpdateStoresLatestCustomerLocationForOrder() {
        CustomerLocationUpdateRequest request = new CustomerLocationUpdateRequest(100L, 300L, 12.35, 56.79);

        when(h3Service.toCell(12.35, 56.79)).thenReturn("987654321");

        trackingService.processCustomerLocationUpdate(request);

        TrackingEvent latest = trackingService.getLatestCustomerForOrder(100L);
        assertNotNull(latest);
        assertEquals(100L, latest.orderId());
        assertNull(latest.deliveryPartnerId());
        assertEquals(300L, latest.customerId());
        assertEquals(12.35, latest.latitude());
        assertEquals(56.79, latest.longitude());
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/100/customer"), any(TrackingEvent.class));
    }
}

