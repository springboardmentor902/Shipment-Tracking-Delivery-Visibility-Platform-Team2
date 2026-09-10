"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import Layout from "../src/components/Layout";
import AnalyticsDashboard from "../src/components/AnalyticsDashboard";
import AuthModal from "../src/components/AuthModal";
import LoadingSkeleton from "../src/components/LoadingSkeleton";
import PodVerificationQueue from "../src/components/PodVerificationQueue";
import PublicTracker from "../src/components/PublicTracker";
import ReportExporter from "../src/components/ReportExporter";
import RouteHistory from "../src/components/RouteHistory";
import SearchFilterBar from "../src/components/SearchFilterBar";
import ShipmentManagement from "../src/components/ShipmentManagement";
import ShipmentTimeline from "../src/components/ShipmentTimeline";
import SummaryCard from "../src/components/SummaryCard";
import type { DashboardTab } from "../src/types/dashboard";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081";

type PackageItem = { description: string; quantity: number; fragile: boolean };
type Shipment = { id: number; trackingNumber: string; status: string; priority?: string; pickupAddress: string; deliveryAddress: string; packages: PackageItem[]; createdAt?: string; estimatedDeliveryDate?: string };
type Eta = { predictedDeliveryTime: string; delayRiskScore: number; confidenceScore: number; factors: string; estimatedRemainingMinutes?: number; manuallyAdjusted?: boolean; overrideReason?: string };
type TrackingEvent = { id: number; status: string; location?: string; eventTimestamp: string };
type Notification = { id: number; shipmentId?: number; title: string; message: string; readAt?: string };
type OverviewStats = {
  totalShipments?: number;
  totalShipmentHistoryCount?: number;
  totalShipmentVolume?: number;
  activeShipments?: number;
  attentionRequired?: number;
  delayedShipments?: number;
  delayedShipmentCount?: number;
  statusBreakdown?: Record<string, number>;
};
type DrilldownCategory = "active" | "delivered" | "attention" | "alerts";
type ShipmentWithRisk = Shipment & { delayRiskScore?: number };
type LoginResult = { token: string; user: { fullName: string; role: string } };
type Route = { id: number; shipmentId: number; origin: string; destination: string; distanceKm?: number; estimatedTimeMinutes?: number; trafficCondition?: string; isCurrent: boolean; createdAt?: string; routeSummary?: string; selectionReason?: string };
type Pod = {
  shipmentId: number;
  deliveredToName: string;
  deliveryNotes?: string;
  signatureUrl?: string;
  photoUrl?: string;
  verificationStatus: string;
  deliveredAt?: string;
};

type LeafletMap = { remove: () => void; setView: (point: [number, number], zoom: number) => LeafletMap; fitBounds: (bounds: unknown, options?: { padding: [number, number] }) => void };
type LeafletLibrary = {
  map: (element: HTMLDivElement) => LeafletMap;
  tileLayer: (url: string, options: { attribution: string }) => { addTo: (map: LeafletMap) => void };
  marker: (point: [number, number]) => { addTo: (map: LeafletMap) => { bindPopup: (text: string) => void } };
  polyline: (points: [number, number][], options: { color: string; weight: number }) => { addTo: (map: LeafletMap) => { getBounds: () => unknown } };
};

declare global {
  interface Window { L?: LeafletLibrary }
}

function DeliveryMap({ shipment, onRouteReady }: { shipment?: Shipment; onRouteReady: (estimate?: { minutes: number; expectedArrival: string }) => void }) {
  const mapElement = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<LeafletMap | undefined>(undefined);
  const [mapMessage, setMapMessage] = useState("Load a shipment to see its OpenStreetMap route.");

  useEffect(() => {
    if (!shipment || !mapElement.current) return;
    let cancelled = false;
    onRouteReady(undefined);

    async function loadLeaflet() {
      if (window.L) return;
      if (!document.querySelector('link[data-leaflet]')) {
        const style = document.createElement("link");
        style.rel = "stylesheet";
        style.href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
        style.dataset.leaflet = "true";
        document.head.appendChild(style);
      }
      await new Promise<void>((resolve, reject) => {
        const script = document.createElement("script");
        script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
        script.onload = () => resolve();
        script.onerror = () => reject(new Error("Could not load the map library."));
        document.body.appendChild(script);
      });
    }

    async function findAddress(address: string) {
      const response = await fetch(`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=${encodeURIComponent(address)}`);
      const places = await response.json();
      if (!places.length) throw new Error("Address not found on OpenStreetMap.");
      return [Number(places[0].lat), Number(places[0].lon)] as [number, number];
    }

    async function showMap() {
      try {
        const element = mapElement.current;
        if (!element) return;
        setMapMessage("Finding pickup and delivery locations...");
        await loadLeaflet();
        const [origin, destination] = await Promise.all([findAddress(shipment!.pickupAddress), findAddress(shipment!.deliveryAddress)]);
        if (cancelled) return;
        mapInstance.current?.remove();
        if (!window.L) throw new Error("Map library is not ready.");
        const map = window.L.map(element).setView(origin, 7);
        mapInstance.current = map;
        window.L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { attribution: "© OpenStreetMap contributors" }).addTo(map);
        window.L.marker(origin).addTo(map).bindPopup("Pickup");
        window.L.marker(destination).addTo(map).bindPopup("Delivery");
        const routeResponse = await fetch(`https://router.project-osrm.org/route/v1/driving/${origin[1]},${origin[0]};${destination[1]},${destination[0]}?overview=full&geometries=geojson`);
        const routeData = await routeResponse.json();
        const coordinates = routeData.routes?.[0]?.geometry?.coordinates;
        if (coordinates) {
          const line = coordinates.map(([longitude, latitude]: [number, number]) => [latitude, longitude]);
          const routeLine = window.L.polyline(line, { color: "#2563eb", weight: 5 }).addTo(map);
          map.fitBounds(routeLine.getBounds(), { padding: [25, 25] });
          const minutes = Math.round(routeData.routes[0].duration / 60);
          onRouteReady({ minutes, expectedArrival: new Date(Date.now() + minutes * 60000).toISOString() });
        } else {
          map.fitBounds([origin, destination], { padding: [25, 25] });
        }
        setMapMessage("OpenStreetMap route loaded.");
      } catch (error) {
        setMapMessage(error instanceof Error ? error.message : "Could not load the map.");
      }
    }

    showMap();
    return () => { cancelled = true; mapInstance.current?.remove(); mapInstance.current = undefined; };
  }, [shipment, onRouteReady]);

  return <section className="card map-card"><h2>Live route map</h2><div className="map" ref={mapElement} /><small>{mapMessage}</small></section>;
}

