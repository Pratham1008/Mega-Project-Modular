package com.megaproject.chat.repository;

import com.megaproject.chat.model.Connection;
import com.megaproject.chat.model.Connection.ConnectionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends MongoRepository<Connection, String> {

    
    @Query("{ $or: [ { 'requesterId': ?0, 'receiverId': ?1 }, { 'requesterId': ?1, 'receiverId': ?0 } ] }")
    Optional<Connection> findByUsers(String userA, String userB);

    
    @Query("{ $or: [ { 'requesterId': ?0, 'receiverId': ?1 }, { 'requesterId': ?1, 'receiverId': ?0 } ], 'status': 'ACCEPTED' }")
    Optional<Connection> findAcceptedConnection(String userA, String userB);

    
    List<Connection> findByReceiverIdAndStatus(String receiverId, ConnectionStatus status);

    
    List<Connection> findByRequesterIdAndStatus(String requesterId, ConnectionStatus status);
    long countByReceiverIdAndStatus(String receiverId, ConnectionStatus status);
    
    @Query("{ $or: [ { 'requesterId': ?0 }, { 'receiverId': ?0 } ], 'status': 'ACCEPTED' }")
    List<Connection> findAllAcceptedForUser(String userId);
}
