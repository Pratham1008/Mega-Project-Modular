package com.megaproject.profile.service;

import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Lightweight lookup service for other modules to query profile data
 * without directly depending on ProfileRepository.
 */
@Service
@RequiredArgsConstructor
public class ProfileLookupService {

    private final ProfileRepository profileRepository;

    public Optional<String> getFullName(String userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> p.getFullName());
    }

    public Optional<String> getPhotoUrl(String userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> p.getPhotoUrl());
    }

    public record ProfileSummary(String fullName, String photoUrl) {}

    public Optional<ProfileSummary> getNameAndPhoto(String userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> new ProfileSummary(p.getFullName(), p.getPhotoUrl()));
    }
}
