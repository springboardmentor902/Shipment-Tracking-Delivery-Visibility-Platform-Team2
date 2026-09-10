package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Central object-level authorization rules for shipment-related modules. */
@Service
@RequiredArgsConstructor
public class ShipmentAccessService {

    private final CurrentUserService currentUserService;

    public User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }

    public void requireCanViewShipment(Shipment shipment) {
        if (!canViewShipment(currentUser(), shipment)) {
            throw forbidden();
        }
    }

    public boolean canViewShipment(User user, Shipment shipment) {
        if (isAdministrator(user) || isSupportAgent(user)) {
            return true;
        }
        if (isLogisticsOperator(user)) {
            return hasId(shipment.getAssignedOperator(), user.getId());
        }
        return isCustomerOrBusinessClient(user)
                && hasId(shipment.getCreatedBy(), user.getId());
    }

    public void requireCanManageShipment(Shipment shipment) {
        User user = currentUser();
        if (isAdministrator(user) || isSupportAgent(user)) {
            return;
        }
        if (isLogisticsOperator(user)
                && (shipment.getAssignedOperator() == null
                || hasId(shipment.getAssignedOperator(), user.getId()))) {
            return;
        }
        if (isCustomerOrBusinessClient(user)
                && hasId(shipment.getCreatedBy(), user.getId())
                && "CREATED".equalsIgnoreCase(shipment.getStatus())) {
            return;
        }
        throw forbidden();
    }

    public void requireCanSubmitProofOfDelivery(Shipment shipment) {
        User user = currentUser();
        if (!isLogisticsOperator(user)
                || !hasId(shipment.getAssignedOperator(), user.getId())) {
            throw forbidden();
        }
    }

    public void requireOperatorOrAdministrator() {
        User user = currentUser();
        if (!isLogisticsOperator(user) && !isAdministrator(user)) {
            throw forbidden();
        }
    }

    public void requireSupportAgentOrAdministrator() {
        User user = currentUser();
        if (!isSupportAgent(user) && !isAdministrator(user)) {
            throw forbidden();
        }
    }

    public boolean isAdministrator(User user) {
        return currentUserService.hasRole(user, "ADMINISTRATOR");
    }

    public boolean isSupportAgent(User user) {
        return currentUserService.hasRole(user, "SUPPORT_AGENT");
    }

    public boolean isLogisticsOperator(User user) {
        return currentUserService.hasRole(user, "LOGISTICS_OPERATOR");
    }

    public boolean isCustomerOrBusinessClient(User user) {
        return currentUserService.hasAnyRole(user, "CUSTOMER", "BUSINESS_CLIENT");
    }

    private boolean hasId(User candidate, Long expectedId) {
        return candidate != null && candidate.getId() != null
                && candidate.getId().equals(expectedId);
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not authorized to access this shipment");
    }
}
