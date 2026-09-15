package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.AdminAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.BusinessClientAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.CustomerAnalyticsResponse;
import com.shiptrack.shiptrackpro.service.AnalyticsService;
import com.shiptrack.shiptrackpro.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<CustomerAnalyticsResponse> getCustomerDashboard() {
        return ResponseEntity.ok(analyticsService.getCustomerDashboard(
                analyticsServiceCurrentUserId()));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<CustomerAnalyticsResponse> getCustomerDashboard(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(analyticsService.getCustomerDashboard(customerId));
    }

    @GetMapping({"/business", "/business-client"})
    @PreAuthorize("hasRole('BUSINESS_CLIENT') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<BusinessClientAnalyticsResponse> getBusinessClientDashboard() {
        return ResponseEntity.ok(analyticsService.getBusinessClientDashboard(
                analyticsServiceCurrentUserId()));
    }

    @GetMapping("/business-client/{clientId}")
    @PreAuthorize("hasRole('BUSINESS_CLIENT') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<BusinessClientAnalyticsResponse> getBusinessClientDashboard(
            @PathVariable Long clientId) {
        return ResponseEntity.ok(analyticsService.getBusinessClientDashboard(clientId));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<AdminAnalyticsResponse> getAdminDashboard() {
        return ResponseEntity.ok(analyticsService.getAdminDashboard());
    }

    private Long analyticsServiceCurrentUserId() {
        return currentUserService.getRequiredCurrentUser().getId();
    }
}