class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

async function request<T>(path: string, token: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    const rawMessage = await response.text();
    let message = "";
    try {
      const body = JSON.parse(rawMessage);
      message = typeof body === "string" ? body : body.detail ?? body.message ?? body.error ?? "";
    } catch {
      message = rawMessage;
    }
    if (response.status === 404 && !message) message = "The requested item was not found.";
    if (response.status === 401 && !message) message = "Your session has expired. Please log in again.";
    if (response.status === 403 && !message) message = "You do not have permission to perform this action.";
    throw new ApiError(message || "Something went wrong. Please try again.", response.status);
  }
  return response.status === 204 ? (undefined as T) : (await response.json() as T);
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message.trim() ? error.message : fallback;
}

function loginErrorMessage(error: unknown) {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
    return "Email or password is incorrect. Please try again.";
  }
  return errorMessage(error, "We could not sign you in. Please try again.");
}

function registrationErrorMessage(error: unknown) {
  if (error instanceof ApiError && (error.status === 409 || error.status === 403)) {
    return "An account with this email address already exists. Please log in instead.";
  }
  return errorMessage(error, "We could not create your account. Please check the details and try again.");
}

function riskStyle(score?: number) {
  if (score === undefined) return "";
  return score >= 7 ? "high" : score >= 4 ? "medium" : "low";
}

function expectedArrival(shipment?: Shipment) {
  if (!shipment) return undefined;
  if (shipment.estimatedDeliveryDate) return new Date(shipment.estimatedDeliveryDate);
  if (shipment.createdAt) return new Date(new Date(shipment.createdAt).getTime() + 4 * 24 * 60 * 60 * 1000);
  return undefined;
}

function remainingTimeLabel(minutes?: number) {
  if (minutes === undefined) return "Calculating";
  if (minutes < 60) return `about ${Math.max(1, minutes)} minute${minutes === 1 ? "" : "s"}`;
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  if (days > 0) return `about ${days} day${days === 1 ? "" : "s"}${hours ? ` ${hours} hour${hours === 1 ? "" : "s"}` : ""}`;
  return `about ${hours} hour${hours === 1 ? "" : "s"}`;
}

function greetingForCurrentTime() {
  const hour = new Date().getHours();
  if (hour < 5) return "Good night";
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  if (hour < 22) return "Good evening";
  return "Good night";
}

function TimeGreeting({ name }: { name: string }) {
  const [greeting, setGreeting] = useState("Welcome");

  useEffect(() => {
    const updateGreeting = () => setGreeting(greetingForCurrentTime());
    const initialTimer = window.setTimeout(updateGreeting, 0);
    const refreshTimer = window.setInterval(updateGreeting, 60_000);
    return () => {
      window.clearTimeout(initialTimer);
      window.clearInterval(refreshTimer);
    };
  }, []);

  return <h1>{greeting}, {name}</h1>;
}

