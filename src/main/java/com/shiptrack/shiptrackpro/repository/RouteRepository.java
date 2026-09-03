package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    /** Kept with this exact signature for ETA and notification consumers. */
    @Query("select route from Route route where route.shipment.id = :shipmentId")
    Optional<Route> findByShipmentId(@Param("shipmentId") Long shipmentId);

    Optional<Route> findByShipment_Id(Long shipmentId);

    boolean existsByShipment_Id(Long shipmentId);
}
