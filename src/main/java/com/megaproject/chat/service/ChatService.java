package com.megaproject.chat.service;

import com.megaproject.chat.dto.ConversationRequest;
import com.megaproject.chat.model.*;
import com.megaproject.chat.repository.*;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final ConnectionRepository connectionRepo;
    private final ProfileRepository profileRepo;

    public Conversation getOrCreateDm(String requesterId, String otherUserId) {
        boolean connected = connectionRepo.findAcceptedConnection(requesterId, otherUserId).isPresent();
        if (!connected) {
            throw new IllegalStateException("Users are not connected. Send a connection request first.");
        }

        Optional<Conversation> existing = conversationRepo
                .findByGroupFalseAndParticipantIdsContainingAndParticipantIdsContaining(requesterId, otherUserId);
        if (existing.isPresent()) return existing.get();

        Conversation dm = Conversation.builder()
                .group(false)
                .participantIds(List.of(requesterId, otherUserId))
                .lastMessageAt(Instant.now())
                .build();
        return conversationRepo.save(dm);
    }

    public Conversation getOrCreateDmBypassConnection(String requesterId, String otherUserId) {
        Optional<Conversation> existing = conversationRepo
                .findByGroupFalseAndParticipantIdsContainingAndParticipantIdsContaining(requesterId, otherUserId);
        if (existing.isPresent()) return existing.get();

        Conversation dm = Conversation.builder()
                .group(false)
                .participantIds(List.of(requesterId, otherUserId))
                .lastMessageAt(Instant.now())
                .build();
        return conversationRepo.save(dm);
    }

    public List<Conversation> getMyConversations(String userId) {
        return conversationRepo.findByParticipantIdsContainingOrderByLastMessageAtDesc(userId);
    }

    public List<ChatMessage> getMessages(String conversationId, int page, int size) {
        return messageRepo.findByConversationIdOrderBySentAtAsc(conversationId, PageRequest.of(page, size));
    }

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

        conversationRepo.findById(conversationId).ifPresent(c -> {
            c.setLastMessage(content.length() > 60 ? content.substring(0, 60) + "…" : content);
            c.setLastMessageAt(Instant.now());
            conversationRepo.save(c);
        });
        return saved;
    }

    public void markRead(String conversationId, String userId) {
        List<ChatMessage> unread = messageRepo.findByConversationIdAndReadFalseAndSenderIdNot(conversationId, userId);
        unread.forEach(m -> m.setRead(true));
        messageRepo.saveAll(unread);
    }

    public Map<String, Long> getUnreadCounts(String userId) {
        List<Conversation> convs = conversationRepo.findByParticipantIdsContainingOrderByLastMessageAtDesc(userId);
        Map<String, Long> counts = new HashMap<>();
        for (Conversation c : convs) {
            long unread = messageRepo.countByConversationIdAndReadFalseAndSenderIdNot(c.getId(), userId);
            if (unread > 0) counts.put(c.getId(), unread);
        }
        return counts;
    }
}