export default function Home() {
  const [token, setToken] = useState("");
  const [currentUser, setCurrentUser] = useState<{ fullName: string; role: string }>();
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [registerName, setRegisterName] = useState("");
  const [registerEmail, setRegisterEmail] = useState("");
  const [registerPassword, setRegisterPassword] = useState("");
  const [shipmentId, setShipmentId] = useState("");
  const [shipment, setShipment] = useState<Shipment>();
  const [eta, setEta] = useState<Eta>();
  const [etaOverrideTime, setEtaOverrideTime] = useState("");
  const [etaOverrideReason, setEtaOverrideReason] = useState("");
  const [events, setEvents] = useState<TrackingEvent[]>([]);
  const [pod, setPod] = useState<Pod>();
  const [pendingProofs, setPendingProofs] = useState<Pod[]>([]);
  const [selectedProof, setSelectedProof] = useState<Pod>();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [overviewStats, setOverviewStats] = useState<OverviewStats>();
  const [adminShipments, setAdminShipments] = useState<Shipment[]>([]);
  const [loadingAdminShipments, setLoadingAdminShipments] = useState(false);
  const [drilldown, setDrilldown] = useState<DrilldownCategory>();
  const [drilldownShipments, setDrilldownShipments] = useState<ShipmentWithRisk[]>([]);
  const [drilldownLoading, setDrilldownLoading] = useState(false);
  const [message, setMessage] = useState("Register as a customer, then log in to create a shipment.");
  const [packages, setPackages] = useState<PackageItem[]>([{ description: "", quantity: 1, fragile: false }]);
  const [podFile, setPodFile] = useState<File>();
  const [recipient, setRecipient] = useState("");
  const [deliveryNotes, setDeliveryNotes] = useState("");
  const [route, setRoute] = useState<Route>();
  const [routeHistory, setRouteHistory] = useState<Route[]>([]);
  const [trafficCondition, setTrafficCondition] = useState("NORMAL");
  const [trackingStatus, setTrackingStatus] = useState("IN_TRANSIT");
  const [trackingLocation, setTrackingLocation] = useState("");
  const [mapEstimate, setMapEstimate] = useState<{ minutes: number; expectedArrival: string }>();
  const [searchStatus, setSearchStatus] = useState("");
  const [searchDate, setSearchDate] = useState("");
  const [loadingShipment, setLoadingShipment] = useState(false);
  const [creatingShipment, setCreatingShipment] = useState(false);
  const creatingShipmentRef = useRef(false);
  const [activeTab, setActiveTab] = useState<DashboardTab>("overview");
  const [showAuth, setShowAuth] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [authFeedback, setAuthFeedback] = useState<{ text: string; tone: "success" | "error" }>();
  const [toast, setToast] = useState<{ text: string; tone: "success" | "error" | "info" }>();
  const unreadCount = useMemo(() => notifications.filter((item) => !item.readAt).length, [notifications]);

  function showToast(text: string, tone: "success" | "error" | "info" = "info") {
    setMessage(text);
    setToast({ text, tone });
  }

  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(undefined), 4500);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthFeedback(undefined);
    try {
      const result = await request<LoginResult>("/api/auth/login", "", {
        method: "POST",
        body: JSON.stringify({ email: loginEmail, password: loginPassword }),
      });
      setToken(result.token);
      setCurrentUser(result.user);
      void loadOverviewStats(result.token, result.user);
      if (result.user.role === "ADMINISTRATOR") void loadAdminShipments(result.token);
      showToast(`Welcome, ${result.user.fullName}.`, "success");
      setShowAuth(false);
    } catch (error) {
      const text = loginErrorMessage(error);
      setAuthFeedback({ text, tone: "error" });
      showToast(text, "error");
    }
  }

  async function registerCustomer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthFeedback(undefined);
    try {
      await request("/api/auth/register", "", {
        method: "POST",
        body: JSON.stringify({
          fullName: registerName,
          email: registerEmail,
          password: registerPassword,
          role: "CUSTOMER",
        }),
      });
      setLoginEmail(registerEmail);
      setLoginPassword(registerPassword);
      const text = "Customer account created. You can now sign in.";
      setAuthFeedback({ text, tone: "success" });
      showToast(text, "success");
      setAuthMode("login");
    } catch (error) {
      const text = registrationErrorMessage(error);
      setAuthFeedback({ text, tone: "error" });
      showToast(text, "error");
    }
  }

  function logout() {
    setToken("");
    setCurrentUser(undefined);
    setShipment(undefined);
    setEta(undefined);
    setEvents([]);
    setPod(undefined);
    setRoute(undefined);
    setRouteHistory([]);
    setNotifications([]);
    setOverviewStats(undefined);
    setAdminShipments([]);
    setActiveTab("overview");
    setShowAuth(false);
    setLoginPassword("");
    showToast("You have been logged out.", "success");
  }

  function changeTab(tab: DashboardTab) {
    setActiveTab(tab);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function loadNotifications() {
    try { setNotifications(await request<Notification[]>("/api/notifications", token)); }
    catch (error) { showToast(errorMessage(error, "Could not load notifications."), "error"); }
  }

  async function loadOverviewStats(
    authToken = token,
    user = currentUser,
  ) {
    if (!authToken || !user) return;
    const analyticsPath = user.role === "CUSTOMER"
      ? "/api/analytics/customer"
      : user.role === "BUSINESS_CLIENT"
        ? "/api/analytics/business-client"
        : user.role === "ADMINISTRATOR"
          ? "/api/analytics/admin"
          : undefined;
    if (!analyticsPath) {
      setOverviewStats(undefined);
      return;
    }
    try {
      setOverviewStats(await request<OverviewStats>(analyticsPath, authToken));
    } catch {
      // The normal shipment screen stays usable if this optional request fails.
    }
  }

  async function loadAdminShipments(authToken = token) {
    if (!authToken) return;
    setLoadingAdminShipments(true);
    try {
      setAdminShipments(await request<Shipment[]>("/api/shipments", authToken));
    } catch (error) {
      showToast(errorMessage(error, "Could not load all shipments."), "error");
    } finally {
      setLoadingAdminShipments(false);
    }
  }

  async function selectAdminShipment(item: Shipment) {
    setShipmentId(String(item.id));
    await loadShipment(true, String(item.id));
    showToast(`${item.trackingNumber} is ready for an Admin update.`, "success");
  }

  async function openDrilldown(category: DrilldownCategory) {
    setDrilldown(category);
    setDrilldownShipments([]);
    setDrilldownLoading(true);
    try {
      if (category === "alerts") {
        setNotifications(await request<Notification[]>("/api/notifications", token));
        return;
      }

      const shipments = await request<Shipment[]>("/api/shipments", token);
      if (category === "active") {
        setDrilldownShipments(shipments.filter((item) => !["DELIVERED", "FAILED", "FAILED_DELIVERY", "CANCELLED"].includes(item.status)));
        return;
      }
      if (category === "delivered") {
        setDrilldownShipments(shipments.filter((item) => item.status === "DELIVERED"));
        return;
      }

      const shipmentsWithRisk = await Promise.all(shipments.map(async (item): Promise<ShipmentWithRisk> => {
        try {
          const prediction = await request<Eta>(`/api/eta/${item.id}`, token);
          return { ...item, delayRiskScore: prediction.delayRiskScore };
        } catch {
          return item;
        }
      }));
      setDrilldownShipments(shipmentsWithRisk.filter((item) => item.status === "DELAYED" || (item.delayRiskScore ?? 0) >= 7));
    } catch (error) {
      showToast(errorMessage(error, "Could not load the dashboard details."), "error");
    } finally {
      setDrilldownLoading(false);
    }
  }

  async function loadShipment(quiet = false, suppliedId?: string) {
    const lookupValue = suppliedId ?? shipmentId;
    if (!lookupValue.trim()) return !quiet && showToast("Enter a shipment ID or tracking number first.", "error");
    setLoadingShipment(true);
    if (!quiet) showToast("Loading shipment details...", "info");
    const id = lookupValue.trim();
    const shipmentPath = /^\d+$/.test(id) ? `/api/shipments/${id}` : `/api/shipments/tracking/${encodeURIComponent(id)}`;
    let loadedShipment: Shipment;
    try {
      loadedShipment = await request<Shipment>(shipmentPath, token);
    } catch (error) {
      setLoadingShipment(false);
      if (!quiet) showToast(errorMessage(error, "Could not load this shipment."), "error");
      return;
    }

    const canViewProof = currentUser?.role !== "CUSTOMER" && currentUser?.role !== undefined;
    const [etaResult, eventsResult, podResult, routeResult, routeHistoryResult] = await Promise.allSettled([
      request<Eta>(`/api/eta/${loadedShipment.id}`, token),
      request<TrackingEvent[]>(`/api/tracking/${loadedShipment.id}`, token),
      canViewProof ? request<Pod>(`/api/pod/${loadedShipment.id}`, token) : Promise.resolve(undefined),
      request<Route>(`/api/routes/${loadedShipment.id}`, token),
      request<Route[]>(`/api/routes/${loadedShipment.id}/history`, token),
    ]);
    setShipment(loadedShipment);
    setShipmentId(String(loadedShipment.id));
    setEta(etaResult.status === "fulfilled" ? etaResult.value : undefined);
    if (etaResult.status === "fulfilled") {
      setEtaOverrideTime(etaResult.value.predictedDeliveryTime.slice(0, 16));
      setEtaOverrideReason(etaResult.value.overrideReason ?? "");
    }
    setEvents(eventsResult.status === "fulfilled" ? eventsResult.value : []);
    setPod(podResult.status === "fulfilled" ? podResult.value : undefined);
    setRoute(routeResult.status === "fulfilled" ? routeResult.value : undefined);
    setRouteHistory(routeHistoryResult.status === "fulfilled" ? routeHistoryResult.value : []);
    if (!quiet) showToast("Shipment loaded successfully.", "success");
    setLoadingShipment(false);
    loadNotifications();
  }

  useEffect(() => {
    if (!shipment || !token || !["CUSTOMER", "BUSINESS_CLIENT"].includes(currentUser?.role ?? "")) return;
    const refreshTimer = window.setInterval(() => { void loadShipment(true); }, 30_000);
    return () => window.clearInterval(refreshTimer);
    // Poll only while the same shipment is selected; restarting after every response is unnecessary.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shipment?.id, token, currentUser?.role]);

  async function createShipment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (creatingShipmentRef.current) return;
    const form = new FormData(event.currentTarget);
    const validPackages = packages.filter((item) => item.description.trim());
    if (!validPackages.length) return showToast("Add at least one package description.", "error");
    creatingShipmentRef.current = true;
    setCreatingShipment(true);
    showToast("Creating your shipment...", "info");
    try {
      const created = await request<Shipment>("/api/shipments", token, { method: "POST", body: JSON.stringify({
        senderName: form.get("senderName"), receiverName: form.get("receiverName"), pickupAddress: form.get("pickupAddress"),
        deliveryAddress: form.get("deliveryAddress"), priority: form.get("priority"), packages: validPackages,
      }) });
      setShipment(created); setShipmentId(String(created.id)); setEvents([]); setPackages([{ description: "", quantity: 1, fragile: false }]);
      void loadOverviewStats();
      showToast(`Shipment created. Your tracking number is ${created.trackingNumber}.`, "success");
    } catch (error) { showToast(errorMessage(error, "Could not create shipment."), "error"); }
    finally {
      creatingShipmentRef.current = false;
      setCreatingShipment(false);
    }
  }

  function changePackage(index: number, updates: Partial<PackageItem>) {
    setPackages((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, ...updates } : item));
  }

  async function submitPod(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!shipmentId || !podFile) return showToast("Choose a shipment and a delivery photo first.", "error");
    const data = new FormData(); data.append("photo", podFile); data.append("deliveredToName", recipient); data.append("deliveryNotes", deliveryNotes);
    try {
      setPod(await request<Pod>(`/api/pod/${shipmentId}`, token, { method: "POST", body: data }));
      void loadOverviewStats();
      showToast("Proof submitted. Shipment status is now delivered.", "success");
    } catch (error) { showToast(errorMessage(error, "Could not submit proof."), "error"); }
  }

  async function markRead(item: Notification) {
    try {
      if (!item.readAt) await request(`/api/notifications/${item.id}/read`, token, { method: "PATCH" });
      setNotifications((current) => current.map((notification) => notification.id === item.id ? { ...notification, readAt: new Date().toISOString() } : notification));
    } catch (error) { showToast(errorMessage(error, "Could not update this notification."), "error"); }
  }

  async function loadPendingProofs() {
    try {
      const proofs = await request<Pod[]>("/api/pod/pending", token);
      setPendingProofs(proofs);
      showToast(`${proofs.length} proof(s) are waiting for verification.`, "success");
    } catch (error) {
      showToast(errorMessage(error, "Could not load the verification queue."), "error");
    }
  }

  async function openProof(proof: Pod) {
    try {
      setSelectedProof(await request<Pod>(`/api/pod/${proof.shipmentId}`, token));
    } catch (error) {
      showToast(errorMessage(error, "Could not load proof details."), "error");
    }
  }

  async function verifyProof(verificationStatus: "VERIFIED" | "REJECTED") {
    if (!selectedProof) return;
    try {
      const updated = await request<Pod>(`/api/pod/${selectedProof.shipmentId}/verify`, token, {
        method: "PATCH",
        body: JSON.stringify({ verificationStatus }),
      });
      setSelectedProof(updated);
      setPendingProofs((current) => current.filter((proof) => proof.shipmentId !== updated.shipmentId));
      showToast(`Proof for shipment ${updated.shipmentId} was ${verificationStatus.toLowerCase()}.`, "success");
    } catch (error) {
      showToast(errorMessage(error, "Could not verify this proof."), "error");
    }
  }

  async function createRoute(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!shipmentId) return showToast("Load a shipment before creating a route.", "error");
    try {
      const created = await request<Route>("/api/routes", token, {
        method: "POST",
        body: JSON.stringify({ shipmentId: Number(shipmentId), trafficCondition }),
      });
      setRoute(created);
      setRouteHistory(await request<Route[]>(`/api/routes/${created.shipmentId}/history`, token));
      showToast("Route saved. Any earlier route is now kept in route history.", "success");
    } catch (error) {
      showToast(errorMessage(error, "Could not create the route."), "error");
    }
  }

  async function overrideEta(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!shipment || !etaOverrideTime) return showToast("Load a shipment and choose a new arrival time first.", "error");
    try {
      const updated = await request<Eta>(`/api/eta/${shipment.id}/override`, token, {
        method: "PATCH",
        body: JSON.stringify({ predictedDeliveryTime: etaOverrideTime, reason: etaOverrideReason }),
      });
      setEta(updated);
      showToast("ETA prediction updated. The customer will receive a notification.", "success");
    } catch (error) {
      showToast(errorMessage(error, "Could not update the ETA prediction."), "error");
    }
  }

  async function addTrackingEvent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!shipmentId) return showToast("Load a shipment before adding a tracking update.", "error");
    try {
      await request(`/api/tracking/${shipmentId}`, token, {
        method: "POST",
        body: JSON.stringify({ status: trackingStatus, location: trackingLocation }),
      });
      void loadOverviewStats();
      await loadShipment(true);
      if (currentUser?.role === "ADMINISTRATOR") void loadAdminShipments();
      showToast("Status updated. The customer timeline and notification have been updated.", "success");
    } catch (error) {
      showToast(errorMessage(error, "Could not add the tracking update."), "error");
    }
  }

  const role = currentUser?.role ?? "GUEST";
  const overviewActiveShipments = overviewStats?.activeShipments
    ?? (shipment?.status && shipment.status !== "DELIVERED" ? 1 : events.filter((event) => event.status === "IN_TRANSIT").length);
  const overviewDeliveredOrders = overviewStats?.statusBreakdown?.DELIVERED
    ?? (shipment?.status === "DELIVERED" ? 1 : events.filter((event) => event.status === "DELIVERED").length);
  const overviewAttentionRequired = overviewStats?.attentionRequired
    ?? overviewStats?.delayedShipments
    ?? overviewStats?.delayedShipmentCount
    ?? overviewStats?.statusBreakdown?.DELAYED
    ?? (eta?.delayRiskScore && eta.delayRiskScore >= 7 ? 1 : 0);
  const authModal = showAuth && <AuthModal mode={authMode} onModeChange={(mode) => { setAuthMode(mode); setAuthFeedback(undefined); }} onClose={() => { setShowAuth(false); setAuthFeedback(undefined); }} onLogin={login} onRegister={registerCustomer} loginEmail={loginEmail} loginPassword={loginPassword} registerName={registerName} registerEmail={registerEmail} registerPassword={registerPassword} setLoginEmail={setLoginEmail} setLoginPassword={setLoginPassword} setRegisterName={setRegisterName} setRegisterEmail={setRegisterEmail} setRegisterPassword={setRegisterPassword} feedback={authFeedback} />;

  if (!token) {
    return <Layout userName={currentUser?.fullName} role={role} activeTab={activeTab} onTabChange={changeTab} onOpenAuth={() => setShowAuth(true)} onLogout={logout}>
      <section className="hero-section"><div><p className="eyebrow">SHIPTRACK PRO</p><h1>Shipment tracking, simplified.</h1><p className="subtitle">Follow every delivery from pickup to proof of delivery.</p></div><div className="hero-status"><span className="status-dot" /> Public tracking</div></section>
      {toast && <div className={`toast ${toast.tone}`} role="status">{toast.text}</div>}
      <PublicTracker trackingNumber={shipmentId} onTrackingNumberChange={setShipmentId} onTrack={loadShipment} events={events} message={message} />
      {authModal}
    </Layout>;
  }

  return <Layout
    userName={currentUser?.fullName}
    role={currentUser?.role}
    activeTab={activeTab}
    onTabChange={changeTab}
    onOpenAuth={() => setShowAuth(true)}
    onLogout={logout}
  >
    <section id="overview" className="hero-section">
      <div><p className="eyebrow">{role === "ADMINISTRATOR" ? "ADMIN OPERATIONS CENTER" : "SHIPMENT CONTROL CENTER"}</p><TimeGreeting name={currentUser?.fullName?.split(" ")[0] ?? "there"} /><p className="subtitle">{role === "ADMINISTRATOR" ? "Review all shipments, publish status updates, and manage delivery operations." : "Monitor deliveries, manage exceptions, and keep every shipment moving."}</p></div>
      <div className="hero-status"><span className="status-dot" /> Live operations <small>Updated just now</small></div>
    </section>

    {false && <section className="grid"><form className="card form" onSubmit={registerCustomer}><h2>Create customer account</h2><input required value={registerName} onChange={(event) => setRegisterName(event.target.value)} placeholder="Full name" /><input required type="email" value={registerEmail} onChange={(event) => setRegisterEmail(event.target.value)} placeholder="Email" /><input required minLength={8} type="password" value={registerPassword} onChange={(event) => setRegisterPassword(event.target.value)} placeholder="Password (minimum 8 characters)" /><button type="submit">Register as customer</button></form>
      <form className="card form" onSubmit={login}><h2>Login</h2><input required type="email" value={loginEmail} onChange={(event) => setLoginEmail(event.target.value)} placeholder="Email" /><input required type="password" value={loginPassword} onChange={(event) => setLoginPassword(event.target.value)} placeholder="Password" /><button type="submit">Login</button><small>Customers can create shipments. Admin is used for verification and management.</small></form></section>
    }
    {toast && <div className={`toast ${toast.tone}`} role="status">{toast.text}</div>}

    {activeTab === "overview" && <section className="summary-grid">
      <SummaryCard label="Active shipments" value={overviewActiveShipments} detail="Created, in transit, or out for delivery" tone="blue" icon="↗" onClick={() => void openDrilldown("active")} />
      <SummaryCard label="Delivered orders" value={overviewDeliveredOrders} detail="Successfully completed" tone="emerald" icon="✓" onClick={() => void openDrilldown("delivered")} />
      <SummaryCard label="Attention required" value={overviewAttentionRequired} detail="Shipments with elevated risk or delay" tone="amber" icon="!" onClick={() => void openDrilldown("attention")} />
      <SummaryCard label="Unread alerts" value={unreadCount} detail="Updates waiting for review" tone="rose" icon="◌" onClick={() => void openDrilldown("alerts")} />
    </section>}

    {drilldown && <div className="modal-backdrop" role="presentation" onMouseDown={() => setDrilldown(undefined)}><section className="drilldown-modal" role="dialog" aria-modal="true" aria-label="Dashboard details" onMouseDown={(event) => event.stopPropagation()}><button type="button" className="drilldown-close" aria-label="Close details" onClick={() => setDrilldown(undefined)}>×</button><p className="eyebrow">DASHBOARD DETAILS</p><h2>{drilldown === "active" ? "Active shipments" : drilldown === "delivered" ? "Delivered orders" : drilldown === "attention" ? "Shipments needing attention" : "Unread alerts"}</h2><p className="subtitle">{drilldown === "active" ? "Created, picked up, in transit, or out for delivery." : drilldown === "delivered" ? "Only shipments that have been completed." : drilldown === "attention" ? "Delayed shipments and shipments with a delay-risk score of 7 or higher." : "Only alerts you have not read yet."}</p>{drilldownLoading ? <LoadingSkeleton /> : drilldown === "alerts" ? <div className="drilldown-list">{notifications.filter((item) => !item.readAt).map((item) => <article className="drilldown-item" key={item.id}><strong>{item.title}</strong><span>{item.message}</span>{item.shipmentId && <small>Shipment #{item.shipmentId}</small>}</article>)}{!notifications.some((item) => !item.readAt) && <p className="empty-state">There are no unread alerts.</p>}</div> : <div className="drilldown-list">{drilldownShipments.map((item) => <article className={`drilldown-item ${drilldown === "attention" ? "risk" : ""}`} key={item.id}><strong>{item.trackingNumber}<span className="chip">{item.status.replaceAll("_", " ")}</span></strong><span>{item.pickupAddress} → {item.deliveryAddress}</span><small>{item.priority && `${item.priority} priority · `}Created {item.createdAt ? new Date(item.createdAt).toLocaleString() : "date not available"}{item.delayRiskScore !== undefined && ` · Delay risk ${item.delayRiskScore}/10`}</small></article>)}{!drilldownShipments.length && <p className="empty-state">No matching shipments were found.</p>}</div>}</section></div>}

    {(activeTab === "overview" || activeTab === "tracking") && <section className="card filter-card">
      <div className="section-heading"><div><p className="eyebrow">FIND A SHIPMENT</p><h2>Search and filter</h2></div><span className="muted-label">Fast lookup</span></div>
      <SearchFilterBar value={shipmentId} status={searchStatus} date={searchDate} onValueChange={setShipmentId} onStatusChange={setSearchStatus} onDateChange={setSearchDate} onClear={() => setShipmentId("")} onSearch={loadShipment} />
    </section>}

    {activeTab === "tracking" && <section className="grid">
      <section className="card"><div className="section-heading"><div><p className="eyebrow">SHIPMENT OVERVIEW</p><h2>Current shipment</h2></div>{shipment && <span className="chip">{shipment.status.replaceAll("_", " ")}</span>}</div>{loadingShipment ? <LoadingSkeleton /> : shipment ? <div className="shipment"><strong>{shipment.trackingNumber}</strong><p>{shipment.pickupAddress} → {shipment.deliveryAddress}</p><p>{shipment.packages.length} package(s)</p></div> : <p className="empty-state">Search for a shipment to view its details.</p>}</section>
      <section className={`eta ${riskStyle(eta?.delayRiskScore)}`}><h3>Predicted arrival</h3>{eta ? <><p className="eta-duration">Package should arrive in <strong>{remainingTimeLabel(eta.estimatedRemainingMinutes)}</strong>.</p><p>Expected arrival: <strong>{new Date(eta.predictedDeliveryTime).toLocaleString()}</strong>{eta.manuallyAdjusted && <span className="chip">ADMIN UPDATED</span>}</p><p>Delay risk: <strong>{eta.delayRiskScore}/10</strong> · Confidence: <strong>{eta.confidenceScore}%</strong></p><small>Prediction basis: {eta.factors}</small></> : <p>Load a shipment to see its predicted arrival time.</p>}</section>
    </section>}

    {activeTab === "management" && (role === "CUSTOMER" || role === "BUSINESS_CLIENT") && <ShipmentManagement><section id="create-shipment" className="grid"><form className="card form" onSubmit={createShipment}><h2>Create shipment</h2><p>Fill in the sender, receiver, and package details. We will generate a tracking number after you create it.</p><input name="senderName" required placeholder="Sender name" /><input name="receiverName" required placeholder="Receiver name" /><input name="pickupAddress" required placeholder="Pickup address" /><input name="deliveryAddress" required placeholder="Delivery address" /><select name="priority" defaultValue="STANDARD"><option value="STANDARD">Standard</option><option value="EXPRESS">Express</option></select><h3>Packages</h3>
      {packages.map((item, index) => <div className="package" key={index}><input value={item.description} onChange={(event) => changePackage(index, { description: event.target.value })} placeholder="Package description" /><input type="number" min="1" value={item.quantity} onChange={(event) => changePackage(index, { quantity: Number(event.target.value) })} /><label><input type="checkbox" checked={item.fragile} onChange={(event) => changePackage(index, { fragile: event.target.checked })} /> Fragile</label>{packages.length > 1 && <button type="button" className="soft" onClick={() => setPackages((current) => current.filter((_, itemIndex) => itemIndex !== index))}>Remove</button>}</div>)}
      <button type="button" className="soft" onClick={() => setPackages((current) => [...current, { description: "", quantity: 1, fragile: false }])} disabled={creatingShipment}>+ Add package</button><button type="submit" disabled={creatingShipment} aria-busy={creatingShipment}>{creatingShipment ? "Creating shipment..." : "Create shipment"}</button></form>
      <section className="card"><div className="section-heading"><div><p className="eyebrow">SHIPMENT OVERVIEW</p><h2>Current shipment</h2></div>{shipment && <span className="chip">{shipment.status.replaceAll("_", " ")}</span>}</div>{loadingShipment ? <LoadingSkeleton /> : shipment && <div className="shipment"><strong>{shipment.trackingNumber}</strong><p>{shipment.pickupAddress} → {shipment.deliveryAddress}</p><p>{shipment.packages.length} package(s)</p>{expectedArrival(shipment) && <p>Expected arrival: <strong>{expectedArrival(shipment)?.toLocaleString()}</strong></p>}</div>}
        <div className={`eta ${riskStyle(eta?.delayRiskScore)}`}><h3>Predicted arrival</h3>{eta ? <><p className="eta-duration">Package should arrive in <strong>{remainingTimeLabel(eta.estimatedRemainingMinutes)}</strong>.</p><p>Expected arrival: <strong>{new Date(eta.predictedDeliveryTime).toLocaleString()}</strong>{eta.manuallyAdjusted && <span className="chip">ADMIN UPDATED</span>}</p><p>Delay risk: <strong>{eta.delayRiskScore}/10</strong> · Confidence: <strong>{eta.confidenceScore}%</strong></p><small>Prediction basis: {eta.factors}</small></> : mapEstimate ? <><p>Approximate expected arrival: <strong>{new Date(mapEstimate.expectedArrival).toLocaleString()}</strong></p><small>Based on the current OpenStreetMap route time of about {mapEstimate.minutes} minutes.</small></> : <p>Load a shipment to see its predicted arrival time.</p>}</div></section>
    </section></ShipmentManagement>}

    {activeTab === "tracking" && <><DeliveryMap shipment={shipment} onRouteReady={setMapEstimate} />

    <RouteHistory routes={routeHistory} />

    <section className={`grid ${role === "CUSTOMER" ? "single-column" : ""}`}><section className="card"><div className="section-heading"><div><p className="eyebrow">TRACKING</p><h2>Delivery timeline</h2></div><span className="muted-label">{events.length} updates</span></div>{!events.length && <p className="empty-state">Load a shipment to see its delivery milestones.</p>}<ShipmentTimeline events={events} currentStatus={shipment?.status} /></section>
      {role !== "CUSTOMER" && <section className="card pod"><h2>Proof of delivery</h2>{pod ? <><p>Received by: <strong>{pod.deliveredToName}</strong></p><p>Verification: <span className="chip">{pod.verificationStatus}</span></p>{pod.deliveryNotes && <p>Notes: {pod.deliveryNotes}</p>}<div className="proof-images">{pod.signatureUrl && <img src={`${API_URL}${pod.signatureUrl}`} alt="Delivery signature" />}{pod.photoUrl && <img src={`${API_URL}${pod.photoUrl}`} alt="Delivery proof" />}</div></> : role === "LOGISTICS_OPERATOR" ? <form className="form" onSubmit={submitPod}><p>Select an assigned shipment, then upload its delivery proof.</p><input required value={recipient} onChange={(event) => setRecipient(event.target.value)} placeholder="Recipient name" /><input required type="file" accept="image/*" onChange={(event) => setPodFile(event.target.files?.[0])} /><textarea value={deliveryNotes} onChange={(event) => setDeliveryNotes(event.target.value)} placeholder="Delivery notes (optional)" /><button type="submit">Complete delivery</button></form> : <p className="empty-state">Proof has not been submitted yet.</p>}</section>}</section>
      </>}

    {activeTab === "management" && (role === "LOGISTICS_OPERATOR" || role === "ADMINISTRATOR") && <ShipmentManagement>{role === "ADMINISTRATOR" && <section className="admin-control"><div className="section-heading"><div><p className="eyebrow">ADMIN CONTROL CENTER</p><h2>All shipments</h2><p>Choose any shipment to view its details, route, and timeline before publishing a new status.</p></div><button type="button" className="soft" onClick={() => void loadAdminShipments()}>{loadingAdminShipments ? "Loading..." : "Refresh shipment lists"}</button></div>{loadingAdminShipments ? <LoadingSkeleton /> : <div className="grid admin-shipment-lists"><section className="card"><h3>Current shipments</h3><p className="muted-label">Created, picked up, in transit, or out for delivery</p><div className="queue">{adminShipments.filter((item) => !["DELIVERED", "FAILED_DELIVERY", "CANCELLED"].includes(item.status)).map((item) => <button type="button" className={`queue-item ${shipment?.id === item.id ? "selected" : ""}`} key={item.id} onClick={() => void selectAdminShipment(item)}><strong>{item.trackingNumber}</strong><span>{item.pickupAddress} → {item.deliveryAddress}</span><small>Status: {item.status.replaceAll("_", " ")}</small></button>)}{!adminShipments.some((item) => !["DELIVERED", "FAILED_DELIVERY", "CANCELLED"].includes(item.status)) && <p className="empty-state">No current shipments.</p>}</div></section><section className="card"><h3>Shipment history</h3><p className="muted-label">Delivered, failed, or cancelled shipments</p><div className="queue">{adminShipments.filter((item) => ["DELIVERED", "FAILED_DELIVERY", "CANCELLED"].includes(item.status)).map((item) => <button type="button" className={`queue-item ${shipment?.id === item.id ? "selected" : ""}`} key={item.id} onClick={() => void selectAdminShipment(item)}><strong>{item.trackingNumber}</strong><span>{item.pickupAddress} → {item.deliveryAddress}</span><small>Status: {item.status.replaceAll("_", " ")}</small></button>)}{!adminShipments.some((item) => ["DELIVERED", "FAILED_DELIVERY", "CANCELLED"].includes(item.status)) && <p className="empty-state">No shipment history yet.</p>}</div></section></div>}</section>}
      <section id="operations" className="grid"><form className="card form" onSubmit={createRoute}><h2>Admin/Operator: create or replace route</h2><p>Create a route after loading a shipment. Creating another route keeps the previous one in history and marks this one as current.</p><select value={trafficCondition} onChange={(event) => setTrafficCondition(event.target.value)}><option value="NORMAL">Normal traffic</option><option value="HEAVY">Heavy traffic</option><option value="LIGHT">Light traffic</option></select><button type="submit">Create route</button>{route && <p>Distance: <strong>{route.distanceKm ?? "Not available"}</strong> km<br />Estimated time: <strong>{route.estimatedTimeMinutes ?? "Not available"}</strong> minutes<br />{route.selectionReason && <small>{route.selectionReason}</small>}</p>}</form>
      <form className="card form" onSubmit={addTrackingEvent}><h2>Admin/Operator: update shipment status</h2>{shipment ? <p>Selected shipment: <strong>{shipment.trackingNumber}</strong></p> : <p>Select a shipment from the Admin list, or load one above.</p>}<select value={trackingStatus} onChange={(event) => setTrackingStatus(event.target.value)}><option value="PICKED_UP">Picked up</option><option value="IN_TRANSIT">In transit</option><option value="OUT_FOR_DELIVERY">Out for delivery</option><option value="DELIVERED">Delivered</option><option value="FAILED_DELIVERY">Delivery failed</option><option value="CANCELLED">Cancelled</option></select><input value={trackingLocation} onChange={(event) => setTrackingLocation(event.target.value)} placeholder="Current location, for example: Meerut" /><button type="submit">Publish status update</button><small>The customer receives an in-app notification and an email, and their delivery timeline refreshes automatically.</small></form>
      {role === "ADMINISTRATOR" && <form className="card form" onSubmit={overrideEta}><h2>Admin: adjust predicted arrival</h2><p>Use this when an operator reports a revised delivery time. The manual prediction is kept until an Admin changes it again.</p><input required type="datetime-local" value={etaOverrideTime} onChange={(event) => setEtaOverrideTime(event.target.value)} /><textarea value={etaOverrideReason} onChange={(event) => setEtaOverrideReason(event.target.value)} placeholder="Reason for this update (optional)" /><button type="submit">Update ETA prediction</button></form>}</section></ShipmentManagement>}

    {activeTab === "pod" && role === "LOGISTICS_OPERATOR" && <PodVerificationQueue><section className="grid"><section className="card"><h2>Complete delivery</h2>{pod ? <><p>Received by: <strong>{pod.deliveredToName}</strong></p><p>Verification: <span className="chip">{pod.verificationStatus}</span></p>{pod.deliveryNotes && <p>Notes: {pod.deliveryNotes}</p>}<div className="proof-images">{pod.signatureUrl && <img src={`${API_URL}${pod.signatureUrl}`} alt="Delivery signature" />}{pod.photoUrl && <img src={`${API_URL}${pod.photoUrl}`} alt="Delivery proof" />}</div></> : <form className="form" onSubmit={submitPod}><p>Load one of your assigned shipments and upload its delivery proof.</p><input required value={recipient} onChange={(event) => setRecipient(event.target.value)} placeholder="Recipient name" /><input required type="file" accept="image/*" onChange={(event) => setPodFile(event.target.files?.[0])} /><textarea value={deliveryNotes} onChange={(event) => setDeliveryNotes(event.target.value)} placeholder="Delivery notes (optional)" /><button type="submit">Complete delivery</button></form>}</section></section></PodVerificationQueue>}

    {activeTab === "pod" && (role === "SUPPORT_AGENT" || role === "ADMINISTRATOR") && <PodVerificationQueue><section id="verification" className="grid"><section className="card"><h2>Proof verification queue</h2><p>Review delivery proofs waiting for approval.</p><button onClick={loadPendingProofs}>Load pending proofs</button>{!pendingProofs.length && <p>No pending proofs are loaded.</p>}<div className="queue">{pendingProofs.map((proof) => <button className="queue-item" key={proof.shipmentId} onClick={() => openProof(proof)}><strong>Shipment #{proof.shipmentId}</strong><span>Received by {proof.deliveredToName}</span><small>{proof.deliveredAt ? new Date(proof.deliveredAt).toLocaleString() : "Date not available"}</small></button>)}</div></section>
      <section className="card"><h2>Proof review details</h2>{selectedProof ? <><p>Shipment: <strong>#{selectedProof.shipmentId}</strong></p><p>Received by: <strong>{selectedProof.deliveredToName}</strong></p>{selectedProof.deliveryNotes && <p>Notes: {selectedProof.deliveryNotes}</p>}<div className="proof-images">{selectedProof.signatureUrl && <img src={`${API_URL}${selectedProof.signatureUrl}`} alt="Full delivery signature" />}{selectedProof.photoUrl && <img src={`${API_URL}${selectedProof.photoUrl}`} alt="Full delivery photo" />}</div><div className="actions"><button onClick={() => verifyProof("VERIFIED")}>Approve proof</button><button className="danger" onClick={() => verifyProof("REJECTED")}>Reject proof</button></div></> : <p>Select a proof from the queue to see its signature and photo.</p>}</section></section></PodVerificationQueue>}

    {activeTab === "notifications" && <section className="module-view"><div className="module-heading"><p className="eyebrow">NOTIFICATIONS</p><h2>Notification center</h2><p>Review shipment updates and delivery alerts.</p></div><div className="notification-list">{notifications.map((item) => <button key={item.id} className={`notification-item ${item.readAt ? "read" : ""}`} onClick={() => markRead(item)}><strong>{item.title}</strong><span>{item.message}</span></button>)}{!notifications.length && <p className="empty-state">No notifications yet.</p>}</div></section>}
    {activeTab === "analytics" && <section id="analytics"><AnalyticsDashboard role={role === "ADMINISTRATOR" ? "admin" : role === "BUSINESS_CLIENT" ? "business-client" : "customer"} token={token} /><ReportExporter token={token} /></section>}
    {authModal}
  </Layout>;
}
