package com.megaproject.donation.service;

import com.megaproject.notification.service.EmailService;
import com.megaproject.notification.service.FcmService;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.model.ProfileType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationCampaignService {

    private final MongoTemplate mongoTemplate;
    private final FcmService fcmService;
    private final EmailService emailService;

    private static final int EMAIL_BATCH_SIZE = 50;
    private static final long BATCH_DELAY_MS = 500L;

    @Scheduled(cron = "0 0 10 1 * ?")
    public void runPhase1Campaign() {
        log.info("Starting Phase 1 Donation Campaign...");
        runCampaignForPhaseAsync(1);
    }

    @Scheduled(cron = "0 0 10 8 * ?")
    public void runPhase2Campaign() {
        log.info("Starting Phase 2 Donation Campaign (Follow-up)...");
        runCampaignForPhaseAsync(2);
    }

    @Async
    public CompletableFuture<Void> runCampaignForPhaseAsync(int phase) {
        try {
            runCampaignForPhase(phase);
        } catch (Exception e) {
            log.error("Donation Campaign Phase {} failed", phase, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public void runCampaignForPhase(int phase) {
        String title = phase == 1 ? "Support Current Students \uD83C\uDF93" : "Reminder: We Still Need Your Support \u2764\uFE0F";
        String body = "Your contribution helps fund scholarships and improve campus facilities at KIT.";
        fcmService.sendToTopic("all", title, body, Map.of(
                "type", "DONATION_CAMPAIGN",
                "phase", String.valueOf(phase)
        ));

        Query query = new Query(Criteria.where("profileType").is(ProfileType.ALUMNI)
                .and("deleted").is(false)
                .and("email").ne(null));
        query.fields().include("email", "fullName");

        int emailsSent = 0;
        int batchCount = 0;
        try (var stream = mongoTemplate.stream(query, ProfileDocument.class)) {
            Iterator<ProfileDocument> cursor = stream.iterator();
            List<ProfileDocument> batch = new ArrayList<>(EMAIL_BATCH_SIZE);

            while (cursor.hasNext()) {
                batch.add(cursor.next());

                if (batch.size() >= EMAIL_BATCH_SIZE) {
                    emailsSent += sendEmailBatch(batch, phase);
                    batch.clear();
                    batchCount++;

                    try {
                        Thread.sleep(BATCH_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Email batch processing interrupted after {} batches", batchCount);
                        break;
                    }
                }
            }

            if (!batch.isEmpty()) {
                emailsSent += sendEmailBatch(batch, phase);
            }
        }

        log.info("Donation Campaign Phase {} completed: topic push sent, {} emails sent in {} batches.",
                phase, emailsSent, batchCount + 1);
    }

    private int sendEmailBatch(List<ProfileDocument> batch, int phase) {
        int sent = 0;
        for (ProfileDocument alumni : batch) {
            String email = alumni.getEmail();
            String name = alumni.getFullName();
            if (email != null && !email.isBlank()) {
                try {
                    emailService.sendDonationReminderEmail(email, name, phase);
                    sent++;
                } catch (Exception e) {
                    log.error("Failed to send donation email to {}", email, e);
                }
            }
        }
        return sent;
    }
}
