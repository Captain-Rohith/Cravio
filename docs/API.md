# API Documentation

Base URLs:

- Core monolith: `http://localhost:8080`
- Tracking service: `http://localhost:8081`

## Authentication

- JWT bearer token is required for protected routes.
- Public routes:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/restaurants/**`

## Core Monolith APIs (`/api/v1`)

### Auth

- `POST /auth/register`
  - Registers a new user with role.
- `POST /auth/login`
  - Returns JWT token and user details.

### Restaurants

- `POST /restaurants` (`ADMIN`)
  - Creates a restaurant and stores its H3 index.
- `GET /restaurants/nearby?latitude={lat}&longitude={lng}`
  - Returns nearby restaurants using H3 neighboring cells.
- `POST /restaurants/{restaurantId}/menu` (`ADMIN`)
  - Adds menu item.
- `GET /restaurants/{restaurantId}/menu`
  - Lists menu items.

### Orders

- `POST /orders` (`CUSTOMER`, `ADMIN`)
  - Places an order and triggers payment processing.
- `GET /orders/{orderId}` (`CUSTOMER`, `DELIVERY_PARTNER`, `ADMIN`)
  - Fetches one order.
- `GET /orders/customers/{customerId}` (`CUSTOMER`, `ADMIN`)
  - Fetches customer orders.
- `PATCH /orders/{orderId}/status?status={ORDER_STATUS}` (`DELIVERY_PARTNER`, `ADMIN`)
  - Updates order status.
- `PATCH /orders/{orderId}/assign/{deliveryPartnerId}` (`ADMIN`)
  - Assigns delivery partner.

### Payments

- `POST /payments/process` (`CUSTOMER`, `ADMIN`)
  - Processes mock payment.
- `GET /payments/orders/{orderId}` (`CUSTOMER`, `ADMIN`)
  - Fetches payment by order.

### Tracking Integration (Monolith)

- `POST /tracking/location` (`DELIVERY_PARTNER`, `ADMIN`)
  - Forwards location update to tracking microservice.

Request body:

```json
{
  "orderId": 101,
  "deliveryPartnerId": 901,
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

## Tracking Microservice APIs (`/api/v1`)

### REST

- `POST /tracking/location`
  - Accepts and processes location update.
  - Returns `202 Accepted`.

### WebSocket/STOMP

- Endpoint: `/ws-tracking`
- Topic per order: `/topic/orders/{orderId}`

Event payload:

```json
{
  "orderId": 101,
  "deliveryPartnerId": 901,
  "latitude": 12.9716,
  "longitude": 77.5946,
  "h3Index": "617700440650850303"
}
```

## Response envelope and errors

- Success responses are wrapped using `ApiResponse<T>` in monolith APIs.
- Validation, business, and unknown errors are handled by global exception handler and returned as `ErrorResponse` with HTTP status, path, and timestamp.

