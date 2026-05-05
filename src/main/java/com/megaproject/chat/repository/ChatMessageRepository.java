package com.megaproject.chat.repository;

import com.megaproject.chat.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** Messages for a conversation, newest first, paginated */
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(String conversationId, Pageable pageable);

    /** Count unread messages for a user in a conversation */
    long countByConversationIdAndReadFalseAndSenderIdNot(String conversationId, String userId);

    /** Mark all messages in a conversation as read (bulk) */
    List<ChatMessage> findByConversationIdAndReadFalseAndSenderIdNot(String conversationId, String userId);
}
