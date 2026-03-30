package com.javacravio.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.tracking.util.H3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private H3Service h3Service;

    @Mock
    private ObjectMapper objectMapper;

    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        trackingService = new TrackingService(redisTemplate, new ChannelTopic("tracking-location-events"), h3Service, objectMapper);
    }

    @Test
    void processLocationUpdateWritesRedisAndPublishesEvent() throws Exception {
        LocationUpdateRequest request = new LocationUpdateRequest(100L, 200L, 12.34, 56.78);

        when(h3Service.toCell(12.34, 56.78)).thenReturn("123456789");
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");

        trackingService.processLocationUpdate(request);

        verify(geoOperations).add(eq("tracking:geo:order:100"), any(Point.class), eq("200"));
        verify(redisTemplate).expire("tracking:geo:order:100", Duration.ofHours(2));
        verify(hashOperations).put("tracking:h3", "200", "123456789");
        verify(redisTemplate).convertAndSend("tracking-location-events", "{\"ok\":true}");
    }
}

