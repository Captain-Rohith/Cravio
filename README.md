# Cravio

Cravio is a food delivery backend built with Java and Spring Boot using a **Modular Monolith** for core business modules and a dedicated **Tracking Microservice** for realtime location updates.

## What is implemented

- Modular monolith with `user`, `restaurant`, `order`, `payment`, and `tracking` integration modules.
- Tracking microservice with REST ingestion, H3 index conversion, in-memory latest-location snapshot, and WebSocket push.
- JWT authentication + role-based authorization.
- DTO-driven API responses (`ApiResponse` / `ErrorResponse`), validation, and global exception handling.
- Cache has been intentionally disabled for now.

## Repository layout

- `src/main/java/com/javacravio/cravio` - Core monolith modules
- `tracking-service/` - Dedicated tracking microservice
- `docs/API.md` - API reference (monolith + tracking)
- `docs/ARCHITECTURE.md` - Architecture and flow explanations
- `docs/DESIGN_DECISIONS.md` - Design trade-offs and rationale

## Local setup

### 1) Prerequisites

- Java 21+
- Maven wrapper (`mvnw.cmd` already included)
- Oracle DB (or adapt datasource to another DB for local dev)

### 2) Configure environment variables (optional overrides)

- `CRAVIO_DB_URL`
- `CRAVIO_DB_USERNAME`
- `CRAVIO_DB_PASSWORD`
- `CRAVIO_JWT_SECRET`
- `CRAVIO_TRACKING_BASE_URL`
- `CRAVIO_H3_RESOLUTION`

### 3) Build

```powershell
Set-Location "C:\Users\rohit\IdeaProjects\Cravio"
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd -q -f tracking-service/pom.xml -DskipTests compile
```

### 4) Run services

```powershell
# terminal 1 - core monolith (port 8090)
Set-Location "C:\Users\rohit\IdeaProjects\Cravio"
.\mvnw.cmd spring-boot:run

# terminal 2 - tracking microservice (port 8081)
Set-Location "C:\Users\rohit\IdeaProjects\Cravio"
.\mvnw.cmd -f tracking-service/pom.xml spring-boot:run
```

## Testing

A targeted tracking unit test is included:

```powershell
Set-Location "C:\Users\rohit\IdeaProjects\Cravio"
.\mvnw.cmd -q -f tracking-service/pom.xml -Dtest=TrackingServiceTest test
```

## Notes

- The root `src/main/java/TrackingService.java` was misplaced and has been removed.
- The canonical tracking business service now lives at `tracking-service/src/main/java/com/javacravio/tracking/service/TrackingService.java`.

