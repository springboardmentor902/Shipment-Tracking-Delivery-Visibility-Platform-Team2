package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.ProofOfDeliveryRequest;
import com.shiptrack.shiptrackpro.dto.ProofOfDeliveryResponse;
import com.shiptrack.shiptrackpro.dto.VerifyProofOfDeliveryRequest;
import org.springframework.core.io.Resource;

public interface ProofOfDeliveryService {

    ProofOfDeliveryResponse submit(Long shipmentId, ProofOfDeliveryRequest request);

    ProofOfDeliveryResponse verify(Long shipmentId, VerifyProofOfDeliveryRequest request);

    ProofOfDeliveryResponse getForShipment(Long shipmentId);

    Resource loadAuthorizedFile(String storedFileName);
}
