"use client";

type TrackingEvent = { status: string; location?: string; eventTimestamp: string };

type Props = {
  trackingNumber: string;
  onTrackingNumberChange: (value: string) => void;
  onTrack: () => void;
  events?: TrackingEvent[];
  message?: string;
};

export default function PublicTracker({
  trackingNumber,
  onTrackingNumberChange,
  onTrack,
  events = [],
  message,
}: Props) {
  return (
    <section className="public-tracker">
      <div className="public-tracker-copy">
        <p className="eyebrow">PUBLIC TRACKING</p>
        <h1>Track every delivery with confidence.</h1>
        <p>Enter a tracking number to see the latest shipment status, route preview, and delivery milestones.</p>
        <div className="public-track-form">
          <input value={trackingNumber} onChange={(event) => onTrackingNumberChange(event.target.value)} placeholder="Enter tracking number" aria-label="Tracking number" />
          <button onClick={onTrack}>Track shipment</button>
        </div>
        {message && <p className="message">{message}</p>}
      </div>
      <div className="public-map-preview">
        <div className="map-preview-grid" />
        <div className="map-route-line" />
        <span className="map-pin start">●</span><span className="map-pin end">●</span>
        <div className="map-preview-label"><span className="status-dot" /> Live route preview</div>
      </div>
      {events.length > 0 && (
        <div className="public-events">
          <h2>Latest delivery updates</h2>
          {events.map((event) => <div className="public-event" key={`${event.status}-${event.eventTimestamp}`}><strong>{event.status.replaceAll("_", " ")}</strong><span>{event.location ?? "Location unavailable"} · {new Date(event.eventTimestamp).toLocaleString()}</span></div>)}
        </div>
      )}
    </section>
  );
}
