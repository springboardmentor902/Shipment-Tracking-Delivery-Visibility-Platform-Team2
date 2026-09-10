package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.EtaPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtaPredictionRepository extends JpaRepository<EtaPrediction, Long> {

    Optional<EtaPrediction> findByShipment_Id(Long shipmentId);
}
