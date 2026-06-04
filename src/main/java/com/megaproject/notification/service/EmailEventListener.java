package com.megaproject.notification.service;

import com.megaproject.profile.event.FacultyProvisionedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void onFacultyProvisioned(FacultyProvisionedEvent event) {
        try {
            emailService.sendCredentialsEmail(event.email(), event.fullName(), event.generatedPassword());
            log.info("Credentials email queued for {}", event.email());
        } catch (Exception e) {
            log.error("Failed to send credentials email to {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
