package com.megaproject.chat.repository;

import com.megaproject.chat.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /** All conversations a user participates in, newest first */
    List<Conversation> findByParticipantIdsContainingOrderByLastMessageAtDesc(String userId);

    /** Find an existing DM between exactly two users */
    Optional<Conversation> findByGroupFalseAndParticipantIdsContainingAndParticipantIdsContaining(
            String userA, String userB);
}
