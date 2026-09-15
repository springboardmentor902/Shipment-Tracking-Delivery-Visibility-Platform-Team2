package com.shiptrack.shiptrackpro.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.shiptrack.shiptrackpro.entity.EtaPrediction;
import com.shiptrack.shiptrackpro.entity.ProofOfDelivery;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.EtaPredictionRepository;
import com.shiptrack.shiptrackpro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrackpro.repository.RouteRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ShipmentRepository shipmentRepository;
    private final RouteRepository routeRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final EtaPredictionRepository etaPredictionRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public byte[] generatePdf(LocalDate startDate, LocalDate endDate, String status) {
        return generatePdf("shipments", startDate, endDate, status);
    }

    @Transactional(readOnly = true)
    public byte[] generateExcel(LocalDate startDate, LocalDate endDate, String status) {
        return generateExcel("shipments", startDate, endDate, status);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(String type, LocalDate startDate, LocalDate endDate, String status) {
        ReportData data = reportData(type, startDate, endDate, status);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(new PdfDocument(new PdfWriter(output)));
            document.add(new Paragraph(data.title()).setBold().setFontSize(16));
            document.add(new Paragraph("Generated: " + LocalDateTime.now()));
            float[] widths = new float[data.headers().size()];
            Arrays.fill(widths, 1f);
            Table table = new Table(widths);
            data.headers().forEach(header ->
                    table.addHeaderCell(new Cell().add(new Paragraph(header).setBold())));
            data.rows().forEach(row -> row.forEach(value ->
                    table.addCell(new Cell().add(new Paragraph(value)))));
            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (IOException exception) {
            throw reportError("PDF", exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateExcel(String type, LocalDate startDate, LocalDate endDate, String status) {
        ReportData data = reportData(type, startDate, endDate, status);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(data.sheetName());
            Row header = sheet.createRow(0);
            for (int column = 0; column < data.headers().size(); column++) {
                header.createCell(column).setCellValue(data.headers().get(column));
            }
            for (int index = 0; index < data.rows().size(); index++) {
                Row row = sheet.createRow(index + 1);
                for (int column = 0; column < data.rows().get(index).size(); column++) {
                    row.createCell(column).setCellValue(data.rows().get(index).get(column));
                }
            }
            for (int column = 0; column < data.headers().size(); column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw reportError("Excel", exception);
        }
    }

    public String normalizeReportType(String reportType) {
        String normalized = reportType == null
                ? "shipments" : reportType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "shipment", "shipments" -> "shipments";
            case "delivery", "deliveries" -> "deliveries";
            case "route", "routes" -> "routes";
            case "delay", "delays" -> "delays";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Report type must be shipments, deliveries, routes, or delays");
        };
    }

    private ReportData reportData(
            String requestedType,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
        String type = normalizeReportType(requestedType);
        List<Shipment> shipments = findShipments(startDate, endDate, status);
        return switch (type) {
            case "deliveries" -> deliveryReport(shipments);
            case "routes" -> routeReport(shipments);
            case "delays" -> delayReport(shipments);
            default -> shipmentReport(shipments);
        };
    }

    private ReportData shipmentReport(List<Shipment> shipments) {
        List<List<String>> rows = shipments.stream()
                .map(shipment -> List.of(
                        value(shipment.getTrackingNumber()), value(shipment.getStatus()),
                        value(shipment.getSenderName()), value(shipment.getReceiverName()),
                        routeLabel(shipment), value(shipment.getCreatedAt()),
                        value(shipment.getEstimatedDeliveryDate())))
                .toList();
        return new ReportData("Shipment Report", "Shipments",
                List.of("Tracking", "Status", "Sender", "Receiver", "Route", "Created", "ETA"), rows);
    }

    private ReportData deliveryReport(List<Shipment> shipments) {
        List<List<String>> rows = new ArrayList<>();
        shipments.stream()
                .filter(shipment -> "DELIVERED".equalsIgnoreCase(shipment.getStatus()))
                .forEach(shipment -> {
                    ProofOfDelivery proof = proofOfDeliveryRepository
                            .findByShipment_Id(shipment.getId()).orElse(null);
                    rows.add(List.of(
                            value(shipment.getTrackingNumber()), value(shipment.getReceiverName()),
                            value(shipment.getEstimatedDeliveryDate()), value(shipment.getActualDeliveryDate()),
                            proof == null ? "NOT SUBMITTED" : proof.getVerificationStatus().name(),
                            proof == null ? "" : value(proof.getDeliveredToName())));
                });
        return new ReportData("Delivery Report", "Deliveries",
                List.of("Tracking", "Receiver", "Expected", "Delivered", "POD Status", "Received By"), rows);
    }

    private ReportData routeReport(List<Shipment> shipments) {
        List<List<String>> rows = new ArrayList<>();
        shipments.forEach(shipment -> routeRepository
                .findByShipment_IdAndIsCurrentTrue(shipment.getId())
                .ifPresent(route -> rows.add(List.of(
                        value(shipment.getTrackingNumber()), value(route.getOrigin()),
                        value(route.getDestination()), value(route.getDistanceKm()),
                        value(route.getEstimatedTimeMinutes()), value(route.getActualTimeMinutes()),
                        value(route.getTrafficCondition())))));
        return new ReportData("Route Performance Report", "Routes",
                List.of("Tracking", "Origin", "Destination", "Distance km", "Estimated min", "Actual min", "Traffic"), rows);
    }

    private ReportData delayReport(List<Shipment> shipments) {
        List<List<String>> rows = new ArrayList<>();
        shipments.forEach(shipment -> {
            EtaPrediction eta = etaPredictionRepository.findByShipment_Id(shipment.getId()).orElse(null);
            rows.add(List.of(
                    value(shipment.getTrackingNumber()), value(shipment.getStatus()),
                    eta == null ? "" : value(eta.getDelayRiskScore()),
                    eta == null ? "" : value(eta.getConfidenceScore()),
                    String.valueOf(delayDays(shipment)),
                    eta == null ? "No prediction" : value(eta.getFactors())));
        });
        return new ReportData("Delay Analysis Report", "Delays",
                List.of("Tracking", "Status", "Risk / 10", "Confidence %", "Delay days", "Factors"), rows);
    }

    private List<Shipment> findShipments(LocalDate startDate, LocalDate endDate, String status) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must not be after endDate");
        }
        User user = currentUserService.getRequiredCurrentUser();
        List<Shipment> shipments = currentUserService.hasRole(user, "ADMINISTRATOR")
                ? shipmentRepository.findAll()
                : shipmentRepository.findByCreatedBy_Id(user.getId());
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        String normalizedStatus = status == null || status.isBlank()
                ? null : status.trim().toUpperCase(Locale.ROOT);

        return shipments.stream()
                .filter(shipment -> start == null || (shipment.getCreatedAt() != null
                        && !shipment.getCreatedAt().isBefore(start)))
                .filter(shipment -> endExclusive == null || (shipment.getCreatedAt() != null
                        && shipment.getCreatedAt().isBefore(endExclusive)))
                .filter(shipment -> normalizedStatus == null
                        || normalizedStatus.equalsIgnoreCase(shipment.getStatus()))
                .toList();
    }

    private long delayDays(Shipment shipment) {
        if (shipment.getEstimatedDeliveryDate() == null) {
            return 0;
        }
        LocalDateTime end = shipment.getActualDeliveryDate() == null
                ? LocalDateTime.now() : shipment.getActualDeliveryDate();
        return Math.max(0, ChronoUnit.DAYS.between(shipment.getEstimatedDeliveryDate(), end));
    }

    private String routeLabel(Shipment shipment) {
        return value(shipment.getPickupAddress()) + " -> " + value(shipment.getDeliveryAddress());
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private ResponseStatusException reportError(String format, Exception exception) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not generate " + format + " report", exception);
    }

    private record ReportData(
            String title,
            String sheetName,
            List<String> headers,
            List<List<String>> rows
    ) {
    }
}
