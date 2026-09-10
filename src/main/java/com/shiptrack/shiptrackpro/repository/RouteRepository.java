package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    /** Kept with this exact signature for ETA and notification consumers. */
    @Query("select route from Route route where route.shipment.id = :shipmentId and route.isCurrent = true")
    Optional<Route> findByShipmentId(@Param("shipmentId") Long shipmentId);

    Optional<Route> findByShipment_IdAndIsCurrentTrue(Long shipmentId);

    List<Route> findByShipment_IdOrderByCreatedAtDesc(Long shipmentId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update Route route set route.isCurrent = false where route.shipment.id = :shipmentId and route.isCurrent = true")
    int markCurrentRoutesAsHistorical(@Param("shipmentId") Long shipmentId);
}
