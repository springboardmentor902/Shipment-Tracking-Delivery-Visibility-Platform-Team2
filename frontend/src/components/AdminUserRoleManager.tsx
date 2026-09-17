export type AdminUser = {
  id: number;
  fullName: string;
  email: string;
  role: string;
  status: string;
};

const assignableRoles = [
  { value: "CUSTOMER", label: "Customer" },
  { value: "LOGISTICS_OPERATOR", label: "Logistics Operator" },
  { value: "SUB_ADMINISTRATOR", label: "Sub-Administrator" },
  { value: "ADMINISTRATOR", label: "Administrator" },
];

type Props = {
  users: AdminUser[];
  loading: boolean;
  updatingUserId?: number;
  onRefresh: () => void;
  onRoleChange: (userId: number, newRole: string) => void;
};

export default function AdminUserRoleManager({ users, loading, updatingUserId, onRefresh, onRoleChange }: Props) {
  return (
    <section className="card user-role-manager">
      <div className="section-heading">
        <div>
          <p className="eyebrow">ACCOUNT MANAGEMENT</p>
          <h2>Assign account roles</h2>
          <p>Choose an account and assign its role. Only Administrators can make these changes.</p>
        </div>
        <button type="button" className="soft" onClick={onRefresh} disabled={loading}>{loading ? "Loading..." : "Refresh users"}</button>
      </div>
      {loading ? <p className="empty-state">Loading user accounts...</p> : <div className="role-assignment-list">
        {users.map((user) => (
          <article className="role-assignment" key={user.id}>
            <div><strong>{user.fullName}</strong><span>{user.email}</span></div>
            <select value={assignableRoles.some((role) => role.value === user.role) ? user.role : ""} onChange={(event) => onRoleChange(user.id, event.target.value)} disabled={updatingUserId === user.id} aria-label={`Change role for ${user.fullName}`}>
              {!assignableRoles.some((role) => role.value === user.role) && <option value="">{user.role.replaceAll("_", " ")}</option>}
              {assignableRoles.map((role) => <option key={role.value} value={role.value}>{role.label}</option>)}
            </select>
          </article>
        ))}
        {!users.length && <p className="empty-state">No user accounts found.</p>}
      </div>}
    </section>
  );
}
