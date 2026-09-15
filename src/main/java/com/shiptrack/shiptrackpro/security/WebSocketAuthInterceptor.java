package com.shiptrack.shiptrackpro.security;

import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import com.shiptrack.shiptrackpro.service.ShipmentAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Pattern LOCATION_TOPIC =
            Pattern.compile("^/topic/shipments/(\\d+)/location$");

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentAccessService shipmentAccessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new AccessDeniedException("Tracking updates must use the location API");
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthenticationCredentialsNotFoundException(
                    "A Bearer token is required for live tracking");
        }
        String token = header.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "The live-tracking token is invalid or expired");
        }
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "The user account no longer exists"));
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Connect before subscribing");
        }
        Matcher matcher = LOCATION_TOPIC.matcher(
                accessor.getDestination() == null ? "" : accessor.getDestination());
        if (!matcher.matches()) {
            throw new AccessDeniedException("Unsupported tracking channel");
        }

        User user = userRepository.findByEmailIgnoreCase(accessor.getUser().getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        Shipment shipment = shipmentRepository.findById(Long.valueOf(matcher.group(1)))
                .orElseThrow(() -> new AccessDeniedException("Shipment not found"));
        if (!shipmentAccessService.canViewShipment(user, shipment)) {
            throw new AccessDeniedException("You cannot subscribe to this shipment");
        }
    }
}
