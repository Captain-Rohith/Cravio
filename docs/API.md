# API Documentation

Base URLs:

- Core monolith: `http://localhost:8090`
- Tracking service: `http://localhost:8081`

## Authentication

- JWT bearer token is required for protected routes.
- Public routes:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/restaurants/**`

## End-to-end flow: restaurant onboarding to order management

1. Register restaurant user
   - `POST /api/v1/auth/register`
   - Body:

```json
{
  "email": "rest5@rest5.com",
  "password": "rest5@rest5.coM",
  "fullName": "restaurant five",
  "role": "RESTAURANT"
}
```

2. Login and get JWT
   - `POST /api/v1/auth/login`
   - Use returned token as `Authorization: Bearer <token>`.

3. Create restaurant profile
   - `POST /api/v1/restaurants` (`ADMIN`, `RESTAURANT`)

4. Add menu items
   - `POST /api/v1/restaurants/{restaurantId}/menu` (`ADMIN`, `RESTAURANT`)

5. Maintain menu/catalog
   - `GET /api/v1/restaurants/{restaurantId}/menu`
   - `PUT /api/v1/restaurants/{restaurantId}/menu/{menuItemId}` (`ADMIN`, `RESTAURANT`)
   - `DELETE /api/v1/restaurants/{restaurantId}/menu/{menuItemId}` (`ADMIN`, `RESTAURANT`)

6. Customer places order
   - `POST /api/v1/orders` (`CUSTOMER`, `ADMIN`)

## End-to-end flow: customer ordering to delivery tracking

1. Login and get JWT
   - `POST /api/v1/auth/login`
   - Use returned token as `Authorization: Bearer <token>`.

2. Discover restaurants and menu
   - `GET /api/v1/restaurants/nearby?latitude={lat}&longitude={lng}`
   - `GET /api/v1/restaurants/{restaurantId}/menu`

3. Place order
   - `POST /api/v1/orders` (`CUSTOMER`, `ADMIN`)
   - Uses `customerId`, `restaurantId`, and `items`.

4. View order details / status
   - `GET /api/v1/orders/{orderId}` (`CUSTOMER`, `DELIVERY_PARTNER`, `ADMIN`)

5. View previous orders (order history)
   - `GET /api/v1/orders/customers/{customerId}` (`CUSTOMER`, `ADMIN`)

6. Cancel order (customer)
   - `PATCH /api/v1/orders/customers/{customerId}/{orderId}/cancel` (`CUSTOMER`, `ADMIN`)

7. Track delivery location (live)
   - Delivery side updates: `POST /api/v1/tracking/location` (`DELIVERY_PARTNER`, `ADMIN`)
   - Customer subscribes to WebSocket topic: `/topic/orders/{orderId}` on `/ws-tracking`

7. Restaurant manages incoming orders
   - `GET /api/v1/orders/restaurants/{restaurantId}` (`RESTAURANT`, `ADMIN`)
   - `PATCH /api/v1/orders/restaurants/{restaurantId}/{orderId}/status?status={ORDER_STATUS}` (`RESTAURANT`, `ADMIN`)

8. Delivery partner order discovery and claiming
   - `GET /api/v1/orders/available/nearby?latitude={lat}&longitude={lng}` (`DELIVERY_PARTNER`)
   - `PATCH /api/v1/orders/{orderId}/claim?latitude={lat}&longitude={lng}` (`DELIVERY_PARTNER`)
   - `PATCH /api/v1/orders/{orderId}/status?status={ORDER_STATUS}` (`DELIVERY_PARTNER`, `ADMIN`)
   - Only orders within configured discovery radius are visible/claimable (`cravio.delivery.discovery-radius-km`).

## Core Monolith APIs (`/api/v1`)

### Auth

- `POST /auth/register`
  - Registers a new user with role.
- `POST /auth/login`
  - Returns JWT token and user details.

### Restaurants

- `POST /restaurants` (`ADMIN`, `RESTAURANT`)
  - Creates a restaurant and stores its H3 index.
- `GET /restaurants/{restaurantId}`
  - Fetches a single restaurant.
- `PUT /restaurants/{restaurantId}` (`ADMIN`, `RESTAURANT`)
  - Updates restaurant name and location.
- `DELETE /restaurants/{restaurantId}` (`ADMIN`, `RESTAURANT`)
  - Deletes restaurant and its menu items.
- `GET /restaurants/nearby?latitude={lat}&longitude={lng}`
  - Returns nearby restaurants using H3 neighboring cells.
- `POST /restaurants/{restaurantId}/menu` (`ADMIN`, `RESTAURANT`)
  - Adds menu item.
- `GET /restaurants/{restaurantId}/menu/{menuItemId}`
  - Fetches a single menu item for the restaurant.
- `PUT /restaurants/{restaurantId}/menu/{menuItemId}` (`ADMIN`, `RESTAURANT`)
  - Updates menu item.
- `DELETE /restaurants/{restaurantId}/menu/{menuItemId}` (`ADMIN`, `RESTAURANT`)
  - Deletes menu item.
- `GET /restaurants/{restaurantId}/menu`
  - Lists menu items.

Restaurant payload:

```json
{
  "name": "Spice Hub",
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

Menu item payload:

```json
{
  "name": "Paneer Butter Masala",
  "price": 249.0
}
```

### Orders

- `POST /orders` (`CUSTOMER`, `ADMIN`)
  - Places an order and triggers payment processing.
- `GET /orders/{orderId}` (`CUSTOMER`, `DELIVERY_PARTNER`, `ADMIN`)
  - Fetches one order.
- `GET /orders/customers/{customerId}` (`CUSTOMER`, `ADMIN`)
  - Fetches customer orders.
- `PATCH /orders/customers/{customerId}/{orderId}/cancel` (`CUSTOMER`, `ADMIN`)
  - Cancels a customer order.
- `GET /orders/restaurants/{restaurantId}` (`RESTAURANT`, `ADMIN`)
  - Fetches orders for a restaurant.
- `PATCH /orders/restaurants/{restaurantId}/{orderId}/status?status={ORDER_STATUS}` (`RESTAURANT`, `ADMIN`)
  - Restaurant updates order status for its own order.
- `GET /orders/available/nearby?latitude={lat}&longitude={lng}` (`DELIVERY_PARTNER`)
  - Returns unassigned orders in nearby H3 cells and within configured radius, including pickup restaurant details.
- `PATCH /orders/{orderId}/claim?latitude={lat}&longitude={lng}` (`DELIVERY_PARTNER`)
  - Lets the logged-in delivery partner self-claim a nearby order if still unclaimed and within configured radius.
- `PATCH /orders/{orderId}/status?status={ORDER_STATUS}` (`DELIVERY_PARTNER`, `ADMIN`)
  - Updates order status.

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
