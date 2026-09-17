"use client";

import { useEffect, useMemo, useState } from "react";
import axios from "axios";
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from "chart.js";
import { Bar, Pie } from "react-chartjs-2";

ChartJS.register(ArcElement, BarElement, CategoryScale, Legend, LinearScale, Tooltip);

type DashboardRole = "customer" | "business" | "admin";
type ShipmentAnalyticsItem = {
  id: number;
  trackingNumber: string;
  status: string;
  receiverName?: string;
  pickupAddress?: string;
  deliveryAddress?: string;
};
type AnalyticsData = {
  totalShipments?: number;
  totalShipmentHistoryCount?: number;
  totalShipmentVolume?: number;
  activeShipments?: number;
  deliveredShipments?: number;
  cancelledShipments?: number;
  failedDeliveries?: number;
  onTimeDeliveries?: number;
  onTimeDeliveryRate?: number;
  deliverySuccessRate?: number;
  totalTrackingEvents?: number;
  atRiskShipments?: number;
  delayedShipmentCount?: number;
  activeUsers?: number;
  totalUsers?: number;
  systemStatus?: string;
  generatedAt?: string;
  availableReports?: string[];
  userRoleBreakdown?: Record<string, number>;
  shipmentHistory?: ShipmentAnalyticsItem[];
  atRiskShipmentList?: ShipmentAnalyticsItem[];
  pendingVerifications?: number;
  pendingPodVerifications?: number;
  statusBreakdown?: Record<string, number>;
  monthlyShipmentVolume?: Record<string, number>;
  recentNotifications?: Array<{
    id: number;
    title: string;
    message: string;
    createdAt: string;
  }>;
  routeAnalytics?: {
    totalRoutes: number;
    averageRouteDistanceKm: number;
    timeEstimateAccuracyPercent: number;
    routesWithActualTime: number;
    bestPerformingRoute?: RoutePerformance;
    worstPerformingRoute?: RoutePerformance;
  };
};

type RoutePerformance = {
  id: number;
  trackingNumber?: string;
  origin: string;
  destination: string;
  distanceKm?: number;
  estimatedTimeMinutes?: number;
  actualTimeMinutes?: number;
  isCurrent: boolean;
};

type Props = {
  role: DashboardRole;
  token: string;
  apiUrl?: string;
};

const chartStatusColors = ["#2563eb", "#16a34a", "#d97706", "#dc2626", "#7c3aed", "#64748b"];

