package com.megaproject.chat.service;

import com.megaproject.chat.model.*;
import com.megaproject.chat.repository.*;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.core.aggregation.*;
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
    private final MongoTemplate mongoTemplate;

    public Conversation getOrCreateDm(String requesterId, String otherUserId) {
        if (connectionRepo.findAcceptedConnection(requesterId, otherUserId).isEmpty())
            throw new NotConnectedException("Users are not connected. Send a connection request first.");
        return findOrBuildDm(requesterId, otherUserId);
    }

    public Conversation getOrCreateDmBypassConnection(String requesterId, String otherUserId) {
        return findOrBuildDm(requesterId, otherUserId);
    }

    public List<Conversation> getMyConversations(String userId) {
        return conversationRepo.findByParticipantIdsContainingOrderByLastMessageAtDesc(userId);
    }

    public Optional<Conversation> getConversationById(String conversationId) {
        return conversationRepo.findById(conversationId);
    }

    public List<ChatMessage> getMessages(String conversationId, int page, int size) {
        return messageRepo.findByConversationIdOrderBySentAtAsc(conversationId, PageRequest.of(page, size));
    }

    // OPTIMIZED: single updateOne on conversation instead of findById + save
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

        String preview = content.length() > 60 ? content.substring(0, 60) + "…" : content;
        Query q = new Query(Criteria.where("_id").is(conversationId));
        Update u = new Update()
                .set("lastMessage", preview)
                .set("lastMessageAt", saved.getSentAt());
        mongoTemplate.updateFirst(q, u, Conversation.class);

        return saved;
    }

    public void markRead(String conversationId, String userId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                .and("read").is(false).and("senderId").ne(userId));
        mongoTemplate.updateMulti(query, new Update().set("read", true), ChatMessage.class);
    }

    // OPTIMIZED: single aggregation instead of fetching all conversations first
    public Map<String, Long> getUnreadCounts(String userId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.lookup("conversations", "conversationId", "_id", "conv"),
                Aggregation.match(Criteria.where("read").is(false)
                        .and("senderId").ne(userId)
                        .and("conv.participantIds").is(userId)),
                Aggregation.group("conversationId").count().as("unreadCount")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "chat_messages", Map.class);
        Map<String, Long> counts = new HashMap<>();
        for (Map result : results.getMappedResults()) {
            counts.put((String) result.get("_id"), ((Number) result.get("unreadCount")).longValue());
        }
        return counts;
    }

    private Conversation findOrBuildDm(String requesterId, String otherUserId) {
        return conversationRepo.findDmByBothParticipants(requesterId, otherUserId)
                .orElseGet(() -> conversationRepo.save(Conversation.builder()
                        .group(false)
                        .participantIds(List.of(requesterId, otherUserId))
                        .lastMessageAt(Instant.now())
                        .build()));
    }

    public static class NotConnectedException extends RuntimeException {
        public NotConnectedException(String msg) { super(msg); }
    }
}