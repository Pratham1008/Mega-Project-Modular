package com.megaproject.auth.service;

import com.megaproject.auth.model.Role;
import com.megaproject.profile.event.ProfileTypeChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleSyncListener {

    private final AuthService authService;

    @Async
    @EventListener
    public void onProfileTypeChanged(ProfileTypeChangedEvent event) {
        try {
            Role role = Role.valueOf(event.newRole());
            authService.updateUserRole(event.userId(), role);
            log.debug("Synced role to {} for userId={}", role, event.userId());
        } catch (Exception e) {
            log.error("Failed to sync role for userId={}: {}", event.userId(), e.getMessage(), e);
        }
    }
}
