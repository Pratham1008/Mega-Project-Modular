package com.megaproject.auth.service;

import com.megaproject.auth.exception.TokenException;
import com.megaproject.auth.model.RefreshToken;
import com.megaproject.auth.repository.RefreshTokenRepository;
import com.megaproject.config.security.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    public RefreshToken create(String userId) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .expiryDate(Instant.now().plusSeconds(jwtConfig.getRefreshTokenExpirySeconds()))
                .build();
        return refreshTokenRepository.save(token);
    }

    public RefreshToken verifyAndGet(String rawToken) {
        RefreshToken rt = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));
        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            throw new TokenException("Refresh token has expired. Please login again.");
        }
        return rt;
    }

    public void revokeAllForUser(String userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}
