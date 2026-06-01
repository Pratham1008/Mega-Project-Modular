package com.megaproject.notification.service;

import com.megaproject.auth.model.User;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.chat.model.Connection;
import com.megaproject.chat.repository.ConnectionRepository;
import com.megaproject.profile.model.ProfileDocument;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionReminderService {

    private final ConnectionRepository connectionRepo;
    private final ProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 10 * * SUN")
    public void sendWeeklyConnectionReminders() {
        List<Connection> allPending = connectionRepo.findAllPendingConnections();
        if (allPending.isEmpty()) {
            log.info("Weekly connection reminder: no pending connections found.");
            return;
        }

        Map<String, List<Connection>> byReceiver = allPending.stream()
                .collect(Collectors.groupingBy(Connection::getReceiverId));

        Set<String> receiverIds = byReceiver.keySet();

        Map<String, User>            userMap    = userRepo.findAllById(receiverIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<String, ProfileDocument> profileMap = profileRepo.findAllByUserIdIn(List.copyOf(receiverIds))
                .stream().collect(Collectors.toMap(ProfileDocument::getUserId, p -> p));

        byReceiver.forEach((receiverId, connections) -> {
            User user = userMap.get(receiverId);
            ProfileDocument profile = profileMap.get(receiverId);
            if (user == null || profile == null) return;

            List<Map<String, String>> requesters = connections.stream()
                    .map(c -> Map.of(
                            "name",     c.getRequesterName()     != null ? c.getRequesterName()     : "Someone",
                            "photoUrl", c.getRequesterPhotoUrl() != null ? c.getRequesterPhotoUrl() : ""
                    ))
                    .collect(Collectors.toList());

            sendReminderAsync(user.getEmail(), profile.getFullName(),
                    connections.size(), requesters);
        });
    }

    @Async("taskExecutor")
    protected void sendReminderAsync(String email, String fullName,
                                     int count, List<Map<String, String>> requesters) {
        try {
            emailService.sendConnectionReminderEmail(email, fullName, count, requesters);
            log.info("Sent connection reminder to {} ({} pending)", email, count);
        } catch (Exception e) {
            log.error("Failed to send connection reminder to {}: {}", email, e.getMessage());
        }
    }
}