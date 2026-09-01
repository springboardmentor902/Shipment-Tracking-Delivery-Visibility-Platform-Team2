"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081";

type PackageItem = { description: string; quantity: number; fragile: boolean };
type Shipment = { id: number; trackingNumber: string; status: string; pickupAddress: string; deliveryAddress: string; packages: PackageItem[] };
type Eta = { predictedDeliveryTime: string; delayRiskScore: number; confidenceScore: number; factors: string };
type TrackingEvent = { id: number; status: string; location?: string; eventTimestamp: string };
type Notification = { id: number; title: string; message: string; readAt?: string };
type LoginResult = { token: string; user: { fullName: string; role: string } };
type Route = { distanceKm?: number; estimatedTimeMinutes?: number; trafficCondition?: string };
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

function DeliveryMap({ shipment }: { shipment?: Shipment }) {
  const mapElement = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<LeafletMap>();
  const [mapMessage, setMapMessage] = useState("Load a shipment to see its OpenStreetMap route.");

  useEffect(() => {
    if (!shipment || !mapElement.current) return;
    let cancelled = false;

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
        const [origin, destination] = await Promise.all([findAddress(shipment.pickupAddress), findAddress(shipment.deliveryAddress)]);
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
  }, [shipment]);

  return <section className="card map-card"><h2>Live route map</h2><div className="map" ref={mapElement} /><small>{mapMessage}</small></section>;
}

async function request<T>(path: string, token: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (!response.ok) throw new Error((await response.text()) || "Request failed.");
  return response.status === 204 ? (undefined as T) : (await response.json() as T);
}

function riskStyle(score?: number) {
  if (score === undefined) return "";
  return score >= 7 ? "high" : score >= 4 ? "medium" : "low";
}

