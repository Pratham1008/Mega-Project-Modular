package com.megaproject.config;

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

        if (token != null && !token.isBlank()) {
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



            } catch (JwtException e) {
                log.warn("WS JWT validation failed: {}", e.getMessage());
                setAnonymous(attributes);
            }
        } else {
            setAnonymous(attributes);
        }

        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletReq) {
            String token = servletReq.getServletRequest().getParameter("token");
            if (token != null && !token.isBlank()) {
                return token;
            }
        }

        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6);
                    if (!token.isBlank()) {
                        return token;
                    }
                }
            }
        }

        return null;
    }

    private void setAnonymous(Map<String, Object> attributes) {
        attributes.put("userId", "anonymous");
        attributes.put("name", "Anonymous");
        attributes.put("role", "GUEST");
    }
}