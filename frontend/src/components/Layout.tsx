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
  unreadCount?: number;
  onOpenNotifications?: () => void;
};

export default function Layout(props: Props) {
  const role = props.role ?? "GUEST";
  const canCreate = role === "CUSTOMER" || role === "BUSINESS_CLIENT";
  const canOperate = role === "LOGISTICS_OPERATOR" || role === "ADMINISTRATOR";
  const canVerifyDelivery = role === "LOGISTICS_OPERATOR" || role === "SUB_ADMINISTRATOR" || role === "SUPPORT_AGENT" || role === "ADMINISTRATOR";
  return (
    <div className={`app-shell ${props.userName ? "signed-in" : ""}`}>
      {props.userName && <aside className="sidebar">
        <div className="sidebar-brand"><BrandLogo /><strong>ShipTrack Pro</strong></div>
        <nav>
          <button type="button" className={`nav-link ${props.activeTab === "overview" ? "active" : ""}`} onClick={() => props.onTabChange("overview")}><span>⌂</span> Overview</button>
          <button type="button" className={`nav-link ${props.activeTab === "tracking" ? "active" : ""}`} onClick={() => props.onTabChange("tracking")}><span>◈</span> Shipment Tracking</button>
          {(canCreate || canOperate) && <button type="button" className={`nav-link ${props.activeTab === "management" ? "active" : ""}`} onClick={() => props.onTabChange("management")}><span>✦</span> {role === "ADMINISTRATOR" ? "Shipment Management" : "Shipment Creation"}</button>}
          {canVerifyDelivery && <button type="button" className={`nav-link ${props.activeTab === "pod" ? "active" : ""}`} onClick={() => props.onTabChange("pod")}><span>✓</span> Verify Delivery</button>}
          <button type="button" className={`nav-link ${props.activeTab === "analytics" ? "active" : ""}`} onClick={() => props.onTabChange("analytics")}><span>▦</span> Analytics Dashboard</button>
          {role === "ADMINISTRATOR" && <button type="button" className={`nav-link ${props.activeTab === "team" ? "active" : ""}`} onClick={() => props.onTabChange("team")}><span>♙</span> Team Management</button>}
        </nav>
        <div className="sidebar-footer"><span className="status-dot" /> All systems operational</div>
      </aside>}
      <div className="content-shell">
        <Navbar userName={props.userName} onOpenAuth={props.onOpenAuth} onLogout={props.onLogout} unreadCount={props.unreadCount} onOpenNotifications={props.onOpenNotifications} />
        <main className="page">{props.children}</main>
      </div>
    </div>
  );
}
