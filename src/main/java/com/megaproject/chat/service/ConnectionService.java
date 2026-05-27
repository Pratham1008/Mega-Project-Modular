package com.megaproject.chat.service;

import com.megaproject.chat.model.Connection;
import com.megaproject.chat.model.Connection.ConnectionStatus;
import com.megaproject.chat.repository.ConnectionRepository;
import com.megaproject.notification.service.FcmService;
import com.megaproject.auth.repository.UserRepository;
import com.megaproject.auth.model.User;
import com.megaproject.auth.model.Role;
import com.megaproject.common.exception.ResourceNotFoundException;
import com.megaproject.profile.service.ProfileLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionService {

    private final ConnectionRepository connectionRepo;
    private final ProfileLookupService profileLookup;
    private final UserRepository userRepo;
    private final FcmService fcmService;

    public Connection sendRequest(String requesterId, String requesterRole, String receiverId) {
        if ("ADMIN".equals(requesterRole)) {
            throw new AccessDeniedException("Admin cannot use connections");
        }

        if (requesterId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot connect with yourself");
        }

        Optional<User> receiver = userRepo.findById(receiverId);
        if (receiver.isPresent() && receiver.get().getRole() == Role.ADMIN) {
            throw new AccessDeniedException("Cannot connect with an Admin");
        }

        Optional<Connection> existing = connectionRepo.findByUsers(requesterId, receiverId);
        if (existing.isPresent()) {
            Connection c = existing.get();
            if (c.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new IllegalStateException("Already connected");
            }
            if (c.getStatus() == ConnectionStatus.PENDING) {
                throw new IllegalStateException("Request already pending");
            }
            c.setStatus(ConnectionStatus.PENDING);
            c.setRequesterId(requesterId);
            c.setReceiverId(receiverId);
            c.setRespondedAt(null);
            updateNames(c, requesterId, receiverId);
            return connectionRepo.save(c);
        }

        Connection conn = Connection.builder()
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(ConnectionStatus.PENDING)
                .build();
        updateNames(conn, requesterId, receiverId);
        Connection saved = connectionRepo.save(conn);

        fcmService.sendToUser(
                receiverId,
                "New Connection Request",
                (saved.getRequesterName() != null ? saved.getRequesterName() : "Someone") + " wants to connect with you",
                Map.of("type", "CONNECTION_REQUEST", "connectionId", saved.getId(), "redirect", "/connections")
        );

        return saved;
    }

    public Connection respondToRequest(String connectionId, String userId, ConnectionStatus newStatus) {
        Connection c = connectionRepo.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found with ID: " + connectionId));

        if (!c.getReceiverId().equals(userId)) {
            throw new AccessDeniedException("Only the receiver can respond to this request");
        }
        if (c.getStatus() != ConnectionStatus.PENDING) {
            throw new IllegalStateException("Request already " + c.getStatus().name().toLowerCase());
        }

        c.setStatus(newStatus);
        c.setRespondedAt(Instant.now());
        Connection updated = connectionRepo.save(c);

        if (newStatus == ConnectionStatus.ACCEPTED) {
            fcmService.sendToUser(
                    updated.getRequesterId(),
                    "Connection Accepted",
                    (updated.getReceiverName() != null ? updated.getReceiverName() : "Someone") + " accepted your connection request",
                    Map.of("type", "CONNECTION_ACCEPTED", "connectionId", updated.getId(), "redirect", "/connections")
            );
        }

        return updated;
    }

    public Map<String, Object> getConnectionStatus(String userId, String otherUserId) {
        Optional<Connection> conn = connectionRepo.findByUsers(userId, otherUserId);
        if (conn.isEmpty()) {
            return Map.of("status", "NONE");
        }
        Connection c = conn.get();
        Map<String, Object> result = new HashMap<>();
        result.put("status", c.getStatus().name());
        result.put("connectionId", c.getId());
        result.put("requesterId", c.getRequesterId());
        result.put("receiverId", c.getReceiverId());
        return result;
    }

    public List<Connection> getPendingRequests(String userId) {
        return connectionRepo.findByReceiverIdAndStatus(userId, ConnectionStatus.PENDING);
    }

    public long getPendingCount(String userId) {
        return connectionRepo.countByReceiverIdAndStatus(userId, ConnectionStatus.PENDING);
    }

    public List<Connection> getAcceptedConnections(String userId) {
        return connectionRepo.findAllAcceptedForUser(userId);
    }

    public boolean isConnected(String userId, String otherUserId) {
        return connectionRepo.findAcceptedConnection(userId, otherUserId).isPresent();
    }

    private void updateNames(Connection c, String requesterId, String receiverId) {
        profileLookup.getNameAndPhoto(requesterId).ifPresent(p -> {
            c.setRequesterName(p.fullName());
            c.setRequesterPhotoUrl(p.photoUrl());
        });
        profileLookup.getNameAndPhoto(receiverId).ifPresent(p -> {
            c.setReceiverName(p.fullName());
            c.setReceiverPhotoUrl(p.photoUrl());
        });
    }
}
