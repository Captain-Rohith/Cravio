# Tracking Service

Realtime delivery tracking microservice for Cravio.

## Responsibilities

- Accept partner location updates via REST.
- Convert location to H3 index.
- Store latest location snapshot in memory.
- Broadcast tracking events to users over WebSocket/STOMP.

## Endpoints

- `POST /api/v1/tracking/location`
  - Input: `orderId`, `deliveryPartnerId`, `latitude`, `longitude`
  - Output: `202 Accepted`

- WebSocket endpoint: `/ws-tracking`
- STOMP topic per order: `/topic/orders/{orderId}`

## Configuration

- `server.port` (default `8081`)
- `cravio.h3.resolution` (default `9`)

## Run locally

```powershell
Set-Location "C:\Users\rohit\IdeaProjects\Cravio"
.\mvnw.cmd -f tracking-service/pom.xml spring-boot:run
```

## Test

```powershell
Set-Location "C:\Users\rohit\IdeaProjects\Cravio"
.\mvnw.cmd -q -f tracking-service/pom.xml -Dtest=TrackingServiceTest test
```

