package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.TrackingEventRequest;
import com.shiptrack.shiptrackpro.dto.TrackingEventResponse;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.ShipmentStatus;
import com.shiptrack.shiptrackpro.entity.TrackingEvent;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.TrackingEventRepository;
import com.shiptrack.shiptrackpro.service.EtaPredictionService;
import com.shiptrack.shiptrackpro.service.NotificationService;
import com.shiptrack.shiptrackpro.service.ShipmentAccessService;
import com.shiptrack.shiptrackpro.service.TrackingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TrackingEventServiceImpl implements TrackingEventService {

    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentAccessService shipmentAccessService;
    private final EtaPredictionService etaPredictionService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TrackingEventResponse addTrackingEvent(
            Long shipmentId,
            TrackingEventRequest request
    ) {
        Shipment shipment = findShipment(shipmentId);
        shipmentAccessService.requireCanManageShipment(shipment);
        User currentUser = shipmentAccessService.currentUser();

        String status = normalizeShipmentStatus(request.getStatus());
        TrackingEvent event = TrackingEvent.builder()
                .shipment(shipment)
                .updatedBy(currentUser)
                .status(status)
                .location(blankToNull(request.getLocation()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .notes(blankToNull(request.getNotes()))
                .eventTimestamp(eventTimestampOrNow(request.getEventTimestamp()))
                .build();

        shipment.setStatus(status);
        TrackingEvent savedEvent = trackingEventRepository.save(event);

        if (shipment.getCreatedBy() != null) {
            notificationService.send("SHIPMENT_UPDATE", shipment.getCreatedBy(), shipment);
        }

        // A missing route intentionally results in no ETA yet, rather than a
        // failed tracking update. A valid route is recalculated in the same
        // transaction and is immediately visible to the caller afterward.
        etaPredictionService.recalculateAfterTrackingEvent(shipment.getId());

        return toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingEventResponse> getTrackingEvents(Long shipmentId) {
        Shipment shipment = findShipment(shipmentId);
        shipmentAccessService.requireCanViewShipment(shipment);

        return trackingEventRepository
                .findByShipment_IdOrderByEventTimestampAsc(shipmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + shipmentId
                ));
    }

    private String normalizeShipmentStatus(String suppliedStatus) {
        String normalized = suppliedStatus.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        try {
            return ShipmentStatus.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid shipment status: " + suppliedStatus
            );
        }
    }

    private LocalDateTime eventTimestampOrNow(LocalDateTime eventTimestamp) {
        return eventTimestamp == null ? LocalDateTime.now() : eventTimestamp;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TrackingEventResponse toResponse(TrackingEvent event) {
        Shipment shipment = event.getShipment();
        User updatedBy = event.getUpdatedBy();

        return TrackingEventResponse.builder()
                .id(event.getId())
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .status(event.getStatus())
                .location(event.getLocation())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .notes(event.getNotes())
                .eventTimestamp(event.getEventTimestamp())
                .updatedById(updatedBy == null ? null : updatedBy.getId())
                .updatedByName(updatedBy == null ? null : updatedBy.getFullName())
                .build();
    }
}
