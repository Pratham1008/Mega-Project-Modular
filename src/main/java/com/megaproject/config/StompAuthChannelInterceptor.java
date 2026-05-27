package com.megaproject.config;

import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Intercepts STOMP CONNECT frames to authenticate via Authorization header.
 * This supports SockJS fallback transports that can't send HTTP-level headers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final ProfileRepository profileRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    String userId = jwt.getSubject();

                    // Store in session attributes for use by @MessageMapping handlers
                    Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                    if (sessionAttrs != null) {
                        sessionAttrs.put("userId", userId);
                        sessionAttrs.put("role", jwt.getClaimAsString("role") != null
                                ? jwt.getClaimAsString("role") : "USER");

                        profileRepository.findByUserId(userId).ifPresentOrElse(
                                profile -> {
                                    sessionAttrs.put("name", profile.getFullName() != null
                                            ? profile.getFullName() : "User");
                                    if (profile.getPhotoUrl() != null) {
                                        sessionAttrs.put("photo", profile.getPhotoUrl());
                                    }
                                },
                                () -> {
                                    String email = jwt.getClaimAsString("email");
                                    sessionAttrs.put("name", email != null ? email.split("@")[0] : "User");
                                }
                        );
                        accessor.setSessionAttributes(sessionAttrs);
                    }
                } catch (JwtException e) {
                    log.warn("STOMP CONNECT rejected: Invalid JWT: {}", e.getMessage());
                    throw new org.springframework.messaging.MessageDeliveryException(
                            message, "Invalid authentication token");
                }
            }
        }

        return message;
    }
}
