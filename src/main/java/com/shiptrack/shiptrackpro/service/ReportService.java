package com.shiptrack.shiptrackpro.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
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
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ShipmentRepository shipmentRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public byte[] generatePdf(LocalDate startDate, LocalDate endDate, String status) {
        List<Shipment> shipments = findShipments(startDate, endDate, status);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(output);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.add(new Paragraph("Shipments Report").setBold().setFontSize(16));
            document.add(new Paragraph("Generated: " + LocalDateTime.now()));

            Table table = new Table(new float[]{1.4f, 1f, 1.5f, 1.5f, 2.2f, 1.5f});
            for (String header : new String[]{
                    "Tracking Number", "Status", "Sender", "Receiver", "Route", "ETA"
            }) {
                table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()));
            }
            for (Shipment shipment : shipments) {
                addPdfCell(table, shipment.getTrackingNumber());
                addPdfCell(table, shipment.getStatus());
                addPdfCell(table, shipment.getSenderName());
                addPdfCell(table, shipment.getReceiverName());
                addPdfCell(table, route(shipment));
                addPdfCell(table, value(shipment.getEstimatedDeliveryDate()));
            }
            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate PDF report", exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateExcel(LocalDate startDate, LocalDate endDate, String status) {
        List<Shipment> shipments = findShipments(startDate, endDate, status);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Shipments");
            String[] headers = {
                    "Tracking Number", "Status", "Sender", "Receiver", "Route", "ETA"
            };
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            for (int rowIndex = 0; rowIndex < shipments.size(); rowIndex++) {
                Shipment shipment = shipments.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                row.createCell(0).setCellValue(value(shipment.getTrackingNumber()));
                row.createCell(1).setCellValue(value(shipment.getStatus()));
                row.createCell(2).setCellValue(value(shipment.getSenderName()));
                row.createCell(3).setCellValue(value(shipment.getReceiverName()));
                row.createCell(4).setCellValue(route(shipment));
                row.createCell(5).setCellValue(value(shipment.getEstimatedDeliveryDate()));
            }
            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate Excel report", exception);
        }
    }

    private List<Shipment> findShipments(
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        User currentUser = currentUserService.getRequiredCurrentUser();
        List<Shipment> shipments = currentUserService.hasRole(currentUser, "ADMINISTRATOR")
                ? shipmentRepository.findAll()
                : shipmentRepository.findByCreatedBy_Id(currentUser.getId());
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate == null
                ? null
                : endDate.plusDays(1).atStartOfDay();
        String normalizedStatus = status == null || status.isBlank()
                ? null
                : status.trim().toUpperCase(Locale.ROOT);

        return shipments.stream()
                .filter(shipment -> start == null
                        || (shipment.getCreatedAt() != null
                        && !shipment.getCreatedAt().isBefore(start)))
                .filter(shipment -> endExclusive == null
                        || (shipment.getCreatedAt() != null
                        && shipment.getCreatedAt().isBefore(endExclusive)))
                .filter(shipment -> normalizedStatus == null
                        || normalizedStatus.equalsIgnoreCase(shipment.getStatus()))
                .toList();
    }

    private void addPdfCell(Table table, String value) {
        table.addCell(new Cell().add(new Paragraph(value(value))));
    }

    private String route(Shipment shipment) {
        return value(shipment.getPickupAddress()) + " -> "
                + value(shipment.getDeliveryAddress());
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
