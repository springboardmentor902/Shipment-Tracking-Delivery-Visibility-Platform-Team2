import { ReactNode } from "react";

export default function PodVerificationQueue({ children }: { children: ReactNode }) {
  return <section className="module-view"><div className="module-heading"><p className="eyebrow">DELIVERY ASSURANCE</p><h2>Proof of delivery verification</h2><p>Review submitted delivery evidence and keep the verification queue moving.</p></div>{children}</section>;
}
