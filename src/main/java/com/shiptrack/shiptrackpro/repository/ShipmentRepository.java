package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    List<Shipment> findByCreatedBy_Id(Long userId);

    List<Shipment> findByAssignedOperator_Id(Long userId);

    List<Shipment> findByStatusIn(Collection<String> statuses);
}
