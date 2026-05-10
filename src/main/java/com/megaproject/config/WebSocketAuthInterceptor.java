package com.megaproject.config;

import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
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
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;

        if (request instanceof ServletServerHttpRequest servletReq) {
            token = servletReq.getServletRequest().getParameter("token");
        }

        if (token == null || token.isBlank()) {
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        token = param.substring(6);
                        break;
                    }
                }
            }
        }

        if (token != null && !token.isBlank()) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                String userId = jwt.getSubject();
                attributes.put("userId", userId);
                attributes.put("role", jwt.getClaimAsString("role"));

                profileRepository.findByUserId(userId).ifPresentOrElse(
                    profile -> {
                        attributes.put("name", profile.getFullName() != null ? profile.getFullName() : "User");
                        attributes.put("photo", profile.getPhotoUrl());
                    },
                    () -> {
                        attributes.put("name", jwt.getClaimAsString("email") != null
                                ? jwt.getClaimAsString("email").split("@")[0] : "User");
                        attributes.put("photo", null);
                    }
                );

                log.debug("WS auth OK userId={}", userId);
            } catch (JwtException e) {
                log.warn("WS auth failed: {}", e.getMessage());
                attributes.put("userId", "anonymous");
                attributes.put("name", "Anonymous");
            }
        } else {
            attributes.put("userId", "anonymous");
            attributes.put("name", "Anonymous");
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
