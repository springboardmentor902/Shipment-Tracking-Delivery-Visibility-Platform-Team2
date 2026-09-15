# shiptrackPro_Infosys

ShipTrack Pro is a full-stack shipment tracking application built with Spring Boot, PostgreSQL, Next.js and TypeScript.

## Features

- Registration, login and role-based access
- Shipment creation with multiple packages
- Customer, operator and administrator shipment views
- Route management and driver assignment
- Tracking history and ETA prediction
- In-app and email notifications
- Proof of delivery submission and verification
- Role-based analytics and PDF/Excel reports
- Live driver location updates using WebSocket and STOMP
- Light and dark interface themes

## Project structure

```text
frontend/   Next.js frontend
src/        Spring Boot backend
pom.xml     Backend dependencies
```

## Requirements

- Java 21+
- Node.js 20+
- PostgreSQL
- Redis

Create a PostgreSQL database named `shiptrack_pro_infosys`, then provide the required values through environment variables. This keeps the project data separate from older ShipTrack databases. Never commit real passwords or API keys.

### Linux/macOS

```bash
export DB_PASSWORD="your-postgres-password"
export JWT_SECRET="use-a-random-secret-with-at-least-32-characters"
export ADMIN_EMAIL="admin@example.com"
export ADMIN_PASSWORD="your-admin-password"
export GEOAPIFY_GEOCODING_API_KEY="your-geoapify-key"
export GEOAPIFY_ROUTING_API_KEY="your-geoapify-key"
```

### Windows PowerShell

```powershell
$env:DB_PASSWORD = "your-postgres-password"
$env:JWT_SECRET = "use-a-random-secret-with-at-least-32-characters"
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "your-admin-password"
$env:GEOAPIFY_GEOCODING_API_KEY = "your-geoapify-key"
$env:GEOAPIFY_ROUTING_API_KEY = "your-geoapify-key"
```

Google Maps can be used instead by setting `GOOGLE_MAPS_API_KEY`. Map and mail credentials are optional: a route is still saved when a map provider fails, and in-app notifications still work when email delivery fails.

## Run the application

Start the backend from the project root:

```bash
redis-server --daemonize yes
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`. The backend runs at `http://localhost:8081`.

Open another terminal and start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`. Use the admin email and password configured above, or register a customer account from the login screen.

## Test before submitting

```bash
./mvnw test
cd frontend
npm run lint
npm run build
```

Backend tests use an in-memory H2 database and do not change PostgreSQL data.
