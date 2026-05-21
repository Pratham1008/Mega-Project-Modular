package com.megaproject.notification.service;

import com.google.firebase.messaging.*;
import com.megaproject.notification.model.FcmToken;
import com.megaproject.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final FcmTokenRepository tokenRepo;
    private final FirebaseMessaging firebaseMessaging;

    public void registerToken(String userId, String token, String platform) {
        tokenRepo.findByToken(token).ifPresentOrElse(
                existing -> {
                    existing.setUserId(userId);
                    existing.setPlatform(platform);
                    existing.setUpdatedAt(Instant.now());
                    tokenRepo.save(existing);
                },
                () -> tokenRepo.save(FcmToken.builder()
                        .userId(userId)
                        .token(token)
                        .platform(platform)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
        );
    }

    public void unregisterToken(String token) {
        tokenRepo.deleteByToken(token);
    }

    @Async
    public void sendToUser(String userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) return;
        List<FcmToken> tokens = tokenRepo.findByUserId(userId);
        for (FcmToken t : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(t.getToken())
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putAllData(data != null ? data : Map.of())
                        .build();
                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    tokenRepo.delete(t);
                } else {
                    log.error("FCM send failed for user {}: {}", userId, e.getMessage());
                }
            }
        }
    }

    @Async
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) return;
        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data != null ? data : Map.of())
                    .build();
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.error("FCM topic send failed for {}: {}", topic, e.getMessage());
        }
    }
}
