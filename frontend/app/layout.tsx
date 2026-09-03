import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "ShipTrack Pro",
  description: "Shipment tracking and delivery visibility platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
