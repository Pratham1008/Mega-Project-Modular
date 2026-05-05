package com.megaproject.chat.repository;

import com.megaproject.chat.model.Connection;
import com.megaproject.chat.model.Connection.ConnectionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConnectionRepository extends MongoRepository<Connection, String> {

    /** Find existing connection between two users (either direction) */
    @Query("{ $or: [ { 'requesterId': ?0, 'receiverId': ?1 }, { 'requesterId': ?1, 'receiverId': ?0 } ] }")
    Optional<Connection> findByUsers(String userA, String userB);

    /** Check if two users are connected (ACCEPTED) */
    @Query("{ $or: [ { 'requesterId': ?0, 'receiverId': ?1 }, { 'requesterId': ?1, 'receiverId': ?0 } ], 'status': 'ACCEPTED' }")
    Optional<Connection> findAcceptedConnection(String userA, String userB);

    /** Pending requests I received */
    List<Connection> findByReceiverIdAndStatus(String receiverId, ConnectionStatus status);

    /** Pending requests I sent */
    List<Connection> findByRequesterIdAndStatus(String requesterId, ConnectionStatus status);

    /** All my accepted connections */
    @Query("{ $or: [ { 'requesterId': ?0 }, { 'receiverId': ?0 } ], 'status': 'ACCEPTED' }")
    List<Connection> findAllAcceptedForUser(String userId);
}
