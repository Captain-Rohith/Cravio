# Cravio Architecture

## High-level design

Cravio uses a **Modular Monolith + Tracking Microservice** model:

1. **Core Monolith (`cravio-core`)**
   - User module: registration/login + JWT issuance.
   - Restaurant module: restaurant/menu management + nearby discovery.
   - Order module: place/manage orders and delivery assignment.
   - Payment module: mock payment processing and retrieval.
   - Tracking integration module: forwards partner location updates to tracking service.

2. **Tracking Microservice (`tracking-service`)**
   - Receives partner location updates.
   - Converts lat/lng to H3 index.
   - Stores current location in Redis GEO.
   - Publishes updates through Redis Pub/Sub.
   - Pushes tracking events to WebSocket topics.

## Architectural principles applied

- MVC layering: `controller -> service -> repository` with DTO boundaries.
- SOLID-friendly structure: service interfaces in monolith modules, implementations isolated.
- Validation and centralized exception handling.
- No entity exposure to API contracts.

## Runtime data flow

### Order and payment flow

1. Customer places order through monolith API.
2. Order service validates customer/restaurant/items.
3. Payment service is invoked transactionally.
4. Order status transitions to `CONFIRMED` or `CANCELLED`.

### Tracking flow

1. Delivery partner calls monolith `POST /api/v1/tracking/location`.
2. Monolith forwards payload to tracking microservice.
3. Tracking microservice:
   - computes H3 cell,
   - writes Redis GEO key `tracking:geo:order:{orderId}`,
   - stores partner->H3 map in `tracking:h3`,
   - publishes event to configured tracking channel.
4. Tracking subscriber relays event to `/topic/orders/{orderId}` over WebSocket.

## Infrastructure choices

- **Oracle DB**: system of record for users, restaurants, orders, payments.
- **Redis**:
  - cache for read-heavy restaurant/menu endpoints,
  - Pub/Sub for near-realtime tracking event fan-out,
  - GEO for partner position lookup.
- **H3**: location indexing for nearby restaurant lookup and tracking indexing.

## Production hardening roadmap

- Shared contracts module between monolith and tracking service.
- Resilience (timeouts, retries, circuit breaker) for tracking integration.
- Outbox/event bus for order lifecycle events.
- Observability: structured logs, metrics, tracing.
- Containerization and environment-specific configs.

