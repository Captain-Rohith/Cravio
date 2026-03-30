package com.javacravio.cravio.tracking.service;

import com.javacravio.cravio.common.exception.BusinessException;
import com.javacravio.cravio.tracking.dto.LocationUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Service
public class TrackingIntegrationServiceImpl implements TrackingIntegrationService {

    private final RestClient restClient;
    private final int maxAttempts;

    public TrackingIntegrationServiceImpl(
            @Value("${cravio.tracking.base-url:http://localhost:8081}") String baseUrl,
            @Value("${cravio.tracking.max-attempts:3}") int maxAttempts) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    public void forwardLocationUpdate(LocationUpdateRequest request) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri("/api/v1/tracking/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientResponseException ex) {
                throw new BusinessException("Tracking service rejected location update with status " + ex.getStatusCode().value());
            } catch (ResourceAccessException ex) {
                if (attempt == maxAttempts) {
                    throw new BusinessException("Tracking service is unavailable. Please retry shortly");
                }
            } catch (Exception ex) {
                throw new BusinessException("Unable to push location update to tracking service");
            }
        }
    }
}

