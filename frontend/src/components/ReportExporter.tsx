"use client";

import { useState } from "react";
import axios from "axios";

type Props = {
  token: string;
  apiUrl?: string;
};

type ReportFormat = "pdf" | "excel";
type ReportType = "shipments" | "deliveries" | "routes" | "delays";

export default function ReportExporter({
  token,
  apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8081",
}: Props) {
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [status, setStatus] = useState("");
  const [reportType, setReportType] = useState<ReportType>("shipments");
  const [exporting, setExporting] = useState<ReportFormat>();
  const [error, setError] = useState("");

  async function exportReport(format: ReportFormat) {
    setExporting(format);
    setError("");
    try {
      const response = await axios.get(
        `${apiUrl}/api/reports/${reportType}`,
        {
          responseType: "arraybuffer",
          headers: { Authorization: `Bearer ${token}` },
          params: {
            format,
            ...(startDate ? { startDate } : {}),
            ...(endDate ? { endDate } : {}),
            ...(status ? { status } : {}),
          },
        },
      );
      const blob = new Blob([response.data], { type: String(response.headers["content-type"] ?? "") });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${reportType}_report.${format === "pdf" ? "pdf" : "xlsx"}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (requestError) {
      if (axios.isAxiosError(requestError)) {
        setError(requestError.response?.data?.message ?? "Could not export report.");
      } else {
        setError("Could not export report.");
      }
    } finally {
      setExporting(undefined);
    }
  }

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">Reports and export</h2>
      <div className="grid gap-4 md:grid-cols-2">
        <label className="text-sm text-slate-600">
          Report type
          <select className="mt-1 w-full rounded border border-slate-300 p-2" value={reportType} onChange={(event) => setReportType(event.target.value as ReportType)}>
            <option value="shipments">Shipment report</option>
            <option value="deliveries">Delivery report</option>
            <option value="routes">Route performance report</option>
            <option value="delays">Delay analysis report</option>
          </select>
        </label>
        <label className="text-sm text-slate-600">
          Start date
          <input
            className="mt-1 w-full rounded border border-slate-300 p-2"
            type="date"
            value={startDate}
            onChange={(event) => setStartDate(event.target.value)}
          />
        </label>
      </div>
      <div className="mt-4 grid gap-4 md:grid-cols-3">
        <label className="text-sm text-slate-600">
          End date
          <input
            className="mt-1 w-full rounded border border-slate-300 p-2"
            type="date"
            value={endDate}
            onChange={(event) => setEndDate(event.target.value)}
          />
        </label>
        <label className="text-sm text-slate-600">
          Shipment status
          <select
            className="mt-1 w-full rounded border border-slate-300 p-2"
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="">All statuses</option>
            <option value="CREATED">Created</option>
            <option value="IN_TRANSIT">In transit</option>
            <option value="DELIVERED">Delivered</option>
            <option value="DELAYED">Delayed</option>
            <option value="FAILED_DELIVERY">Failed delivery</option>
          </select>
        </label>
      </div>
      {error && <p role="alert" className="mt-4 text-sm text-red-600">{error}</p>}
      <div className="mt-5 flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => exportReport("pdf")}
          disabled={Boolean(exporting)}
          className="rounded bg-blue-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
        >
          {exporting === "pdf" ? "Exporting..." : "Export PDF"}
        </button>
        <button
          type="button"
          onClick={() => exportReport("excel")}
          disabled={Boolean(exporting)}
          className="rounded bg-emerald-600 px-4 py-2 font-semibold text-white disabled:opacity-50"
        >
          {exporting === "excel" ? "Exporting..." : "Export Excel"}
        </button>
      </div>
    </section>
  );
}
