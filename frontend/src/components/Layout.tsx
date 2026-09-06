import { ReactNode } from "react";
import Navbar from "./Navbar";

type Notification = { id: number; title: string; message: string; readAt?: string };

type Props = {
  children: ReactNode;
  userName?: string;
  role?: string;
  notifications: Notification[];
  showNotifications: boolean;
  onToggleNotifications: () => void;
  onMarkRead: (notification: Notification) => void;
  onOpenAuth: () => void;
};

export default function Layout(props: Props) {
  const role = props.role ?? "GUEST";
  const canManage = role === "ADMINISTRATOR" || role === "SUPPORT_AGENT";
  const canPod = role === "LOGISTICS_OPERATOR" || canManage;
  const canAnalytics = role === "BUSINESS_CLIENT" || canManage;
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand"><span className="brand-icon">S</span><strong>ShipTrack Pro</strong></div>
        <nav>
          <a className="nav-link active" href="#overview"><span>⌂</span> Overview</a>
          <a className="nav-link" href="#shipments"><span>◈</span> Track Shipment</a>
          {canManage && <a className="nav-link" href="#operations"><span>✦</span> Shipment Management</a>}
          {canPod && <a className="nav-link" href="#verification"><span>✓</span> POD Queue</a>}
          {canAnalytics && <a className="nav-link" href="#analytics"><span>▦</span> Analytics</a>}
        </nav>
        <div className="sidebar-footer"><span className="status-dot" /> All systems operational</div>
      </aside>
      <div className="content-shell">
        <Navbar {...props} />
        <main className="page">{props.children}</main>
      </div>
    </div>
  );
}
