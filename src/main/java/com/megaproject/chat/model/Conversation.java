package com.megaproject.chat.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {

    @Id
    private String id;

    /** Display name — null for DMs (derived from participants), set for groups */
    private String title;

    @Builder.Default
    private boolean group = false;

    /** User IDs of all participants */
    @Builder.Default
    private List<String> participantIds = new ArrayList<>();

    private String lastMessage;
    private Instant lastMessageAt;

    @CreatedDate
    private Instant createdAt;
}
