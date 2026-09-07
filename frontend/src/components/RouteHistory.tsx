type RouteItem = {
  id: number;
  origin: string;
  destination: string;
  distanceKm?: number;
  createdAt?: string;
  isCurrent: boolean;
  routeSummary?: string;
  selectionReason?: string;
};

export default function RouteHistory({ routes }: { routes: RouteItem[] }) {
  return (
    <section className="card route-history">
      <div className="section-heading">
        <div><p className="eyebrow">ROUTE MANAGEMENT</p><h2>Route history</h2></div>
        <span className="muted-label">{routes.length} route{routes.length === 1 ? "" : "s"}</span>
      </div>
      {!routes.length ? <p className="empty-state">No route has been created for this shipment yet.</p> : (
        <div className="route-history-list">
          {routes.map((route) => (
            <article className={`route-history-item ${route.isCurrent ? "current" : ""}`} key={route.id}>
              <div className="route-history-title"><strong>{route.isCurrent ? "Current route" : "Previous route"}</strong><span className="chip">{route.isCurrent ? "ACTIVE" : "HISTORY"}</span></div>
              <p>{route.origin} → {route.destination}</p>
              <small>{route.distanceKm === undefined ? "Distance not available" : `${route.distanceKm} km`} · {route.createdAt ? new Date(route.createdAt).toLocaleString() : "Date not available"}</small>
              {route.routeSummary && <small>{route.routeSummary}</small>}
              {route.isCurrent && route.selectionReason && <small className="route-reason">{route.selectionReason}</small>}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
