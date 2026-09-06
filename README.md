# ShipTrack Pro

ShipTrack Pro is a Spring Boot and Next.js shipment tracking platform for creating shipments, tracking delivery progress, planning routes, predicting ETA, sending notifications, uploading proof of delivery, analyzing operations, and exporting reports.

## What is included

- Multiple packages for one shipment
- Role-based shipment list: customers see their own shipments, operators see assigned shipments, and administrators see all shipments
- Route creation, driver assignment, and optional Google Maps distance/time lookup
- Tracking events and simple ETA/delay-risk prediction
- Email notification records with duplicate prevention
- Proof of delivery upload, delivery status update, and support/admin verification
- Role-based analytics dashboards for customers, business clients, and administrators
- Filterable PDF and Excel shipment reports
- Responsive logistics dashboard UI with shipment timelines, KPI cards, filters, notifications, and loading states

## Before running the backend

Install Java 21 or newer and PostgreSQL. Create a database named `shiptrack`.

Set the environment variables in PowerShell. Keep real passwords and API keys out of the source code.

```powershell
$env:DB_PASSWORD = "your-postgres-password"
$env:JWT_SECRET = "a-long-random-secret-at-least-32-characters"
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "a-strong-admin-password"
```

`DB_PASSWORD` must match the password for the local PostgreSQL `postgres` user. Do not commit the actual password to this repository.

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

## Run the frontend

Install Node.js 20 or newer, then run:

```powershell
Set-Location frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:3000`. Set `NEXT_PUBLIC_API_URL` when the backend is not running on the default URL:

```powershell
$env:NEXT_PUBLIC_API_URL = "http://localhost:8081"
```

If port 3000 is already in use, Next.js may start on port 3001. Open the port shown in the terminal output.

Start the backend before using Login or Register. Authentication requests require the backend at `http://localhost:8081` and a valid PostgreSQL connection.

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
| View pending proof queue | `GET /api/pod/pending` (Support Agent/Admin) |
| Customer analytics | `GET /api/analytics/customer` or `/api/analytics/customer/{customerId}` |
| Business client analytics | `GET /api/analytics/business-client` or `/api/analytics/business-client/{clientId}` |
| Admin analytics | `GET /api/analytics/admin` |
| Export PDF report | `GET /api/reports/export/pdf` |
| Export Excel report | `GET /api/reports/export/excel` |

Analytics access is restricted to the matching customer/business-client role or an administrator. Report exports are available to business clients and administrators. Reports accept optional query parameters:

```text
startDate=2026-01-01&endDate=2026-12-31&status=DELIVERED
```

Report responses are downloadable attachments named `shipments_report.pdf` and `shipments_report.xlsx`.

All protected requests need this header after login:

```text
Authorization: Bearer <token>
```

## Run the tests

```powershell
.\mvnw.cmd test
```

The test configuration uses an in-memory H2 database, so it does not need PostgreSQL.

## Frontend validation

```powershell
Set-Location frontend
npm run lint
npm run build
```
