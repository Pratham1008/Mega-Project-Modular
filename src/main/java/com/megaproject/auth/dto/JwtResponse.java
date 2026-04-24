package com.megaproject.auth.dto;
import com.megaproject.auth.model.Role;
public record JwtResponse(
        String accessToken,
        String refreshToken,
        String userId,
        Role role,
        boolean verified
) {}
