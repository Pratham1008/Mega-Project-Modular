package com.megaproject.config.websocket;

import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;
    private final ProfileRepository profileRepository;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {

        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            log.warn("WS Handshake rejected: Missing WebSocket JWT token");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject();
            attributes.put("userId", userId);
            attributes.put("role", jwt.getClaimAsString("role") != null
                    ? jwt.getClaimAsString("role") : "USER");

            profileRepository.findByUserId(userId).ifPresentOrElse(
                    profile -> {
                        attributes.put("name", profile.getFullName() != null
                                ? profile.getFullName() : "User");
                        if (profile.getPhotoUrl() != null) {
                            attributes.put("photo", profile.getPhotoUrl());
                        }
                    },
                    () -> {
                        String email = jwt.getClaimAsString("email");
                        attributes.put("name", email != null ? email.split("@")[0] : "User");
                    }
            );

            return true;
        } catch (JwtException e) {
            log.warn("WS Handshake rejected: Invalid JWT: {}", e.getMessage());
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        // Priority 1: Check Authorization header (secure method)
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!token.isBlank()) {
                return token;
            }
        }

        // Priority 2: Check custom X-Auth-Token header (for SockJS/STOMP)
        String customHeader = request.getHeaders().getFirst("X-Auth-Token");
        if (customHeader != null && !customHeader.isBlank()) {
            return customHeader;
        }

        // Priority 3: Fallback to query param (deprecated — for old clients during migration)
        if (request instanceof ServletServerHttpRequest servletReq) {
            String token = servletReq.getServletRequest().getParameter("token");
            if (token != null && !token.isBlank()) {
                log.warn("WS token via query param is deprecated. Client: {}", 
                        servletReq.getServletRequest().getRemoteAddr());
                return token;
            }
        }

        return null;
    }
}