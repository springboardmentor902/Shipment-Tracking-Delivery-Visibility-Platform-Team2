package com.shiptrack.shiptrackpro;

import com.shiptrack.shiptrackpro.entity.ProofOfDeliveryVerificationStatus;
import com.shiptrack.shiptrackpro.entity.Route;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.ShipmentPackage;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrackpro.repository.NotificationRepository;
import com.shiptrack.shiptrackpro.repository.EtaPredictionRepository;
import com.shiptrack.shiptrackpro.repository.RouteRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.TrackingEventRepository;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import com.shiptrack.shiptrackpro.security.JwtUtil;
import com.shiptrack.shiptrackpro.security.WebSocketAuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeatureModulesIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired RouteRepository routeRepository;
    @Autowired TrackingEventRepository trackingEventRepository;
    @Autowired ProofOfDeliveryRepository proofRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired WebSocketAuthInterceptor webSocketAuthInterceptor;
    @Autowired JwtUtil jwtUtil;
    @Autowired EtaPredictionRepository etaPredictionRepository;

    private User customer;
    private User business;
    private User operator;
    private User admin;
    private Shipment shipment;
    private Route route;

    @BeforeEach
    void setUp() {
        customer = saveUser("Feature Customer", "feature.customer@example.com", "CUSTOMER");
        business = saveUser("Feature Business", "feature.business@example.com", "BUSINESS_CLIENT");
        operator = saveUser("Feature Operator", "feature.operator@example.com", "LOGISTICS_OPERATOR");
        admin = saveUser("Feature Admin", "feature.admin@example.com", "ADMINISTRATOR");
        shipment = shipmentRepository.save(shipment("STP-FEATURE", customer, operator));
        shipmentRepository.save(shipment("STP-BUSINESS", business, null));
        route = routeRepository.save(Route.builder()
                .shipment(shipment)
                .driver(operator)
                .createdBy(operator)
                .origin("Delhi")
                .destination("Bengaluru")
                .distanceKm(BigDecimal.valueOf(2100))
                .estimatedTimeMinutes(1800)
                .trafficCondition("NORMAL")
                .isCurrent(true)
                .build());
    }

    @Test
    void driverLocationIsSavedAndAddedToTrackingHistory() throws Exception {
        mockMvc.perform(post("/api/route/{id}/location", route.getId())
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latitude":28.6139,"longitude":77.2090,"location":"New Delhi Hub"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipmentId").value(shipment.getId()))
                .andExpect(jsonPath("$.latitude").value(28.6139));

        Route updated = routeRepository.findById(route.getId()).orElseThrow();
        assertThat(updated.getLastKnownLatitude()).isEqualByComparingTo("28.6139");
        assertThat(updated.getLastLocationUpdatedAt()).isNotNull();
        assertThat(trackingEventRepository
                .findByShipment_IdOrderByEventTimestampAsc(shipment.getId())).hasSize(1);
        assertThat(etaPredictionRepository.findByShipment_Id(shipment.getId())).isPresent();
    }

    @Test
    void allReportTypesDownloadAsPdfAndExcel() throws Exception {
        for (String type : List.of("shipments", "deliveries", "routes", "delays")) {
            mockMvc.perform(get("/api/reports/{type}", type)
                            .param("format", "pdf")
                            .with(user(customer.getEmail()).roles("CUSTOMER")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.containsString(type + "_report.pdf")));

            mockMvc.perform(get("/api/reports/{type}", type)
                            .param("format", "excel")
                            .with(user(customer.getEmail()).roles("CUSTOMER")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
    }

    @Test
    void podStoresSignatureAndPhotoThenRecordsVerificationAudit() throws Exception {
        shipment.setStatus("OUT_FOR_DELIVERY");
        MockMultipartFile signature = new MockMultipartFile(
                "signature", "signature.png", "image/png", "signature".getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "delivery.png", "image/png", "photo".getBytes());

        mockMvc.perform(multipart("/api/pod/{shipmentId}", shipment.getId())
                        .file(signature)
                        .file(photo)
                        .param("deliveredToName", "Test Receiver")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.submittedById").value(operator.getId()));

        mockMvc.perform(patch("/api/pod/{shipmentId}/verify", shipment.getId())
                        .with(user(admin.getEmail()).roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationStatus":"VERIFIED","verificationNotes":"Photo and signature matched"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedById").value(admin.getId()))
                .andExpect(jsonPath("$.verificationNotes").value("Photo and signature matched"));

        assertThat(proofRepository.findByShipment_Id(shipment.getId()).orElseThrow()
                .getVerificationStatus()).isEqualTo(ProofOfDeliveryVerificationStatus.VERIFIED);
    }

    @Test
    void businessAnalyticsAliasReturnsOnlyBusinessData() throws Exception {
        mockMvc.perform(get("/api/analytics/business")
                        .with(user(business.getEmail()).roles("BUSINESS_CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalShipments").value(1))
                .andExpect(jsonPath("$.shipmentHistory[0].trackingNumber")
                        .value("STP-BUSINESS"));
    }

    @Test
    void statusPatchRecordsValidTransitionAndRejectsInvalidJump() throws Exception {
        mockMvc.perform(patch("/api/shipments/{id}/status", shipment.getId())
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PICKED_UP","location":"Delhi warehouse"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        mockMvc.perform(patch("/api/shipments/{id}/status", shipment.getId())
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED","location":"Bengaluru"}
                                """))
                .andExpect(status().isConflict());

        assertThat(trackingEventRepository
                .findByShipment_IdOrderByEventTimestampAsc(shipment.getId())).hasSize(1);
    }

    @Test
    void manualNotificationTargetsCustomerAndPreventsRecentDuplicate() throws Exception {
        String body = """
                {
                  "shipmentId": %d,
                  "type": "MANUAL",
                  "title": "Delivery desk",
                  "message": "Please keep an identity document ready."
                }
                """.formatted(shipment.getId());

        mockMvc.perform(post("/api/notification")
                        .with(user(admin.getEmail()).roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/notification")
                        .with(user(admin.getEmail()).roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("identity document", "photo ID")))
                .andExpect(status().isConflict());

        assertThat(notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(customer.getId())).hasSize(1);
        assertThat(notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(admin.getId())).isEmpty();
    }

    @Test
    void websocketRequiresJwtAndAllowsOnlyOwnedShipmentTopic() {
        StompHeaderAccessor connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setNativeHeader("Authorization", "Bearer "
                + jwtUtil.generateToken(customer.getEmail(), customer.getRole()));
        connect.setLeaveMutable(true);
        Message<byte[]> connectMessage = MessageBuilder.createMessage(
                new byte[0], connect.getMessageHeaders());
        Message<?> authenticated = webSocketAuthInterceptor.preSend(connectMessage, null);
        var principal = StompHeaderAccessor.wrap(authenticated).getUser();
        assertThat(principal).isNotNull();

        StompHeaderAccessor ownedSubscription = subscription(
                shipment.getId(), principal);
        assertThat(webSocketAuthInterceptor.preSend(
                MessageBuilder.createMessage(new byte[0], ownedSubscription.getMessageHeaders()),
                null)).isNotNull();

        Shipment businessShipment = shipmentRepository
                .findByTrackingNumber("STP-BUSINESS").orElseThrow();
        StompHeaderAccessor otherSubscription = subscription(
                businessShipment.getId(), principal);
        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(
                MessageBuilder.createMessage(new byte[0], otherSubscription.getMessageHeaders()),
                null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void routeIsSavedWhenMapProviderIsUnavailable() throws Exception {
        Shipment businessShipment = shipmentRepository
                .findByTrackingNumber("STP-BUSINESS").orElseThrow();

        mockMvc.perform(post("/api/routes")
                        .with(user(operator.getEmail()).roles("LOGISTICS_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shipmentId": %d, "trafficCondition": "NORMAL"}
                                """.formatted(businessShipment.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipmentId").value(businessShipment.getId()))
                .andExpect(jsonPath("$.distanceKm").doesNotExist())
                .andExpect(jsonPath("$.estimatedTimeMinutes").doesNotExist());

        Shipment assigned = shipmentRepository.findById(businessShipment.getId()).orElseThrow();
        assertThat(assigned.getAssignedOperator().getId()).isEqualTo(operator.getId());
        assertThat(routeRepository.findByShipment_IdAndIsCurrentTrue(businessShipment.getId()))
                .isPresent();
    }

    @Test
    void anonymousTrackingReturnsMilestonesWithoutPrivateCustomerData() throws Exception {
        mockMvc.perform(get("/api/shipments/public/tracking/{trackingNumber}",
                        shipment.getTrackingNumber().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("STP-FEATURE"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.receiverName").doesNotExist())
                .andExpect(jsonPath("$.createdById").doesNotExist());
    }

    private StompHeaderAccessor subscription(Long shipmentId, java.security.Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/shipments/" + shipmentId + "/location");
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return accessor;
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
        Shipment record = Shipment.builder()
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
        record.replacePackages(List.of(ShipmentPackage.builder()
                .description("Documents")
                .quantity(1)
                .fragile(false)
                .build()));
        return record;
    }
}
