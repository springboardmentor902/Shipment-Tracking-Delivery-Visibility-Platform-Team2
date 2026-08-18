package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.ShipmentRequest;
import com.shiptrack.shiptrackpro.dto.ShipmentResponse;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;

    @Override
    public ShipmentResponse createShipment(ShipmentRequest request) {

        Shipment shipment = new Shipment();

        shipment.setTrackingNumber(generateTrackingNumber());

        copyRequestToEntity(request, shipment);

        Shipment savedShipment = shipmentRepository.save(shipment);

        return mapToResponse(savedShipment);
    }

    @Override
    public List<ShipmentResponse> getAllShipments() {

        return shipmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ShipmentResponse getShipmentById(Long id) {

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + id
                ));

        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse getShipmentByTrackingNumber(
            String trackingNumber) {

        Shipment shipment = shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with tracking number: "
                                + trackingNumber
                ));

        return mapToResponse(shipment);
    }

    @Override
    public ShipmentResponse updateShipment(
            Long id,
            ShipmentRequest request) {

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + id
                ));

        copyRequestToEntity(request, shipment);

        Shipment updatedShipment =
                shipmentRepository.save(shipment);

        return mapToResponse(updatedShipment);
    }

    @Override
    public void deleteShipment(Long id) {

        if (!shipmentRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Shipment not found with id: " + id
            );
        }

        shipmentRepository.deleteById(id);
    }

    private void copyRequestToEntity(
            ShipmentRequest request,
            Shipment shipment) {

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

        shipment.setPackageDescription(
                request.getPackageDescription()
        );
        shipment.setWeight(request.getWeight());
        shipment.setLength(request.getLength());
        shipment.setWidth(request.getWidth());
        shipment.setHeight(request.getHeight());
        shipment.setQuantity(request.getQuantity());
        shipment.setDeclaredValue(request.getDeclaredValue());
        shipment.setFragile(request.getFragile());
    }

    private String generateTrackingNumber() {

        return "STP-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {

        return ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())

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

                .packageDescription(
                        shipment.getPackageDescription()
                )
                .weight(shipment.getWeight())
                .length(shipment.getLength())
                .width(shipment.getWidth())
                .height(shipment.getHeight())
                .quantity(shipment.getQuantity())
                .declaredValue(shipment.getDeclaredValue())
                .fragile(shipment.getFragile())

                .createdAt(shipment.getCreatedAt())
                .build();
    }
}