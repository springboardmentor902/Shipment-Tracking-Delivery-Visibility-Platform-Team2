import { ReactNode } from "react";

export default function ShipmentManagement({ children }: { children: ReactNode }) {
  return <section className="module-view"><div className="module-heading"><p className="eyebrow">OPERATIONS</p><h2>Shipment management</h2><p>Create shipments, plan routes, and publish tracking updates from one workspace.</p></div>{children}</section>;
}
