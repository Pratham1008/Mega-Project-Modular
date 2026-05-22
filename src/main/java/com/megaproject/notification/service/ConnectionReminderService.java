package com.megaproject.notification.service;

import com.megaproject.auth.repository.UserRepository;
import com.megaproject.chat.model.Connection;
import com.megaproject.chat.repository.ConnectionRepository;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

        byReceiver.forEach((receiverId, connections) -> {
            try {
                userRepo.findById(receiverId).ifPresent(user -> profileRepo.findByUserId(receiverId).ifPresent(profile -> {
                    List<Map<String, String>> requesters = connections.stream()
                            .map(c -> Map.of(
                                    "name", c.getRequesterName() != null ? c.getRequesterName() : "Someone",
                                    "photoUrl", c.getRequesterPhotoUrl() != null ? c.getRequesterPhotoUrl() : ""
                            ))
                            .collect(Collectors.toList());

                    emailService.sendConnectionReminderEmail(
                            user.getEmail(),
                            profile.getFullName(),
                            connections.size(),
                            requesters
                    );
                    log.info("Sent connection reminder to {} ({} pending)", user.getEmail(), connections.size());
                }));
            } catch (Exception e) {
                log.error("Failed to send connection reminder for receiverId {}: {}", receiverId, e.getMessage());
            }
        });
    }
}