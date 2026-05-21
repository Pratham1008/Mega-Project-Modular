package com.megaproject.notification.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "fcm_tokens")
@CompoundIndex(name = "userId_token", def = "{'userId': 1, 'token': 1}", unique = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FcmToken {
    @Id
    private String id;
    private String userId;
    private String token;
    private String platform;
    private Instant createdAt;
    private Instant updatedAt;
}
