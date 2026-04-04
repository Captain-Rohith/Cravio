package com.javacravio.cravio.tracking.service;

import com.javacravio.cravio.common.exception.BusinessException;
import com.javacravio.cravio.common.exception.NotFoundException;
import com.javacravio.cravio.order.model.Order;
import com.javacravio.cravio.order.repository.OrderRepository;
import com.javacravio.cravio.tracking.dto.CustomerLocationUpdateRequest;
import com.javacravio.cravio.tracking.dto.LocationUpdateRequest;
import com.javacravio.cravio.tracking.dto.TrackingEventResponse;
import com.javacravio.cravio.user.model.Role;
import com.javacravio.cravio.user.model.User;
import com.javacravio.cravio.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public TrackingIntegrationServiceImpl(
            UserRepository userRepository,
            OrderRepository orderRepository,
            @Value("${cravio.tracking.base-url:http://localhost:8081}") String baseUrl,
            @Value("${cravio.tracking.max-attempts:3}") int maxAttempts) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    public void forwardLocationUpdate(LocationUpdateRequest request, String principalEmail) {
        User principal = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        Long effectiveDeliveryPartnerId = request.deliveryPartnerId();

        if (principal.getRole() == Role.DELIVERY_PARTNER) {
            if (order.getDeliveryPartnerId() == null || !order.getDeliveryPartnerId().equals(principal.getId())) {
                throw new BusinessException("You can send tracking updates only for your accepted orders");
            }
            effectiveDeliveryPartnerId = principal.getId();
        }

        postWithFallback(
                new LocationUpdateRequest(
                        request.orderId(),
                        effectiveDeliveryPartnerId,
                        request.latitude(),
                        request.longitude()
                ),
                "location update",
                "Unable to push location update to tracking service",
                "/api/v1/tracking/location",
                "/tracking/api/v1/tracking/location",
                "location"
        );
    }

    @Override
    public void forwardCustomerLocationUpdate(CustomerLocationUpdateRequest request, String principalEmail) {
        User principal = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getDeliveryPartnerId() == null) {
            throw new BusinessException("Order has no assigned delivery partner yet");
        }

        if (principal.getRole() == Role.CUSTOMER && !order.getCustomerId().equals(principal.getId())) {
            throw new BusinessException("You can share location only for your own order");
        }

        postWithFallback(
                new ForwardCustomerLocationRequest(
                        request.orderId(),
                        principal.getId(),
                        request.latitude(),
                        request.longitude()
                ),
                "customer-location update",
                "Unable to push customer location update to tracking service",
                "/api/v1/tracking/customer-location",
                "/tracking/api/v1/tracking/customer-location",
                "customer-location"
        );
    }

    @Override
    public TrackingEventResponse getLatestLocationByOrderId(Long orderId, String principalEmail) {
        authorizeOrderTrackingAccess(orderId, principalEmail);
        return getWithFallback(
                orderId,
                "latest-location request",
                "Unable to fetch latest tracking location",
                "/api/v1/tracking/orders/{orderId}/latest",
                "/tracking/api/v1/tracking/orders/{orderId}/latest",
                "orders/{orderId}/latest"
        );
    }

    @Override
    public TrackingEventResponse getLatestCustomerLocationByOrderId(Long orderId, String principalEmail) {
        authorizeOrderTrackingAccess(orderId, principalEmail);
        return getWithFallback(
                orderId,
                "latest customer-location request",
                "Unable to fetch latest customer location",
                "/api/v1/tracking/orders/{orderId}/customer/latest",
                "/tracking/api/v1/tracking/orders/{orderId}/customer/latest",
                "orders/{orderId}/customer/latest"
        );
    }

    private record ForwardCustomerLocationRequest(
            Long orderId,
            Long customerId,
            double latitude,
            double longitude
    ) {
    }

    private void postWithFallback(
            Object payload,
            String rejectedActionLabel,
            String genericFailureMessage,
            String... candidateUris
    ) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean resourceAccessFailure = false;
            boolean allNotFound = true;

            for (String candidateUri : candidateUris) {
                try {
                    restClient.post()
                            .uri(candidateUri)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .toBodilessEntity();
                    return;
                } catch (RestClientResponseException ex) {
                    if (ex.getStatusCode().value() == 404) {
                        continue;
                    }
                    allNotFound = false;
                    throw new BusinessException("Tracking service rejected " + rejectedActionLabel + " with status " + ex.getStatusCode().value());
                } catch (ResourceAccessException ex) {
                    resourceAccessFailure = true;
                    allNotFound = false;
                    break;
                } catch (Exception ex) {
                    allNotFound = false;
                    throw new BusinessException(genericFailureMessage);
                }
            }

            if (resourceAccessFailure) {
                if (attempt == maxAttempts) {
                    throw new BusinessException("Tracking service is unavailable. Please retry shortly");
                }
                continue;
            }

            if (allNotFound) {
                throw new BusinessException("Tracking service rejected " + rejectedActionLabel + " with status 404");
            }
        }
    }

    private TrackingEventResponse getWithFallback(
            Long orderId,
            String rejectedActionLabel,
            String genericFailureMessage,
            String... candidateUris
    ) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean resourceAccessFailure = false;
            boolean allNotFound = true;

            for (String candidateUri : candidateUris) {
                try {
                    return restClient.get()
                            .uri(candidateUri, orderId)
                            .retrieve()
                            .body(TrackingEventResponse.class);
                } catch (RestClientResponseException ex) {
                    if (ex.getStatusCode().value() == 404) {
                        continue;
                    }
                    allNotFound = false;
                    throw new BusinessException("Tracking service rejected " + rejectedActionLabel + " with status " + ex.getStatusCode().value());
                } catch (ResourceAccessException ex) {
                    resourceAccessFailure = true;
                    allNotFound = false;
                    break;
                } catch (Exception ex) {
                    allNotFound = false;
                    throw new BusinessException(genericFailureMessage);
                }
            }

            if (resourceAccessFailure) {
                if (attempt == maxAttempts) {
                    throw new BusinessException("Tracking service is unavailable. Please retry shortly");
                }
                continue;
            }

            if (allNotFound) {
                return null;
            }
        }

        return null;
    }

    private void authorizeOrderTrackingAccess(Long orderId, String principalEmail) {
        User principal = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (principal.getRole() == Role.ADMIN) {
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (principal.getRole() == Role.CUSTOMER && order.getCustomerId().equals(principal.getId())) {
            return;
        }

        if (principal.getRole() == Role.DELIVERY_PARTNER
                && order.getDeliveryPartnerId() != null
                && order.getDeliveryPartnerId().equals(principal.getId())) {
            return;
        }

        throw new BusinessException("You are not authorized to access tracking for this order");
    }
}

