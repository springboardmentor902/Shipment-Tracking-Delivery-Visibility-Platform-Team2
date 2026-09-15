package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.ShipmentRequest;
import com.shiptrack.shiptrackpro.dto.ShipmentResponse;
import com.shiptrack.shiptrackpro.dto.PublicTrackingResponse;

import java.util.List;

public interface ShipmentService {

    ShipmentResponse createShipment(ShipmentRequest request);

    List<ShipmentResponse> getAllShipments();

    ShipmentResponse getShipmentById(Long id);

    ShipmentResponse getShipmentByTrackingNumber(String trackingNumber);

    PublicTrackingResponse getPublicTracking(String trackingNumber);

    ShipmentResponse updateShipment(Long id, ShipmentRequest request);

    ShipmentResponse assignOperator(Long id, Long operatorId);

    void deleteShipment(Long id);
}
