# ShipTrack Pro

ShipTrack Pro is a beginner-friendly Spring Boot project for creating shipments, tracking delivery progress, planning routes, predicting ETA, sending notifications, and uploading proof of delivery.

## What is included

- Multiple packages for one shipment
- Role-based shipment list: customers see their own shipments, operators see assigned shipments, and administrators see all shipments
- Route creation, driver assignment, and optional Google Maps distance/time lookup
- Tracking events and simple ETA/delay-risk prediction
- Email notification records with duplicate prevention
- Proof of delivery upload, delivery status update, and support/admin verification

## Before running the backend

Install Java 21 or newer and PostgreSQL. Create a database named `shiptrack`.

Set the environment variables in PowerShell. Keep real passwords and API keys out of the source code.

```powershell
$env:DB_PASSWORD = "your-postgres-password"
$env:JWT_SECRET = "a-long-random-secret-at-least-32-characters"
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "a-strong-admin-password"
```

Google Maps is optional while developing. Without the key, route creation still works; only `distanceKm` and `estimatedTimeMinutes` remain empty.

```powershell
$env:GOOGLE_MAPS_API_KEY = "your-demo-google-maps-key"
```

For email notifications, add these only when an SMTP account is available:

```powershell
$env:MAIL_HOST = "smtp.example.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "your-email"
$env:MAIL_PASSWORD = "your-app-password"
$env:MAIL_FROM = "your-email"
$env:MAIL_SMTP_AUTH = "true"
$env:MAIL_SMTP_STARTTLS = "true"
```

## Run the backend

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs on `http://localhost:8081`.

## Main API endpoints

| Feature | Endpoint |
| --- | --- |
| Create/list shipments | `POST`, `GET /api/shipments` |
| Assign an operator | `PATCH /api/shipments/{id}/operator` |
| Create/get a route | `POST /api/routes`, `GET /api/routes/{shipmentId}` |
| Change route driver | `PATCH /api/routes/{shipmentId}/driver` |
| Add/get tracking events | `POST`, `GET /api/tracking/{shipmentId}` |
| Predict/get ETA | `POST /api/eta/{shipmentId}/predict`, `GET /api/eta/{shipmentId}` |
| List/read notifications | `GET /api/notifications`, `PATCH /api/notifications/{id}/read` |
| Submit/get POD | `POST`, `GET /api/pod/{shipmentId}` |
| Verify POD | `PATCH /api/pod/{shipmentId}/verify` |

All protected requests need this header after login:

```text
Authorization: Bearer <token>
```

## Run the tests

```powershell
.\mvnw.cmd test
```

The test configuration uses an in-memory H2 database, so it does not need PostgreSQL.
