package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.ProofOfDeliveryRequest;
import com.shiptrack.shiptrackpro.dto.ProofOfDeliveryResponse;
import com.shiptrack.shiptrackpro.dto.VerifyProofOfDeliveryRequest;
import com.shiptrack.shiptrackpro.entity.ProofOfDelivery;
import com.shiptrack.shiptrackpro.entity.ProofOfDeliveryVerificationStatus;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.service.FileStorageService;
import com.shiptrack.shiptrackpro.service.ProofOfDeliveryService;
import com.shiptrack.shiptrackpro.service.ShipmentAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProofOfDeliveryServiceImpl implements ProofOfDeliveryService {

    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentAccessService shipmentAccessService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProofOfDeliveryResponse submit(Long shipmentId, ProofOfDeliveryRequest request) {
        Shipment shipment = findShipment(shipmentId);
        shipmentAccessService.requireCanSubmitProofOfDelivery(shipment);

        if (proofOfDeliveryRepository.existsByShipment_Id(shipmentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Proof of delivery has already been submitted for this shipment");
        }
        if ((request.getSignature() == null || request.getSignature().isEmpty())
                && (request.getPhoto() == null || request.getPhoto().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload a signature or delivery photo as proof");
        }

        ProofOfDelivery proof = ProofOfDelivery.builder()
                .shipment(shipment)
                .signatureUrl(fileStorageService.store(request.getSignature()))
                .photoUrl(fileStorageService.store(request.getPhoto()))
                .deliveredToName(request.getDeliveredToName().trim())
                .deliveryNotes(blankToNull(request.getDeliveryNotes()))
                .verificationStatus(ProofOfDeliveryVerificationStatus.PENDING)
                .deliveredAt(LocalDateTime.now())
                .build();

        shipment.setStatus("DELIVERED");
        shipment.setActualDeliveryDate(proof.getDeliveredAt());
        shipmentRepository.save(shipment);
        return toResponse(proofOfDeliveryRepository.save(proof));
    }

    @Override
    @Transactional
    public ProofOfDeliveryResponse verify(Long shipmentId, VerifyProofOfDeliveryRequest request) {
        shipmentAccessService.requireSupportAgentOrAdministrator();
        ProofOfDelivery proof = findProof(shipmentId);
        if (request.getVerificationStatus() == ProofOfDeliveryVerificationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verification status must be VERIFIED or REJECTED");
        }

        User reviewer = shipmentAccessService.currentUser();
        proof.setVerificationStatus(request.getVerificationStatus());
        proof.setVerifiedBy(reviewer);
        return toResponse(proofOfDeliveryRepository.save(proof));
    }

    @Override
    @Transactional(readOnly = true)
    public ProofOfDeliveryResponse getForShipment(Long shipmentId) {
        ProofOfDelivery proof = findProof(shipmentId);
        shipmentAccessService.requireCanViewShipment(proof.getShipment());
        return toResponse(proof);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProofOfDeliveryResponse> getPendingProofs() {
        shipmentAccessService.requireSupportAgentOrAdministrator();
        return proofOfDeliveryRepository
                .findByVerificationStatusOrderByDeliveredAtAsc(ProofOfDeliveryVerificationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadAuthorizedFile(String storedFileName) {
        String fileUrl = "/api/pod/files/" + storedFileName;
        ProofOfDelivery proof = proofOfDeliveryRepository
                .findBySignatureUrlOrPhotoUrl(fileUrl, fileUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proof file not found"));
        shipmentAccessService.requireCanViewShipment(proof.getShipment());
        return fileStorageService.load(storedFileName);
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + shipmentId));
    }

    private ProofOfDelivery findProof(Long shipmentId) {
        return proofOfDeliveryRepository.findByShipment_Id(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Proof of delivery not found for shipment id: " + shipmentId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProofOfDeliveryResponse toResponse(ProofOfDelivery proof) {
        return ProofOfDeliveryResponse.builder()
                .id(proof.getId())
                .shipmentId(proof.getShipment().getId())
                .verifiedById(proof.getVerifiedBy() == null ? null : proof.getVerifiedBy().getId())
                .signatureUrl(proof.getSignatureUrl())
                .photoUrl(proof.getPhotoUrl())
                .deliveredToName(proof.getDeliveredToName())
                .deliveryNotes(proof.getDeliveryNotes())
                .verificationStatus(proof.getVerificationStatus().name())
                .deliveredAt(proof.getDeliveredAt())
                .build();
    }
}
