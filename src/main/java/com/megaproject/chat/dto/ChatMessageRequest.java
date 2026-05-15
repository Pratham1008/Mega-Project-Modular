package com.megaproject.chat.dto;

import lombok.Data;


@Data
public class ChatMessageRequest {
    private String conversationId;
    private String content;
}
