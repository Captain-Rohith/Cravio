package com.javacravio.tracking.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacravio.tracking.dto.TrackingEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrackingSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public TrackingSubscriber(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    public void onMessage(String message) {
        try {
            TrackingEvent event = objectMapper.readValue(message, TrackingEvent.class);
            messagingTemplate.convertAndSend("/topic/orders/" + event.orderId(), event);
        } catch (Exception ignored) {
            // Ignore malformed events and keep the subscriber resilient.
        }
    }
}

