package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.ProofOfDelivery;
import com.shiptrack.shiptrackpro.entity.ProofOfDeliveryVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {

    Optional<ProofOfDelivery> findByShipment_Id(Long shipmentId);

    boolean existsByShipment_Id(Long shipmentId);

    Optional<ProofOfDelivery> findBySignatureUrlOrPhotoUrl(
            String signatureUrl,
            String photoUrl
    );

    List<ProofOfDelivery> findByVerificationStatusOrderByDeliveredAtAsc(
            ProofOfDeliveryVerificationStatus verificationStatus
    );
}
