package com.megaproject.notification.service;

import com.google.firebase.messaging.*;
import com.megaproject.notification.model.FcmToken;
import com.megaproject.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
                        .userId(userId).token(token).platform(platform)
                        .createdAt(Instant.now()).updatedAt(Instant.now()).build()));

        if (firebaseMessaging != null) {
            try {
                firebaseMessaging.subscribeToTopicAsync(List.of(token), "all");
            } catch (Exception e) {
                log.error("Failed to subscribe token to 'all' topic", e);
            }
        }
    }

    public void unregisterToken(String token) {
        tokenRepo.deleteByToken(token);
    }

    @Async
    public void sendToUser(String userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) return;
        List<FcmToken> tokens = tokenRepo.findByUserId(userId);
        if (tokens.isEmpty()) return;

        List<String> tokenStrings = tokens.stream().map(FcmToken::getToken).toList();
        for (int i = 0; i < tokenStrings.size(); i += 500) {
            List<String> batch = tokenStrings.subList(i, Math.min(i + 500, tokenStrings.size()));
            try {
                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putAllData(data != null ? data : Map.of())
                        .build();
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    List<String> toDelete = new ArrayList<>();
                    for (int j = 0; j < responses.size(); j++) {
                        SendResponse sr = responses.get(j);
                        if (!sr.isSuccessful()) {
                            FirebaseMessagingException ex = sr.getException();
                            if (ex != null && ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                                toDelete.add(batch.get(j));
                            }
                        }
                    }
                    if (!toDelete.isEmpty()) tokenRepo.deleteAllByTokenIn(toDelete);
                }
            } catch (FirebaseMessagingException e) {
                log.error("FCM multicast failed for user {}: {}", userId, e.getMessage());
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