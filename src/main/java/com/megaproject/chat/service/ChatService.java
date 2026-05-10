package com.megaproject.chat.service;

import com.megaproject.chat.model.*;
import com.megaproject.chat.repository.*;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
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
        boolean connected = connectionRepo.findAcceptedConnection(requesterId, otherUserId).isPresent();
        if (!connected) {
            throw new NotConnectedException("Users are not connected. Send a connection request first.");
        }
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
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                .and("read").is(false)
                .and("senderId").ne(userId));
        Update update = new Update().set("read", true);
        mongoTemplate.updateMulti(query, update, ChatMessage.class);
    }

    public Map<String, Long> getUnreadCounts(String userId) {
        List<Conversation> convs = conversationRepo.findByParticipantIdsContainingOrderByLastMessageAtDesc(userId);
        if (convs.isEmpty()) return Collections.emptyMap();
        
        List<String> convIds = convs.stream().map(Conversation::getId).toList();
        
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("conversationId").in(convIds)
                        .and("read").is(false)
                        .and("senderId").ne(userId)),
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
        Optional<Conversation> existing = conversationRepo
                .findDmByBothParticipants(requesterId, otherUserId);
        if (existing.isPresent()) return existing.get();

        Conversation dm = Conversation.builder()
                .group(false)
                .participantIds(List.of(requesterId, otherUserId))
                .lastMessageAt(Instant.now())
                .build();
        return conversationRepo.save(dm);
    }

    public static class NotConnectedException extends RuntimeException {
        public NotConnectedException(String message) {
            super(message);
        }
    }
}
