export default function PublicTracker() {
  return (
    <section className="public-tracker">
      <div className="public-tracker-copy">
        <p className="eyebrow">DELIVERY VISIBILITY</p>
        <h1>Every delivery, clearly managed.</h1>
        <p>Sign in to view shipment status, route details, and delivery milestones for your account.</p>
      </div>
      <div className="public-map-preview">
        <div className="map-preview-grid" />
        <div className="map-route-line" />
        <span className="map-pin start">●</span><span className="map-pin end">●</span>
        <div className="map-preview-label"><span className="status-dot" /> Live route preview</div>
      </div>
    </section>
  );
}
