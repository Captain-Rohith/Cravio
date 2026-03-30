package com.javacravio.tracking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.tracking.dto.TrackingEvent;
import com.javacravio.tracking.util.H3Service;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TrackingService {

    private static final Duration TRACKING_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic trackingTopic;
    private final H3Service h3Service;
    private final ObjectMapper objectMapper;

    public TrackingService(
            StringRedisTemplate redisTemplate,
            ChannelTopic trackingTopic,
            H3Service h3Service,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.trackingTopic = trackingTopic;
        this.h3Service = h3Service;
        this.objectMapper = objectMapper;
    }

    public void processLocationUpdate(LocationUpdateRequest request) {
        String h3Index = h3Service.toCell(request.latitude(), request.longitude());
        String geoKey = geoKey(request.orderId());
        String partnerId = String.valueOf(request.deliveryPartnerId());

        redisTemplate.opsForGeo().add(
                geoKey,
                new Point(request.longitude(), request.latitude()),
                partnerId
        );

        redisTemplate.expire(geoKey, TRACKING_TTL);
        redisTemplate.opsForHash().put("tracking:h3", partnerId, h3Index);

        TrackingEvent event = new TrackingEvent(
                request.orderId(),
                request.deliveryPartnerId(),
                request.latitude(),
                request.longitude(),
                h3Index
        );

        try {
            redisTemplate.convertAndSend(trackingTopic.getTopic(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize tracking event", ex);
        }
    }

    private String geoKey(Long orderId) {
        return "tracking:geo:order:" + orderId;
    }
}

