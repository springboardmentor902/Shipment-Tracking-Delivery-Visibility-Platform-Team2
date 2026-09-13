type Props = {
  className?: string;
};

export default function BrandLogo({ className = "" }: Props) {
  return (
    <svg
      className={`brand-logo ${className}`.trim()}
      viewBox="0 0 40 40"
      aria-hidden="true"
    >
      <rect width="40" height="40" rx="11" fill="#16324f" />
      <circle cx="11.5" cy="28.5" r="3.5" fill="#ffffff" />
      <path
        d="M15 28.5c0-6.5 10.5-4.5 10.5-12"
        fill="none"
        stroke="#55a6d9"
        strokeWidth="3.2"
        strokeLinecap="round"
      />
      <path
        d="M22.5 12.5h8v8"
        fill="none"
        stroke="#ffffff"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="m30.5 12.5-7.25 7.25"
        fill="none"
        stroke="#ffffff"
        strokeWidth="3.2"
        strokeLinecap="round"
      />
    </svg>
  );
}
