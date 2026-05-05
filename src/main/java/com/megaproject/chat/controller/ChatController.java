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

    // ── REST: Conversation management ──────────────────────────────────────

    /** Get or create a 1-to-1 DM conversation */
    @PostMapping("/dm/{otherUserId}")
    public ResponseEntity<Conversation> startDm(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal Jwt jwt) {
        String me = jwt.getSubject();
        return ResponseEntity.ok(chatService.getOrCreateDm(me, otherUserId));
    }

    /** Create a group conversation */
    @PostMapping("/group")
    public ResponseEntity<Conversation> createGroup(
            @RequestBody ConversationRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createGroup(jwt.getSubject(), req));
    }

    /** List my conversations */
    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> myConversations(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getMyConversations(jwt.getSubject()));
    }

    /** Get paginated message history */
    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<List<ChatMessage>> history(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {
        // Mark messages as read on fetch
        chatService.markRead(conversationId, jwt.getSubject());
        return ResponseEntity.ok(chatService.getMessages(conversationId, page, size));
    }

    /** Unread message counts per conversation */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> unread(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getUnreadCounts(jwt.getSubject()));
    }

    // ── WebSocket: Real-time messaging ────────────────────────────────────

    /**
     * Client sends to: /app/chat.send
     * Message is broadcast to: /topic/conversation/{conversationId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest req,
                            @Header("simpSessionAttributes") Map<String, Object> attrs) {
        // SimpSessionAttributes carries the JWT principal in sessionAttrs
        String senderId   = (String) attrs.getOrDefault("userId", "unknown");
        String senderName = (String) attrs.getOrDefault("name", "User");
        String senderPhoto = (String) attrs.getOrDefault("photo", null);

        ChatMessage saved = chatService.saveMessage(
                req.getConversationId(), senderId, senderName, senderPhoto, req.getContent());

        // Broadcast to all subscribers of this conversation topic
        broker.convertAndSend("/topic/conversation/" + req.getConversationId(), saved);
    }

    /**
     * Client sends to: /app/chat.seen
     * Marks messages as read
     */
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
