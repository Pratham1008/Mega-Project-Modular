package com.megaproject.donation.service;

import com.megaproject.notification.service.EmailService;
import com.megaproject.notification.service.FcmService;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationCampaignService {

    private final ProfileRepository profileRepository;
    private final FcmService fcmService;
    private final EmailService emailService;

    // Run on the 1st of every month at 10 AM (Phase 1)
    @Scheduled(cron = "0 0 10 1 * ?")
    public void runPhase1Campaign() {
        log.info("Starting Phase 1 Donation Campaign...");
        runCampaignForPhase(1);
    }

    // Run on the 8th of every month at 10 AM (Phase 2 - Follow-up)
    @Scheduled(cron = "0 0 10 8 * ?")
    public void runPhase2Campaign() {
        log.info("Starting Phase 2 Donation Campaign (Follow-up)...");
        runCampaignForPhase(2);
    }

    public void runCampaignForPhase(int phase) {
        List<ProfileDocument> alumniList = profileRepository.findByProfileTypeAndDeletedFalse(ProfileType.ALUMNI);
        
        for (ProfileDocument alumni : alumniList) {
            String userId = alumni.getUserId();
            String email = alumni.getEmail();
            String name = alumni.getFullName();

            // 1. Send FCM Push Notification
            String title = phase == 1 ? "Support Current Students \uD83C\uDF93" : "Reminder: We Still Need Your Support \u2764\uFE0F";
            String body = "Your contribution helps fund scholarships and improve campus facilities at KIT.";
            fcmService.sendToUser(userId, title, body, Map.of(
                    "type", "DONATION_CAMPAIGN",
                    "phase", String.valueOf(phase)
            ));

            // 2. Send Email
            if (email != null && !email.isBlank()) {
                emailService.sendDonationReminderEmail(email, name, phase);
            }
        }
        log.info("Donation Campaign Phase {} completed for {} alumni.", phase, alumniList.size());
    }
}
