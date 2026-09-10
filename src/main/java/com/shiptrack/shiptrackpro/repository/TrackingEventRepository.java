package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    List<TrackingEvent> findByShipment_IdOrderByEventTimestampAsc(Long shipmentId);

    Optional<TrackingEvent> findTopByShipment_IdOrderByEventTimestampDesc(Long shipmentId);
}
