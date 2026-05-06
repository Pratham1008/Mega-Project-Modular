package com.megaproject.chat.controller;

import com.megaproject.chat.dto.*;
import com.megaproject.chat.model.*;
import com.megaproject.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate broker;

    @PostMapping("/dm/{otherUserId}")
    public ResponseEntity<Conversation> startDm(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal Jwt jwt) {
        String me = jwt.getSubject();
        String role = jwt.getClaimAsString("role");
        if ("ADMIN".equals(role)) {
            return ResponseEntity.ok(chatService.getOrCreateDmBypassConnection(me, otherUserId));
        }
        return ResponseEntity.ok(chatService.getOrCreateDm(me, otherUserId));
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

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> unread(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getUnreadCounts(jwt.getSubject()));
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest req,
                            @Header("simpSessionAttributes") Map<String, Object> attrs) {
        String senderId = (String) attrs.getOrDefault("userId", "unknown");
        String senderName = (String) attrs.getOrDefault("name", "User");
        String senderPhoto = (String) attrs.getOrDefault("photo", null);

        ChatMessage saved = chatService.saveMessage(
                req.getConversationId(), senderId, senderName, senderPhoto, req.getContent());
        broker.convertAndSend("/topic/conversation/" + req.getConversationId(), saved);
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
