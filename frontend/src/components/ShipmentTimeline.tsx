type TimelineEvent = { status: string; location?: string; eventTimestamp: string };

const steps = ["CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"];

export default function ShipmentTimeline({ events, currentStatus }: { events: TimelineEvent[]; currentStatus?: string }) {
  const normalized = currentStatus?.toUpperCase();
  const currentIndex = Math.max(steps.indexOf(normalized ?? ""), events.length ? steps.indexOf(events.at(-1)?.status.toUpperCase() ?? "") : 0);
  return (
    <div className="shipment-timeline">
      {steps.map((step, index) => {
        const event = [...events].reverse().find((item) => item.status.toUpperCase() === step);
        const complete = index <= currentIndex;
        return (
          <div className={`timeline-step ${complete ? "complete" : ""}`} key={step}>
            <div className="timeline-node">{complete ? "✓" : index + 1}</div>
            {index < steps.length - 1 && <div className={`timeline-line ${index < currentIndex ? "complete" : ""}`} />}
            <strong>{step.replaceAll("_", " ")}</strong>
            <span>{event?.location ?? (complete ? "Updated" : "Pending")}</span>
            {event && <small>{new Date(event.eventTimestamp).toLocaleDateString()}</small>}
          </div>
        );
      })}
    </div>
  );
}
