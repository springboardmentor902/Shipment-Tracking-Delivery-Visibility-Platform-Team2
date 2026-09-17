export type DashboardTab = "overview" | "tracking" | "management" | "pod" | "analytics" | "notifications" | "team";

const tabs: Array<{ id: DashboardTab; label: string; roles: string[] }> = [
  { id: "overview", label: "Overview", roles: ["CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR", "SUPPORT_AGENT"] },
  { id: "tracking", label: "Track Shipment", roles: ["CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR", "SUPPORT_AGENT"] },
  { id: "notifications", label: "Notifications", roles: ["CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR", "SUPPORT_AGENT"] },
  { id: "management", label: "Shipment Management", roles: ["CUSTOMER", "BUSINESS_CLIENT", "LOGISTICS_OPERATOR", "ADMINISTRATOR"] },
  { id: "pod", label: "Proof of Delivery", roles: ["LOGISTICS_OPERATOR", "ADMINISTRATOR", "SUPPORT_AGENT"] },
  { id: "analytics", label: "Analytics & Reports", roles: ["BUSINESS_CLIENT", "ADMINISTRATOR", "SUPPORT_AGENT"] },
  { id: "team", label: "Team Management", roles: ["ADMINISTRATOR"] },
];

export default function DashboardTabs({ activeTab, onChange, role = "CUSTOMER" }: { activeTab: DashboardTab; onChange: (tab: DashboardTab) => void; role?: string }) {
  const visibleTabs = tabs.filter((tab) => tab.roles.includes(role));
  return (
    <nav className="dashboard-tabs" aria-label="Dashboard sections">
      {visibleTabs.map((tab) => (
        <button key={tab.id} className={activeTab === tab.id ? "active" : ""} onClick={() => onChange(tab.id)}>
          {tab.id === "management" && role === "ADMINISTRATOR" ? "Admin control center" : tab.id === "management" && (role === "CUSTOMER" || role === "BUSINESS_CLIENT") ? "Create shipment" : tab.label}
        </button>
      ))}
    </nav>
  );
}
