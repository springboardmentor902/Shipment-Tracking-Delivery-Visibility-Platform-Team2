type Props = {
  label: string;
  value: string | number;
  detail: string;
  tone: "blue" | "emerald" | "amber" | "rose";
  icon: string;
  onClick?: () => void;
};

export default function SummaryCard({ label, value, detail, tone, icon, onClick }: Props) {
  return (
    <button type="button" className={`summary-card ${tone}`} onClick={onClick} aria-label={`View ${label}`}>
      <div className="summary-card-top"><span className="summary-icon">{icon}</span><span className="summary-trend">Live</span></div>
      <p>{label}</p>
      <strong>{value}</strong>
      <small>{detail}</small>
    </button>
  );
}
