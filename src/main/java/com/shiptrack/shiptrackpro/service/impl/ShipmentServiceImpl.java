package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.ShipmentRequest;
import com.shiptrack.shiptrackpro.dto.ShipmentResponse;
import com.shiptrack.shiptrackpro.dto.PackageRequest;
import com.shiptrack.shiptrackpro.dto.PackageResponse;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.ShipmentPackage;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import com.shiptrack.shiptrackpro.service.CurrentUserService;
import com.shiptrack.shiptrackpro.service.ShipmentService;
import com.shiptrack.shiptrackpro.service.ShipmentAccessService;
import com.shiptrack.shiptrackpro.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ShipmentAccessService shipmentAccessService;
    private final NotificationService notificationService;

    @Override
    public ShipmentResponse createShipment(ShipmentRequest request) {

        User creator = currentUserService.getRequiredCurrentUser();
        if (!shipmentAccessService.isCustomerOrBusinessClient(creator)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only customers and business clients can create shipments");
        }

        Shipment shipment = new Shipment();

        shipment.setTrackingNumber(generateTrackingNumber());
        shipment.setCreatedBy(creator);

        copyRequestToEntity(request, shipment, true);

        Shipment savedShipment = shipmentRepository.save(shipment);
        notificationService.send("SHIPMENT_UPDATE", creator, savedShipment);

        return mapToResponse(savedShipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipments() {
        User currentUser = currentUserService.getRequiredCurrentUser();
        List<Shipment> shipments;
        if (shipmentAccessService.isAdministrator(currentUser)
                || shipmentAccessService.isSupportAgent(currentUser)) {
            shipments = shipmentRepository.findAll();
        } else if (shipmentAccessService.isLogisticsOperator(currentUser)) {
            shipments = shipmentRepository.findByAssignedOperator_Id(currentUser.getId());
        } else {
            shipments = shipmentRepository.findByCreatedBy_Id(currentUser.getId());
        }

        return shipments
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id) {

        Shipment shipment = findShipment(id);
        shipmentAccessService.requireCanViewShipment(shipment);

        return mapToResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingNumber(
            String trackingNumber) {

        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with tracking number: "
                                + trackingNumber
                ));

        shipmentAccessService.requireCanViewShipment(shipment);
        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse updateShipment(
            Long id,
            ShipmentRequest request) {

        Shipment shipment = findShipment(id);
        shipmentAccessService.requireCanManageShipment(shipment);

        copyRequestToEntity(request, shipment, false);

        Shipment updatedShipment =
                shipmentRepository.save(shipment);

        return mapToResponse(updatedShipment);
    }

    @Override
    public ShipmentResponse assignOperator(Long id, Long operatorId) {
        Shipment shipment = findShipment(id);
        User currentUser = currentUserService.getRequiredCurrentUser();
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Operator not found with id: " + operatorId));

        if (!shipmentAccessService.isLogisticsOperator(operator)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The selected user is not a logistics operator");
        }
        if (!shipmentAccessService.isAdministrator(currentUser)
                && (!shipmentAccessService.isLogisticsOperator(currentUser)
                || !currentUser.getId().equals(operatorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an administrator can assign a different operator");
        }

        shipment.setAssignedOperator(operator);
        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Override
    public void deleteShipment(Long id) {

        Shipment shipment = findShipment(id);
        shipmentAccessService.requireCanManageShipment(shipment);
        shipmentRepository.delete(shipment);
    }

    private void copyRequestToEntity(
            ShipmentRequest request,
            Shipment shipment,
            boolean creating) {

        shipment.setSenderName(request.getSenderName());
        shipment.setSenderPhone(request.getSenderPhone());
        shipment.setSenderAddress(request.getSenderAddress());

        shipment.setReceiverName(request.getReceiverName());
        shipment.setReceiverPhone(request.getReceiverPhone());
        shipment.setReceiverEmail(request.getReceiverEmail());
        shipment.setReceiverAddress(request.getReceiverAddress());

        shipment.setPickupAddress(request.getPickupAddress());
        shipment.setDeliveryAddress(request.getDeliveryAddress());

        shipment.setPriority(request.getPriority());

        List<ShipmentPackage> packages = packagesFromRequest(request);
        if (!packages.isEmpty()) {
            shipment.replacePackages(packages);
        } else if (creating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one package is required");
        }
    }

    private String generateTrackingNumber() {

        return "STP-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {

        List<PackageResponse> packages = shipment.getPackages().stream()
                .map(this::mapPackage)
                .toList();
        PackageResponse firstPackage = packages.isEmpty() ? null : packages.getFirst();

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .createdById(shipment.getCreatedBy() == null ? null : shipment.getCreatedBy().getId())
                .assignedOperatorId(shipment.getAssignedOperator() == null
                        ? null : shipment.getAssignedOperator().getId())

                .senderName(shipment.getSenderName())
                .senderPhone(shipment.getSenderPhone())
                .senderAddress(shipment.getSenderAddress())

                .receiverName(shipment.getReceiverName())
                .receiverPhone(shipment.getReceiverPhone())
                .receiverEmail(shipment.getReceiverEmail())
                .receiverAddress(shipment.getReceiverAddress())

                .pickupAddress(shipment.getPickupAddress())
                .deliveryAddress(shipment.getDeliveryAddress())

                .pickupLatitude(shipment.getPickupLatitude())
                .pickupLongitude(shipment.getPickupLongitude())
                .deliveryLatitude(shipment.getDeliveryLatitude())
                .deliveryLongitude(shipment.getDeliveryLongitude())

                .status(shipment.getStatus())
                .priority(shipment.getPriority())

                .estimatedDeliveryDate(
                        shipment.getEstimatedDeliveryDate()
                )
                .actualDeliveryDate(
                        shipment.getActualDeliveryDate()
                )

                .cancellationReason(
                        shipment.getCancellationReason()
                )

                // Preserve the former single-package response fields for old clients.
                .packageDescription(firstPackage == null ? null : firstPackage.getDescription())
                .weight(firstPackage == null || firstPackage.getWeight() == null
                        ? null : firstPackage.getWeight().doubleValue())
                .length(firstPackage == null || firstPackage.getLengthCm() == null
                        ? null : firstPackage.getLengthCm().doubleValue())
                .width(firstPackage == null || firstPackage.getWidthCm() == null
                        ? null : firstPackage.getWidthCm().doubleValue())
                .height(firstPackage == null || firstPackage.getHeightCm() == null
                        ? null : firstPackage.getHeightCm().doubleValue())
                .quantity(firstPackage == null ? null : firstPackage.getQuantity())
                .declaredValue(firstPackage == null || firstPackage.getDeclaredValue() == null
                        ? null : firstPackage.getDeclaredValue().doubleValue())
                .fragile(firstPackage == null ? null : firstPackage.getFragile())
                .packages(packages)

                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }

    private Shipment findShipment(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + id));
    }

    private List<ShipmentPackage> packagesFromRequest(ShipmentRequest request) {
        List<ShipmentPackage> packages = new ArrayList<>();
        if (request.getPackages() != null && !request.getPackages().isEmpty()) {
            request.getPackages().forEach(packageRequest ->
                    packages.add(packageFromRequest(packageRequest)));
            return packages;
        }

        if (request.getPackageDescription() != null && !request.getPackageDescription().isBlank()) {
            ShipmentPackage legacyPackage = ShipmentPackage.builder()
                    .description(request.getPackageDescription())
                    .weight(decimal(request.getWeight()))
                    .lengthCm(decimal(request.getLength()))
                    .widthCm(decimal(request.getWidth()))
                    .heightCm(decimal(request.getHeight()))
                    .quantity(request.getQuantity())
                    .declaredValue(decimal(request.getDeclaredValue()))
                    .fragile(request.getFragile())
                    .build();
            packages.add(legacyPackage);
        }
        return packages;
    }

    private ShipmentPackage packageFromRequest(PackageRequest request) {
        return ShipmentPackage.builder()
                .description(request.getDescription())
                .weight(request.getWeight())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .quantity(request.getQuantity())
                .declaredValue(request.getDeclaredValue())
                .fragile(request.getFragile())
                .build();
    }

    private PackageResponse mapPackage(ShipmentPackage shipmentPackage) {
        return PackageResponse.builder()
                .id(shipmentPackage.getId())
                .description(shipmentPackage.getDescription())
                .weight(shipmentPackage.getWeight())
                .lengthCm(shipmentPackage.getLengthCm())
                .widthCm(shipmentPackage.getWidthCm())
                .heightCm(shipmentPackage.getHeightCm())
                .quantity(shipmentPackage.getQuantity())
                .declaredValue(shipmentPackage.getDeclaredValue())
                .fragile(shipmentPackage.getFragile())
                .build();
    }

    private BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
