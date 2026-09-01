"use client";

import { FormEvent, useMemo, useState } from "react";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081";

type PackageItem = { description: string; quantity: number; fragile: boolean };
type Shipment = { id: number; trackingNumber: string; status: string; pickupAddress: string; deliveryAddress: string; packages: PackageItem[] };
type Eta = { predictedDeliveryTime: string; delayRiskScore: number; confidenceScore: number; factors: string };
type TrackingEvent = { id: number; status: string; location?: string; eventTimestamp: string };
type Notification = { id: number; title: string; message: string; readAt?: string };
type Pod = { deliveredToName: string; deliveryNotes?: string; photoUrl?: string; verificationStatus: string };

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
  const [shipmentId, setShipmentId] = useState("");
  const [shipment, setShipment] = useState<Shipment>();
  const [eta, setEta] = useState<Eta>();
  const [events, setEvents] = useState<TrackingEvent[]>([]);
  const [pod, setPod] = useState<Pod>();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [message, setMessage] = useState("Paste a login token, then load a shipment.");
  const [packages, setPackages] = useState<PackageItem[]>([{ description: "", quantity: 1, fragile: false }]);
  const [podFile, setPodFile] = useState<File>();
  const [recipient, setRecipient] = useState("");
  const [deliveryNotes, setDeliveryNotes] = useState("");
  const unreadCount = useMemo(() => notifications.filter((item) => !item.readAt).length, [notifications]);

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

  return <main className="page">
    <header className="header"><div><p className="eyebrow">INFOSYS SPRINGBOARD PROJECT</p><h1>ShipTrack Pro</h1><p className="subtitle">Track shipments, see delivery risk and upload delivery proof.</p></div>
      <div className="notification-area"><button className="bell" onClick={() => { setShowNotifications(!showNotifications); loadNotifications(); }}>🔔 Notifications {unreadCount > 0 && <span className="badge">{unreadCount}</span>}</button>
        {showNotifications && <div className="card notifications"><h2>Latest notifications</h2>{!notifications.length && <p>No notifications yet.</p>}{notifications.map((item) => <button key={item.id} className={item.readAt ? "notification read" : "notification"} onClick={() => markRead(item)}><strong>{item.title}</strong><span>{item.message}</span></button>)}</div>}</div>
    </header>

    <section className="card form"><label htmlFor="token">Login token</label><input id="token" value={token} onChange={(event) => setToken(event.target.value)} placeholder="Paste the token returned by login" /><small>This token is stored only in this page.</small></section>
    <p className="message" role="status">{message}</p>

    <section className="grid"><form className="card form" onSubmit={createShipment}><h2>Create shipment</h2><input name="senderName" required placeholder="Sender name" /><input name="receiverName" required placeholder="Receiver name" /><input name="pickupAddress" required placeholder="Pickup address" /><input name="deliveryAddress" required placeholder="Delivery address" /><select name="priority" defaultValue="STANDARD"><option value="STANDARD">Standard</option><option value="EXPRESS">Express</option></select><h3>Packages</h3>
      {packages.map((item, index) => <div className="package" key={index}><input value={item.description} onChange={(event) => changePackage(index, { description: event.target.value })} placeholder="Package description" /><input type="number" min="1" value={item.quantity} onChange={(event) => changePackage(index, { quantity: Number(event.target.value) })} /><label><input type="checkbox" checked={item.fragile} onChange={(event) => changePackage(index, { fragile: event.target.checked })} /> Fragile</label>{packages.length > 1 && <button type="button" className="soft" onClick={() => setPackages((current) => current.filter((_, itemIndex) => itemIndex !== index))}>Remove</button>}</div>)}
      <button type="button" className="soft" onClick={() => setPackages((current) => [...current, { description: "", quantity: 1, fragile: false }])}>+ Add package</button><button type="submit">Create shipment</button></form>
      <section className="card"><h2>Track shipment</h2><div className="search"><input value={shipmentId} onChange={(event) => setShipmentId(event.target.value)} placeholder="Shipment ID" /><button onClick={loadShipment}>Load</button></div>{shipment && <div className="shipment"><strong>{shipment.trackingNumber}</strong><p>Status: <span className="chip">{shipment.status}</span></p><p>{shipment.pickupAddress} → {shipment.deliveryAddress}</p><p>{shipment.packages.length} package(s)</p></div>}
        <div className={`eta ${riskStyle(eta?.delayRiskScore)}`}><h3>ETA and delay risk</h3>{eta ? <><p><strong>{new Date(eta.predictedDeliveryTime).toLocaleString()}</strong></p><p>Delay risk: <strong>{eta.delayRiskScore}/10</strong> · Confidence: <strong>{eta.confidenceScore}%</strong></p><small>Why: {eta.factors}</small></> : <p>ETA appears after a route and tracking update are added.</p>}</div></section>
    </section>

    <section className="grid"><section className="card"><h2>Live delivery updates</h2>{!events.length && <p>No tracking events loaded yet.</p>}<ol className="timeline">{events.map((event) => <li key={event.id}><strong>{event.status}</strong><span>{event.location || "Location not provided"}</span><small>{new Date(event.eventTimestamp).toLocaleString()}</small></li>)}</ol></section>
      <section className="card pod"><h2>Proof of delivery</h2>{pod ? <><p>Received by: <strong>{pod.deliveredToName}</strong></p><p>Verification: <span className="chip">{pod.verificationStatus}</span></p>{pod.deliveryNotes && <p>Notes: {pod.deliveryNotes}</p>}{pod.photoUrl && <a href={`${API_URL}${pod.photoUrl}`} target="_blank">View delivery photo</a>}</> : <form className="form" onSubmit={submitPod}><p>For logistics operators: select a shipment above, then upload proof.</p><input required value={recipient} onChange={(event) => setRecipient(event.target.value)} placeholder="Recipient name" /><input required type="file" accept="image/*" onChange={(event) => setPodFile(event.target.files?.[0])} /><textarea value={deliveryNotes} onChange={(event) => setDeliveryNotes(event.target.value)} placeholder="Delivery notes (optional)" /><button type="submit">Complete delivery</button></form>}</section></section>
  </main>;
}