export default function AnalyticsDashboard({
  role,
  token,
  apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081",
}: Props) {
  const [data, setData] = useState<AnalyticsData>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [darkTheme, setDarkTheme] = useState(false);

  useEffect(() => {
    const syncTheme = () => setDarkTheme(document.documentElement.dataset.theme === "dark");
    syncTheme();
    window.addEventListener("shiptrack-theme-change", syncTheme);
    return () => window.removeEventListener("shiptrack-theme-change", syncTheme);
  }, []);

  useEffect(() => {
    let cancelled = false;
    async function loadDashboard() {
      setLoading(true);
      setError("");
      try {
        const response = await axios.get<AnalyticsData>(
          `${apiUrl}/api/analytics/${role}`,
          { headers: token ? { Authorization: `Bearer ${token}` } : undefined },
        );
        if (!cancelled) setData(response.data);
      } catch (requestError) {
        if (cancelled) return;
        if (axios.isAxiosError(requestError)) {
          const responseData = requestError.response?.data as { message?: string; detail?: string } | undefined;
          setError(responseData?.detail ?? responseData?.message ?? "Could not load analytics.");
        } else {
          setError("Could not load analytics.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    loadDashboard();
    return () => { cancelled = true; };
  }, [apiUrl, role, token]);

  const totalShipments = data?.totalShipments
    ?? data?.totalShipmentHistoryCount
    ?? data?.totalShipmentVolume
    ?? 0;
  const pendingVerifications = data?.pendingVerifications
    ?? data?.pendingPodVerifications
    ?? 0;
  const statusData = useMemo(() => {
    const statuses = Object.keys(data?.statusBreakdown ?? {});
    return ({
    labels: statuses.map((status) => status.replaceAll("_", " ")),
    datasets: [{
      data: statuses.map((status) => data?.statusBreakdown?.[status] ?? 0),
      backgroundColor: statuses.map((_, index) => chartStatusColors[index % chartStatusColors.length]),
    }],
  });
  }, [data]);
  const monthlyData = useMemo(() => {
    const monthly = data?.monthlyShipmentVolume ?? {};
    const labels = Object.keys(monthly);
    return {
      labels,
      datasets: [{
        label: "Shipments",
        data: labels.map((label) => monthly[label]),
        backgroundColor: "#2563eb",
        borderRadius: 6,
      }],
    };
  }, [data]);

  if (loading) {
    return <div className="rounded-xl border border-slate-200 bg-white p-8 text-slate-600">Loading analytics...</div>;
  }
  if (error) {
    return <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">{error}</div>;
  }
  return (
    <section className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-3">
        <MetricCard label="Total shipments" value={totalShipments} />
        <MetricCard label="Active shipments" value={data?.activeShipments ?? 0} />
        <MetricCard label="Pending verifications" value={pendingVerifications} />
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        {role === "customer" && <MetricCard label="Tracking updates" value={data?.totalTrackingEvents ?? 0} />}
        {role === "customer" && <MetricCard label="Cancelled shipments" value={data?.cancelledShipments ?? 0} />}
        {role === "business" && <MetricCard label="Delivery success" value={data?.deliverySuccessRate ?? 0} suffix="%" />}
        {role === "business" && <MetricCard label="At risk" value={data?.atRiskShipments ?? 0} />}
        {role === "business" && <MetricCard label="Failed deliveries" value={data?.failedDeliveries ?? 0} />}
        {role === "admin" && <MetricCard label="Active users" value={data?.activeUsers ?? 0} />}
        {role === "admin" && <MetricCard label="On-time delivery" value={data?.onTimeDeliveryRate ?? 0} suffix="%" />}
        {role === "admin" && <MetricCard label="Tracking events" value={data?.totalTrackingEvents ?? 0} />}
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Shipment statuses</h2>
          <div className="mx-auto max-w-sm"><Pie data={statusData} options={{ plugins: { legend: { labels: { color: darkTheme ? "#cbd5e1" : "#475569" } } } }} /></div>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Monthly shipment volume</h2>
          <Bar data={monthlyData} options={{ responsive: true, plugins: { legend: { display: false } }, scales: { x: { ticks: { color: darkTheme ? "#94a3b8" : "#64748b" }, grid: { color: darkTheme ? "#334155" : "#e2e8f0" } }, y: { ticks: { color: darkTheme ? "#94a3b8" : "#64748b" }, grid: { color: darkTheme ? "#334155" : "#e2e8f0" } } } }} />
        </div>
      </div>
      {role === "admin" && data?.routeAnalytics && <section className="rounded-xl border border-slate-200 bg-white p-5">
        <div className="mb-4"><p className="text-xs font-bold tracking-widest text-blue-600">ROUTE MANAGEMENT</p><h2 className="text-lg font-semibold text-slate-900">Route analytics</h2></div>
        <div className="grid gap-4 sm:grid-cols-3">
          <MetricCard label="Saved routes" value={data.routeAnalytics.totalRoutes} />
          <MetricCard label="Average route distance" value={data.routeAnalytics.averageRouteDistanceKm} suffix=" km" />
          <MetricCard label="ETA accuracy" value={data.routeAnalytics.timeEstimateAccuracyPercent} suffix="%" />
        </div>
        <p className="mt-4 text-sm text-slate-500">ETA accuracy uses {data.routeAnalytics.routesWithActualTime} route(s) with an actual travel time recorded.</p>
        <div className="mt-5 grid gap-4 lg:grid-cols-2">
          <RoutePerformanceCard title="Best performing route" route={data.routeAnalytics.bestPerformingRoute} emptyText="Create routes to see the best-performing route." />
          <RoutePerformanceCard title="Worst performing route" route={data.routeAnalytics.worstPerformingRoute} emptyText="Create routes to see the route needing the most attention." />
        </div>
      </section>}
      {role === "business" && <ShipmentList title="At-risk shipments" items={data?.atRiskShipmentList ?? []} emptyText="No business shipments are currently at risk." />}
      {(role === "customer" || role === "business") && <ShipmentList title="Shipment history" items={data?.shipmentHistory ?? []} emptyText="No shipment history yet." />}
      {role === "admin" && <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="text-lg font-semibold text-slate-900">System monitoring</h2>
        <p className="mt-2 text-sm text-slate-600">Status: <strong>{data?.systemStatus ?? "OPERATIONAL"}</strong> · Users: <strong>{data?.totalUsers ?? 0}</strong></p>
        <p className="mt-2 text-sm text-slate-600">Reports available: {(data?.availableReports ?? []).join(", ") || "Shipments, deliveries, routes, delays"}</p>
      </section>}
      {data?.recentNotifications && data.recentNotifications.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="mb-3 text-lg font-semibold text-slate-900">Recent notifications</h2>
          <div className="space-y-3">
            {data.recentNotifications.map((notification) => (
              <div key={notification.id} className="border-b border-slate-100 pb-3 last:border-0">
                <p className="font-medium text-slate-800">{notification.title}</p>
                <p className="text-sm text-slate-600">{notification.message}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function ShipmentList({ title, items, emptyText }: { title: string; items: ShipmentAnalyticsItem[]; emptyText: string }) {
  return <section className="rounded-xl border border-slate-200 bg-white p-5"><h2 className="mb-3 text-lg font-semibold text-slate-900">{title}</h2>{items.length ? <div className="space-y-2">{items.slice(0, 8).map((shipment) => <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 py-2" key={shipment.id}><div><strong className="text-sm text-slate-800">{shipment.trackingNumber}</strong><p className="text-xs text-slate-500">{shipment.pickupAddress} → {shipment.deliveryAddress}</p></div><span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700">{shipment.status.replaceAll("_", " ")}</span></div>)}</div> : <p className="text-sm text-slate-500">{emptyText}</p>}</section>;
}

function MetricCard({ label, value, suffix = "" }: { label: string; value: number; suffix?: string }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-bold text-slate-900">{value}{suffix}</p>
    </div>
  );
}

function RoutePerformanceCard({ title, route, emptyText }: { title: string; route?: RoutePerformance; emptyText: string }) {
  return <div className="rounded-lg border border-slate-100 bg-slate-50 p-4"><p className="font-semibold text-slate-800">{title}</p>{route ? <><p className="mt-2 text-sm font-medium text-slate-700">{route.trackingNumber ?? `Route #${route.id}`}</p><p className="text-sm text-slate-600">{route.origin} → {route.destination}</p><p className="mt-2 text-xs text-slate-500">{route.distanceKm ?? "—"} km · Estimated: {route.estimatedTimeMinutes ?? "—"} min · Actual: {route.actualTimeMinutes ?? "Not recorded"}</p></> : <p className="mt-2 text-sm text-slate-500">{emptyText}</p>}</div>;
}
