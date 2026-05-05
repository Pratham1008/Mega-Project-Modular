package com.megaproject.chat.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private String senderId;
    private String senderName;
    private String senderPhotoUrl;

    private String content;

    @Builder.Default
    private boolean read = false;

    @CreatedDate
    private Instant sentAt;
}
