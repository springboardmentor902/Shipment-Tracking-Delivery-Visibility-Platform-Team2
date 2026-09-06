type Props = {
  label: string;
  value: string | number;
  detail: string;
  tone: "blue" | "emerald" | "amber" | "rose";
  icon: string;
};

export default function SummaryCard({ label, value, detail, tone, icon }: Props) {
  return (
    <article className={`summary-card ${tone}`}>
      <div className="summary-card-top"><span className="summary-icon">{icon}</span><span className="summary-trend">Live</span></div>
      <p>{label}</p>
      <strong>{value}</strong>
      <small>{detail}</small>
    </article>
  );
}
