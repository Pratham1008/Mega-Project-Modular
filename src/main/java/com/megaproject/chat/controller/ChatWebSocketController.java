package com.megaproject.chat.controller;

import com.megaproject.chat.dto.ChatMessageRequest;
import com.megaproject.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket STOMP message handlers.
 * REST endpoints live in ChatRestController.
 * Keeping them separate avoids confusion about routing and
 * makes each class testable in isolation.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest req,
                            @Header("simpSessionAttributes") Map<String, Object> attrs) {
        String senderId = (String) attrs.getOrDefault("userId", "unknown");
        String senderName = (String) attrs.getOrDefault("name", "User");
        String senderPhoto = (String) attrs.getOrDefault("photo", null);

        chatService.sendAndNotify(
                req.getConversationId(), senderId, senderName, senderPhoto, req.getContent());
    }

    @MessageMapping("/chat.seen")
    public void markSeen(@Payload Map<String, String> payload,
                         @Header("simpSessionAttributes") Map<String, Object> attrs) {
        String userId = (String) attrs.getOrDefault("userId", "");
        String conversationId = payload.get("conversationId");
        if (userId != null && conversationId != null) {
            chatService.markRead(conversationId, userId);
        }
    }
}
