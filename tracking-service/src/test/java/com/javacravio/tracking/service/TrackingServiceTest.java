package com.javacravio.tracking.service;

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
}

