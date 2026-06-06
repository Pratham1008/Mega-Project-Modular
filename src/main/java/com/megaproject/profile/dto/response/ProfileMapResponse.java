package com.megaproject.profile.dto.response;

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
