# Design Decisions

## 1) Modular Monolith + Tracking Microservice

**Decision**: Keep core business modules in one deployable monolith and isolate realtime tracking as a separate service.

**Why**:
- Business modules are tightly coupled around transactional consistency.
- Tracking has different scalability profile (high-frequency writes + fan-out).
- Separate tracking runtime allows independent scaling and deployment cadence.

## 2) MVC + DTO-only API contracts

**Decision**: Enforce controller/service/repository separation and never expose JPA entities directly.

**Why**:
- Keeps HTTP contracts stable even when persistence model changes.
- Improves testability and limits accidental data leaks.

## 3) Lightweight tracking storage

**Decision**:
- Keep tracking-service state in memory and broadcast updates directly over WebSocket topics.

**Why**:
- Removes external infrastructure dependency for current scope.
- Keeps realtime order tracking behavior available during local development.

## 4) H3 as geospatial indexing strategy

**Decision**: Convert lat/lng to H3 cell indexes for restaurants and tracking updates.

**Why**:
- Fast proximity queries using neighboring cells.
- Better partitioning characteristics than naive bounding boxes.

## 5) Security model

**Decision**: JWT-based stateless auth with method-level role checks.

**Why**:
- Simple horizontal scaling and no sticky sessions.
- Clear authorization boundaries per endpoint.

## 6) Tracking integration reliability

**Decision**: Monolith tracking client retries failed network calls up to configurable max attempts.

**Why**:
- Handles transient network faults without immediate request failure.
- Keeps behavior explicit and configurable (`cravio.tracking.max-attempts`).

## 7) Current limitations and next improvements

- Shared tracking contracts are duplicated between services; a shared contract artifact is recommended.
- Payment integration is mocked; replace with provider adapter and idempotency keys.
- Add circuit breaker, distributed tracing, and durable tracking storage if horizontal scaling is needed.

