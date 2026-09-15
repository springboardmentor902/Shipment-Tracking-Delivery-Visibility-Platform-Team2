"use client";

import BrandLogo from "./BrandLogo";

type Props = {
  userName?: string;
  role?: string;
  onOpenAuth: () => void;
  onLogout: () => void;
  unreadCount?: number;
  onOpenNotifications?: () => void;
};

export default function Navbar({
  userName,
  role,
  onOpenAuth,
  onLogout,
  unreadCount = 0,
  onOpenNotifications,
}: Props) {
  const roleLabel = role ? role.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase()) : "Not signed in";

  function toggleTheme() {
    const nextTheme = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = nextTheme;
    localStorage.setItem("shiptrack-theme", nextTheme);
    window.dispatchEvent(new CustomEvent("shiptrack-theme-change", { detail: nextTheme }));
  }

  return (
    <header className="topbar">
      <div className="brand-mark">
        <BrandLogo />
        <div>
          <strong>ShipTrack</strong>
          <span>Delivery visibility</span>
        </div>
      </div>
      <div className="topbar-actions">
        <button
          type="button"
          className="theme-toggle"
          onClick={toggleTheme}
          aria-label="Toggle light and dark theme"
          title="Toggle light and dark theme"
        >
          <span className="theme-moon" aria-hidden="true">☾</span>
          <span className="theme-sun" aria-hidden="true">☀</span>
          <span className="theme-label">Theme</span>
        </button>
        {userName && onOpenNotifications && (
          <button
            type="button"
            className="notification-bell"
            onClick={onOpenNotifications}
            aria-label={`${unreadCount} unread notifications`}
            title="Notifications"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>
            {unreadCount > 0 && <b>{unreadCount > 99 ? "99+" : unreadCount}</b>}
          </button>
        )}
        <button
          suppressHydrationWarning
          className="auth-button"
          onClick={userName ? onLogout : onOpenAuth}
        >
          {userName ? "Log out" : "Login / Register"}
        </button>
        <div className="profile">
          <span className="avatar">{(userName?.[0] ?? "G").toUpperCase()}</span>
          <div><strong>{userName ?? "Guest user"}</strong><span>{roleLabel}</span></div>
        </div>
      </div>
    </header>
  );
}
