package com.megaproject.chat.repository;

import com.megaproject.chat.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    
    List<Conversation> findByParticipantIdsContainingOrderByLastMessageAtDesc(String userId);

    @Query("{ 'group': false, 'participantIds': { $all: [?0, ?1] } }")
    Optional<Conversation> findDmByBothParticipants(String userA, String userB);
}
