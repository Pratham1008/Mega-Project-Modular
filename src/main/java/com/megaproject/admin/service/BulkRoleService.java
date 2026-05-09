package com.megaproject.admin.service;

import com.megaproject.auth.model.Role;
import com.megaproject.auth.service.AuthService;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkRoleService {

    private final ProfileRepository profileRepository;
    private final AuthService authService;

    public BulkRoleResult changeRoles(List<String> userIds, String targetRole) {
        BulkRoleResult result = new BulkRoleResult();

        ProfileType profileType;
        Role authRole;

        try {
            profileType = ProfileType.valueOf(targetRole.toUpperCase());
            authRole    = Role.valueOf(targetRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + targetRole + ". Must be STUDENT or ALUMNI.");
        }

        if (profileType != ProfileType.STUDENT && profileType != ProfileType.ALUMNI) {
            throw new IllegalArgumentException("Bulk role change only supports STUDENT and ALUMNI.");
        }

        for (String userId : userIds) {
            try {
                ProfileDocument profile = profileRepository.findByUserId(userId)
                        .orElseThrow(() -> new RuntimeException("Profile not found for userId: " + userId));
                profile.setProfileType(profileType);
                profileRepository.save(profile);
                authService.updateUserRole(userId, authRole);
                result.updated++;
                log.info("Role changed userId={} to {}", userId, targetRole);
            } catch (Exception e) {
                result.failed++;
                result.errors.add(userId + ": " + e.getMessage());
                log.warn("Role change failed userId={}: {}", userId, e.getMessage());
            }
        }

        return result;
    }

    @Data
    public static class BulkRoleResult {
        private int updated = 0;
        private int failed  = 0;
        private List<String> errors = new ArrayList<>();
    }
}
