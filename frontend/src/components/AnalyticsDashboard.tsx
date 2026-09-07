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

type DashboardRole = "customer" | "business-client" | "admin";
type AnalyticsData = {
  totalShipments?: number;
  totalShipmentHistoryCount?: number;
  totalShipmentVolume?: number;
  activeShipments?: number;
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

const chartStatusLabels = ["IN_TRANSIT", "DELIVERED", "DELAYED", "FAILED"];
const chartStatusColors = ["#2563eb", "#16a34a", "#dc2626", "#6b7280"];

export default function AnalyticsDashboard({
  role,
  token,
  apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081",
}: Props) {
  const [data, setData] = useState<AnalyticsData>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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
          setError(requestError.response?.data?.message ?? "Could not load analytics.");
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
  const statusData = useMemo(() => ({
    labels: chartStatusLabels.map((status) => status.replace("_", " ")),
    datasets: [{
      data: chartStatusLabels.map((status) => status === "FAILED"
        ? (data?.statusBreakdown?.FAILED ?? 0) + (data?.statusBreakdown?.FAILED_DELIVERY ?? 0)
        : data?.statusBreakdown?.[status] ?? 0),
      backgroundColor: chartStatusColors,
    }],
  }), [data]);
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
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Shipment statuses</h2>
          <div className="mx-auto max-w-sm"><Pie data={statusData} /></div>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Monthly shipment volume</h2>
          <Bar data={monthlyData} options={{ responsive: true, plugins: { legend: { display: false } } }} />
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
