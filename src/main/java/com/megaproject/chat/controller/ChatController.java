package com.megaproject.chat.controller;

import com.megaproject.chat.dto.*;
import com.megaproject.chat.model.*;
import com.megaproject.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/dm/{otherUserId}")
    public ResponseEntity<?> startDm(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal Jwt jwt) {
        String me = jwt.getSubject();
        String role = jwt.getClaimAsString("role");
        try {
            if ("ADMIN".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin cannot use chat functionality"));
            }
            return ResponseEntity.ok(chatService.getOrCreateDm(me, otherUserId));
        } catch (com.megaproject.common.exception.NotConnectedException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> myConversations(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getMyConversations(jwt.getSubject()));
    }

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<List<ChatMessage>> history(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {
        chatService.markRead(conversationId, jwt.getSubject());
        return ResponseEntity.ok(chatService.getMessages(conversationId, page, size));
    }

    // REST endpoint for sending messages (used by mobile app)
    @PostMapping("/messages/{conversationId}")
    public ResponseEntity<?> sendMessageRest(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String senderId = jwt.getSubject();
        String senderName = jwt.getClaimAsString("name");
        if (senderName == null || senderName.isBlank()) senderName = "User";
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }

        ChatMessage saved = chatService.sendAndNotify(conversationId, senderId, senderName, null, content);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> unread(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getUnreadCounts(jwt.getSubject()));
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest req,
                            @Header("simpSessionAttributes") Map<String, Object> attrs) {
        String senderId    = (String) attrs.getOrDefault("userId", "unknown");
        String senderName  = (String) attrs.getOrDefault("name", "User");
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
