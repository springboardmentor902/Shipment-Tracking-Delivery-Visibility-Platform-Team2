"use client";

type Notification = { id: number; title: string; message: string; readAt?: string };

type Props = {
  userName?: string;
  role?: string;
  notifications: Notification[];
  showNotifications: boolean;
  onToggleNotifications: () => void;
  onMarkRead: (notification: Notification) => void;
  onOpenAuth: () => void;
};

export default function Navbar({
  userName,
  role,
  notifications,
  showNotifications,
  onToggleNotifications,
  onMarkRead,
  onOpenAuth,
}: Props) {
  const unreadCount = notifications.filter((item) => !item.readAt).length;
  return (
    <header className="topbar">
      <div className="brand-mark">
        <span className="brand-icon">S</span>
        <div>
          <strong>ShipTrack</strong>
          <span>Delivery visibility</span>
        </div>
      </div>
      <div className="topbar-actions">
        <button className="auth-button" onClick={onOpenAuth}>{userName ? "Account" : "Login / Register"}</button>
        <div className="notification-area">
          <button className="icon-button" onClick={onToggleNotifications} aria-label="Open notifications">
            <span aria-hidden="true">♢</span>
            {unreadCount > 0 && <span className="badge">{unreadCount}</span>}
          </button>
          {showNotifications && (
            <div className="notification-panel">
              <div className="panel-heading"><strong>Notifications</strong><span>{unreadCount} unread</span></div>
              {!notifications.length && <p className="empty-state">No notifications yet.</p>}
              {notifications.map((item) => (
                <button key={item.id} className={`notification-item ${item.readAt ? "read" : ""}`} onClick={() => onMarkRead(item)}>
                  <strong>{item.title}</strong>
                  <span>{item.message}</span>
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="profile">
          <span className="avatar">{(userName?.[0] ?? "G").toUpperCase()}</span>
          <div><strong>{userName ?? "Guest user"}</strong><span>{role ?? "Not signed in"}</span></div>
        </div>
      </div>
    </header>
  );
}
