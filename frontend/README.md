# ShipTrack Pro Frontend

The frontend is a Next.js dashboard for shipment creation, tracking, ETA visibility, proof-of-delivery workflows, analytics, and report exports.

## Development

```powershell
npm install
$env:NEXT_PUBLIC_API_URL = "http://localhost:8081"
npm run dev
```

Open `http://localhost:3000`.

## Available commands

```powershell
npm run lint
npm run build
npm run start
```

## Main UI components

- `app/page.tsx` — shipment operations workspace
- `src/components/Layout.tsx` — responsive application shell and navigation
- `src/components/ShipmentTimeline.tsx` — delivery milestone timeline
- `src/components/AnalyticsDashboard.tsx` — Chart.js analytics dashboard
- `src/components/ReportExporter.tsx` — PDF and Excel report downloads
