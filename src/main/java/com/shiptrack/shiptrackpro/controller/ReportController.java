package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/export")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/pdf")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<InputStreamResource> exportPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status
    ) {
        byte[] report = reportService.generatePdf(startDate, endDate, status);
        return download(report, "shipments_report.pdf", MediaType.APPLICATION_PDF);
    }

    @GetMapping("/excel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'ADMINISTRATOR')")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status
    ) {
        byte[] report = reportService.generateExcel(startDate, endDate, status);
        return download(
                report,
                "shipments_report.xlsx",
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private ResponseEntity<InputStreamResource> download(
            byte[] report,
            String filename,
            MediaType mediaType
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(report.length);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(new ByteArrayInputStream(report)));
    }
}
