package com.megaproject.chat.dto;

import lombok.Data;

/** Incoming STOMP message payload from the client */
@Data
public class ChatMessageRequest {
    private String conversationId;
    private String content;
}
