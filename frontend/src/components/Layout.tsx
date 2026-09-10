import { ReactNode } from "react";
import BrandLogo from "./BrandLogo";
import type { DashboardTab } from "../types/dashboard";
import Navbar from "./Navbar";

type Props = {
  children: ReactNode;
  userName?: string;
  role?: string;
  activeTab: DashboardTab;
  onTabChange: (tab: DashboardTab) => void;
  onOpenAuth: () => void;
  onLogout: () => void;
};

export default function Layout(props: Props) {
  const role = props.role ?? "GUEST";
  const canCreate = role === "CUSTOMER" || role === "BUSINESS_CLIENT";
  const canOperate = role === "LOGISTICS_OPERATOR" || role === "ADMINISTRATOR";
  const canReviewPod = role === "SUPPORT_AGENT" || role === "ADMINISTRATOR";
  const canPod = role === "LOGISTICS_OPERATOR" || canReviewPod;
  const canAnalytics = role === "CUSTOMER" || role === "BUSINESS_CLIENT" || role === "ADMINISTRATOR";
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand"><BrandLogo /><strong>ShipTrack Pro</strong></div>
        <nav>
          <button type="button" className={`nav-link ${props.activeTab === "overview" ? "active" : ""}`} onClick={() => props.onTabChange("overview")}><span>⌂</span> Overview</button>
          <button type="button" className={`nav-link ${props.activeTab === "tracking" ? "active" : ""}`} onClick={() => props.onTabChange("tracking")}><span>◈</span> Track Shipment</button>
          {role !== "GUEST" && <button type="button" className={`nav-link ${props.activeTab === "notifications" ? "active" : ""}`} onClick={() => props.onTabChange("notifications")}><span>◉</span> Notifications</button>}
          {(canCreate || canOperate) && <button type="button" className={`nav-link ${props.activeTab === "management" ? "active" : ""}`} onClick={() => props.onTabChange("management")}><span>✦</span> {canCreate ? "Create Shipment" : "Shipment Management"}</button>}
          {canPod && <button type="button" className={`nav-link ${props.activeTab === "pod" ? "active" : ""}`} onClick={() => props.onTabChange("pod")}><span>✓</span> {role === "LOGISTICS_OPERATOR" ? "Complete Delivery" : "Verify Delivery"}</button>}
          {canAnalytics && <button type="button" className={`nav-link ${props.activeTab === "analytics" ? "active" : ""}`} onClick={() => props.onTabChange("analytics")}><span>▦</span> Analytics</button>}
        </nav>
        <div className="sidebar-footer"><span className="status-dot" /> All systems operational</div>
      </aside>
      <div className="content-shell">
        <Navbar userName={props.userName} role={props.role} onOpenAuth={props.onOpenAuth} onLogout={props.onLogout} />
        <main className="page">{props.children}</main>
      </div>
    </div>
  );
}
