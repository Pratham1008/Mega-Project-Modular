package com.megaproject.profile.dto.response;

/**
 * Typed DTO for the alumni map endpoint.
 * Replaces the raw Map<String, Object> that was built inline in ProfileController,
 * giving compile-time safety and a documented contract.
 */
public record ProfileMapResponse(
        String userId,
        String fullName,
        String photoUrl,
        String location,
        String department,
        String profileType,
        String company,
        int passingYear
) {}
