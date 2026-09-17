package com.shiptrack.shiptrackpro;

import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.ShipmentPackage;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleBasedAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private User customer;
    private User otherCustomer;
    private User operator;

    @BeforeEach
    void setUp() {
        customer = saveUser("Customer One", "rbac.customer@example.com", "CUSTOMER");
        otherCustomer = saveUser("Customer Two", "rbac.other@example.com", "CUSTOMER");
        operator = saveUser("Operator", "rbac.operator@example.com", "LOGISTICS_OPERATOR");
        saveUser("Administrator", "rbac.admin@example.com", "ADMINISTRATOR");

        shipmentRepository.save(shipment("STP-RBAC-ONE", customer, operator));
        shipmentRepository.save(shipment("STP-RBAC-TWO", otherCustomer, null));
    }

    @Test
    void shipmentListsFollowOwnershipAndAssignment() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trackingNumber").value("STP-RBAC-ONE"));

        mockMvc.perform(get("/api/shipments")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/shipments")
                        .with(user("rbac.admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void customerCannotReadAnotherCustomersShipment() throws Exception {
        Shipment otherShipment = shipmentRepository.findByTrackingNumber("STP-RBAC-TWO").orElseThrow();

        mockMvc.perform(get("/api/shipments/{id}", otherShipment.getId())
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void operationalAndAdminEndpointsRejectCustomer() throws Exception {
        mockMvc.perform(post("/api/routes")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notification")
                        .with(user(customer.getEmail()).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/analytics/admin")
                        .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicRegistrationAllowsAllApplicationRoles() throws Exception {
        List<String> roles = List.of(
                "CUSTOMER",
                "BUSINESS_CLIENT",
                "LOGISTICS_OPERATOR",
                "SUPPORT_AGENT",
                "ADMINISTRATOR"
        );

        for (int index = 0; index < roles.size(); index++) {
            String role = roles.get(index);
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "fullName": "New User",
                                      "email": "new.user.%d@example.com",
                                      "password": "Password@123",
                                      "role": "%s"
                                    }
                                    """.formatted(index, role)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value(role));
        }
    }

    private User saveUser(String name, String email, String role) {
        return userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .password("test-password")
                .role(role)
                .status("ACTIVE")
                .build());
    }

    private Shipment shipment(String trackingNumber, User owner, User assignedOperator) {
        Shipment shipment = Shipment.builder()
                .trackingNumber(trackingNumber)
                .senderName(owner.getFullName())
                .receiverName("Receiver")
                .pickupAddress("Delhi")
                .deliveryAddress("Bengaluru")
                .status("CREATED")
                .priority("STANDARD")
                .createdBy(owner)
                .assignedOperator(assignedOperator)
                .build();
        shipment.replacePackages(List.of(ShipmentPackage.builder()
                .description("Documents")
                .quantity(1)
                .fragile(false)
                .build()));
        return shipment;
    }
}