export default function Home() {
  const [token, setToken] = useState("");
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [registerName, setRegisterName] = useState("");
  const [registerEmail, setRegisterEmail] = useState("");
  const [registerPassword, setRegisterPassword] = useState("");
  const [shipmentId, setShipmentId] = useState("");
  const [shipment, setShipment] = useState<Shipment>();
  const [eta, setEta] = useState<Eta>();
  const [events, setEvents] = useState<TrackingEvent[]>([]);
  const [pod, setPod] = useState<Pod>();
  const [pendingProofs, setPendingProofs] = useState<Pod[]>([]);
  const [selectedProof, setSelectedProof] = useState<Pod>();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [message, setMessage] = useState("Register as a customer, then log in to create a shipment.");
  const [packages, setPackages] = useState<PackageItem[]>([{ description: "", quantity: 1, fragile: false }]);
  const [podFile, setPodFile] = useState<File>();
  const [recipient, setRecipient] = useState("");
  const [deliveryNotes, setDeliveryNotes] = useState("");
  const [route, setRoute] = useState<Route>();
  const [trafficCondition, setTrafficCondition] = useState("NORMAL");
  const [trackingStatus, setTrackingStatus] = useState("IN_TRANSIT");
  const [trackingLocation, setTrackingLocation] = useState("");
  const unreadCount = useMemo(() => notifications.filter((item) => !item.readAt).length, [notifications]);

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const result = await request<LoginResult>("/api/auth/login", "", {
        method: "POST",
        body: JSON.stringify({ email: loginEmail, password: loginPassword }),
      });
      setToken(result.token);
      setMessage(`Logged in as ${result.user.fullName} (${result.user.role}).`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Login failed.");
    }
  }

  async function registerCustomer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
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
      setMessage("Customer account created. Click Login now.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Registration failed.");
    }
  }

  async function loadNotifications() {
    try { setNotifications(await request<Notification[]>("/api/notifications", token)); }
    catch (error) { setMessage(error instanceof Error ? error.message : "Could not load notifications."); }
  }

  async function loadShipment() {
    if (!shipmentId.trim()) return setMessage("Enter a shipment ID first.");
    setMessage("Loading shipment details...");
    const id = shipmentId.trim();
    const [shipmentResult, etaResult, eventsResult, podResult] = await Promise.allSettled([
      request<Shipment>(`/api/shipments/${id}`, token), request<Eta>(`/api/eta/${id}`, token),
      request<TrackingEvent[]>(`/api/tracking/${id}`, token), request<Pod>(`/api/pod/${id}`, token),
    ]);
    if (shipmentResult.status === "rejected") return setMessage(shipmentResult.reason.message || "Could not load shipment.");
    setShipment(shipmentResult.value);
    setEta(etaResult.status === "fulfilled" ? etaResult.value : undefined);
    setEvents(eventsResult.status === "fulfilled" ? eventsResult.value : []);
    setPod(podResult.status === "fulfilled" ? podResult.value : undefined);
    setMessage("Shipment loaded successfully.");
    loadNotifications();
  }

  async function createShipment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const validPackages = packages.filter((item) => item.description.trim());
    if (!validPackages.length) return setMessage("Add at least one package description.");
    try {
      const created = await request<Shipment>("/api/shipments", token, { method: "POST", body: JSON.stringify({
        senderName: form.get("senderName"), receiverName: form.get("receiverName"), pickupAddress: form.get("pickupAddress"),
        deliveryAddress: form.get("deliveryAddress"), priority: form.get("priority"), packages: validPackages,
      }) });
      setShipmentId(String(created.id)); setPackages([{ description: "", quantity: 1, fragile: false }]);
      setMessage(`Shipment created. Tracking number: ${created.trackingNumber}`);
    } catch (error) { setMessage(error instanceof Error ? error.message : "Could not create shipment."); }
  }

  function changePackage(index: number, updates: Partial<PackageItem>) {
    setPackages((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, ...updates } : item));
  }

  async function submitPod(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!shipmentId || !podFile) return setMessage("Choose a shipment and a delivery photo first.");
    const data = new FormData(); data.append("photo", podFile); data.append("deliveredToName", recipient); data.append("deliveryNotes", deliveryNotes);
    try {
      setPod(await request<Pod>(`/api/pod/${shipmentId}`, token, { method: "POST", body: data }));
      setMessage("Proof submitted. Shipment status is now DELIVERED.");
    } catch (error) { setMessage(error instanceof Error ? error.message : "Could not submit proof."); }
  }

  async function markRead(item: Notification) {
    if (!item.readAt) await request(`/api/notifications/${item.id}/read`, token, { method: "PATCH" });
    setNotifications((current) => current.map((notification) => notification.id === item.id ? { ...notification, readAt: new Date().toISOString() } : notification));
  }

  async function loadPendingProofs() {
    try {
      const proofs = await request<Pod[]>("/api/pod/pending", token);
      setPendingProofs(proofs);
      setMessage(`${proofs.length} proof(s) are waiting for verification.`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not load the verification queue.");
    }
  }

  async function openProof(proof: Pod) {
    try {
      setSelectedProof(await request<Pod>(`/api/pod/${proof.shipmentId}`, token));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not load proof details.");
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
      setMessage(`Proof for shipment ${updated.shipmentId} was ${verificationStatus.toLowerCase()}.`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not verify this proof.");
    }
  }

  async function createRoute(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!shipmentId) return setMessage("Load or enter a shipment ID first.");
    try {
      const created = await request<Route>("/api/routes", token, {
        method: "POST",
        body: JSON.stringify({ shipmentId: Number(shipmentId), trafficCondition }),
      });
      setRoute(created);
      setMessage("Route created. Distance and time are filled when Google Maps is available.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not create the route.");
    }
  }

  async function addTrackingEvent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!shipmentId) return setMessage("Load or enter a shipment ID first.");
    try {
      await request(`/api/tracking/${shipmentId}`, token, {
        method: "POST",
        body: JSON.stringify({ status: trackingStatus, location: trackingLocation }),
      });
      setMessage("Tracking update added. Load the shipment again to see the ETA.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Could not add the tracking update.");
    }
  }

  return <main className="page">
    <header className="header"><div><p className="eyebrow">INFOSYS SPRINGBOARD PROJECT</p><h1>ShipTrack Pro</h1><p className="subtitle">Track shipments, see delivery risk and upload delivery proof.</p></div>
      <div className="notification-area"><button className="bell" onClick={() => { setShowNotifications(!showNotifications); loadNotifications(); }}>🔔 Notifications {unreadCount > 0 && <span className="badge">{unreadCount}</span>}</button>
        {showNotifications && <div className="card notifications"><h2>Latest notifications</h2>{!notifications.length && <p>No notifications yet.</p>}{notifications.map((item) => <button key={item.id} className={item.readAt ? "notification read" : "notification"} onClick={() => markRead(item)}><strong>{item.title}</strong><span>{item.message}</span></button>)}</div>}</div>
    </header>

    <section className="grid"><form className="card form" onSubmit={registerCustomer}><h2>Create customer account</h2><input required value={registerName} onChange={(event) => setRegisterName(event.target.value)} placeholder="Full name" /><input required type="email" value={registerEmail} onChange={(event) => setRegisterEmail(event.target.value)} placeholder="Email" /><input required minLength={8} type="password" value={registerPassword} onChange={(event) => setRegisterPassword(event.target.value)} placeholder="Password (minimum 8 characters)" /><button type="submit">Register as customer</button></form>
      <form className="card form" onSubmit={login}><h2>Login</h2><input required type="email" value={loginEmail} onChange={(event) => setLoginEmail(event.target.value)} placeholder="Email" /><input required type="password" value={loginPassword} onChange={(event) => setLoginPassword(event.target.value)} placeholder="Password" /><button type="submit">Login</button><small>Customers can create shipments. Admin is used for verification and management.</small></form></section>
    <section className="card form"><label htmlFor="token">Login token</label><input id="token" value={token} onChange={(event) => setToken(event.target.value)} placeholder="Token is filled automatically after login" /><small>You normally do not need to paste a token manually.</small></section>
    <p className="message" role="status">{message}</p>

    <section className="grid"><form className="card form" onSubmit={createShipment}><h2>Create shipment</h2><input name="senderName" required placeholder="Sender name" /><input name="receiverName" required placeholder="Receiver name" /><input name="pickupAddress" required placeholder="Pickup address" /><input name="deliveryAddress" required placeholder="Delivery address" /><select name="priority" defaultValue="STANDARD"><option value="STANDARD">Standard</option><option value="EXPRESS">Express</option></select><h3>Packages</h3>
      {packages.map((item, index) => <div className="package" key={index}><input value={item.description} onChange={(event) => changePackage(index, { description: event.target.value })} placeholder="Package description" /><input type="number" min="1" value={item.quantity} onChange={(event) => changePackage(index, { quantity: Number(event.target.value) })} /><label><input type="checkbox" checked={item.fragile} onChange={(event) => changePackage(index, { fragile: event.target.checked })} /> Fragile</label>{packages.length > 1 && <button type="button" className="soft" onClick={() => setPackages((current) => current.filter((_, itemIndex) => itemIndex !== index))}>Remove</button>}</div>)}
      <button type="button" className="soft" onClick={() => setPackages((current) => [...current, { description: "", quantity: 1, fragile: false }])}>+ Add package</button><button type="submit">Create shipment</button></form>
      <section className="card"><h2>Track shipment</h2><div className="search"><input value={shipmentId} onChange={(event) => setShipmentId(event.target.value)} placeholder="Shipment ID" /><button onClick={loadShipment}>Load</button></div>{shipment && <div className="shipment"><strong>{shipment.trackingNumber}</strong><p>Status: <span className="chip">{shipment.status}</span></p><p>{shipment.pickupAddress} → {shipment.deliveryAddress}</p><p>{shipment.packages.length} package(s)</p></div>}
        <div className={`eta ${riskStyle(eta?.delayRiskScore)}`}><h3>ETA and delay risk</h3>{eta ? <><p><strong>{new Date(eta.predictedDeliveryTime).toLocaleString()}</strong></p><p>Delay risk: <strong>{eta.delayRiskScore}/10</strong> · Confidence: <strong>{eta.confidenceScore}%</strong></p><small>Why: {eta.factors}</small></> : <p>ETA appears after a route and tracking update are added.</p>}</div></section>
    </section>

    <DeliveryMap shipment={shipment} />

    <section className="grid"><section className="card"><h2>Live delivery updates</h2>{!events.length && <p>No tracking events loaded yet.</p>}<ol className="timeline">{events.map((event) => <li key={event.id}><strong>{event.status}</strong><span>{event.location || "Location not provided"}</span><small>{new Date(event.eventTimestamp).toLocaleString()}</small></li>)}</ol></section>
      <section className="card pod"><h2>Proof of delivery</h2>{pod ? <><p>Received by: <strong>{pod.deliveredToName}</strong></p><p>Verification: <span className="chip">{pod.verificationStatus}</span></p>{pod.deliveryNotes && <p>Notes: {pod.deliveryNotes}</p>}<div className="proof-images">{pod.signatureUrl && <img src={`${API_URL}${pod.signatureUrl}`} alt="Delivery signature" />}{pod.photoUrl && <img src={`${API_URL}${pod.photoUrl}`} alt="Delivery proof" />}</div></> : <form className="form" onSubmit={submitPod}><p>For logistics operators: select a shipment above, then upload proof.</p><input required value={recipient} onChange={(event) => setRecipient(event.target.value)} placeholder="Recipient name" /><input required type="file" accept="image/*" onChange={(event) => setPodFile(event.target.files?.[0])} /><textarea value={deliveryNotes} onChange={(event) => setDeliveryNotes(event.target.value)} placeholder="Delivery notes (optional)" /><button type="submit">Complete delivery</button></form>}</section></section>

    <section className="grid"><form className="card form" onSubmit={createRoute}><h2>Admin/Operator: create route</h2><p>Create a route after loading a shipment. Google Maps then calculates distance and time.</p><select value={trafficCondition} onChange={(event) => setTrafficCondition(event.target.value)}><option value="NORMAL">Normal traffic</option><option value="HEAVY">Heavy traffic</option><option value="LIGHT">Light traffic</option></select><button type="submit">Create route</button>{route && <p>Distance: <strong>{route.distanceKm ?? "Not available"}</strong> km<br />Estimated time: <strong>{route.estimatedTimeMinutes ?? "Not available"}</strong> minutes</p>}</form>
      <form className="card form" onSubmit={addTrackingEvent}><h2>Admin/Operator: add tracking update</h2><select value={trackingStatus} onChange={(event) => setTrackingStatus(event.target.value)}><option value="PICKED_UP">Picked up</option><option value="IN_TRANSIT">In transit</option><option value="OUT_FOR_DELIVERY">Out for delivery</option></select><input value={trackingLocation} onChange={(event) => setTrackingLocation(event.target.value)} placeholder="Current location, for example: Meerut" /><button type="submit">Add tracking update</button><small>After adding it, click Load above to refresh the ETA.</small></form></section>

    <section className="grid"><section className="card"><h2>Support/Admin verification queue</h2><p>Use a Support Agent or Admin login token, then load the proofs waiting for review.</p><button onClick={loadPendingProofs}>Load pending proofs</button>{!pendingProofs.length && <p>No pending proofs are loaded.</p>}<div className="queue">{pendingProofs.map((proof) => <button className="queue-item" key={proof.shipmentId} onClick={() => openProof(proof)}><strong>Shipment #{proof.shipmentId}</strong><span>Received by {proof.deliveredToName}</span><small>{proof.deliveredAt ? new Date(proof.deliveredAt).toLocaleString() : "Date not available"}</small></button>)}</div></section>
      <section className="card"><h2>Proof review details</h2>{selectedProof ? <><p>Shipment: <strong>#{selectedProof.shipmentId}</strong></p><p>Received by: <strong>{selectedProof.deliveredToName}</strong></p>{selectedProof.deliveryNotes && <p>Notes: {selectedProof.deliveryNotes}</p>}<div className="proof-images">{selectedProof.signatureUrl && <img src={`${API_URL}${selectedProof.signatureUrl}`} alt="Full delivery signature" />}{selectedProof.photoUrl && <img src={`${API_URL}${selectedProof.photoUrl}`} alt="Full delivery photo" />}</div><div className="actions"><button onClick={() => verifyProof("VERIFIED")}>Approve proof</button><button className="danger" onClick={() => verifyProof("REJECTED")}>Reject proof</button></div></> : <p>Select a proof from the queue to see its signature and photo.</p>}</section></section>
  </main>;
}
