package com.megaproject.chat.service;

import com.megaproject.chat.dto.ConversationRequest;
import com.megaproject.chat.model.*;
import com.megaproject.chat.repository.*;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final ConnectionRepository connectionRepo;
    private final ProfileRepository profileRepo;

    // ── Conversations ─────────────────────────────────────────────────────

    /**
     * Get or create a DM conversation between two users.
     * REQUIRES: the two users must have an ACCEPTED connection.
     */
    public Conversation getOrCreateDm(String requesterId, String otherUserId) {
        // Gate: must be connected
        boolean connected = connectionRepo.findAcceptedConnection(requesterId, otherUserId).isPresent();
        if (!connected) {
            throw new IllegalStateException("Cannot start a conversation — users are not connected. Send a connection request first.");
        }

        // Check existing DM
        Optional<Conversation> existing = conversationRepo
                .findByGroupFalseAndParticipantIdsContainingAndParticipantIdsContaining(
                        requesterId, otherUserId);
        if (existing.isPresent()) return existing.get();

        Conversation dm = Conversation.builder()
                .group(false)
                .participantIds(List.of(requesterId, otherUserId))
                .lastMessageAt(Instant.now())
                .build();
        return conversationRepo.save(dm);
    }

    /** Create a group conversation */
    public Conversation createGroup(String creatorId, ConversationRequest req) {
        List<String> all = new ArrayList<>(req.getParticipantIds());
        if (!all.contains(creatorId)) all.add(0, creatorId);

        Conversation group = Conversation.builder()
                .group(true)
                .title(req.getTitle())
                .participantIds(all)
                .lastMessageAt(Instant.now())
                .build();
        return conversationRepo.save(group);
    }

    /** List conversations for current user */
    public List<Conversation> getMyConversations(String userId) {
        return conversationRepo
                .findByParticipantIdsContainingOrderByLastMessageAtDesc(userId);
    }

    // ── Messages ──────────────────────────────────────────────────────────

    /** Fetch paginated message history (oldest → newest) */
    public List<ChatMessage> getMessages(String conversationId, int page, int size) {
        return messageRepo.findByConversationIdOrderBySentAtAsc(
                conversationId, PageRequest.of(page, size));
    }

    /**
     * Persist a new message, update conversation's lastMessage,
     * and return the saved entity for broadcasting.
     */
    public ChatMessage saveMessage(String conversationId, String senderId,
                                   String senderName, String senderPhoto, String content) {
        ChatMessage msg = ChatMessage.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .senderName(senderName)
                .senderPhotoUrl(senderPhoto)
                .content(content)
                .sentAt(Instant.now())
                .build();
        ChatMessage saved = messageRepo.save(msg);

        // Update conversation snapshot
        conversationRepo.findById(conversationId).ifPresent(c -> {
            c.setLastMessage(content.length() > 60 ? content.substring(0, 60) + "…" : content);
            c.setLastMessageAt(Instant.now());
            conversationRepo.save(c);
        });
        return saved;
    }

    /** Mark all unread messages in a conversation as read for a specific user */
    public void markRead(String conversationId, String userId) {
        List<ChatMessage> unread = messageRepo
                .findByConversationIdAndReadFalseAndSenderIdNot(conversationId, userId);
        unread.forEach(m -> m.setRead(true));
        messageRepo.saveAll(unread);
    }

    /** Count unread messages across all conversations for a user */
    public Map<String, Long> getUnreadCounts(String userId) {
        List<Conversation> convs = conversationRepo
                .findByParticipantIdsContainingOrderByLastMessageAtDesc(userId);
        Map<String, Long> counts = new HashMap<>();
        for (Conversation c : convs) {
            long unread = messageRepo.countByConversationIdAndReadFalseAndSenderIdNot(c.getId(), userId);
            if (unread > 0) counts.put(c.getId(), unread);
        }
        return counts;
    }
}
