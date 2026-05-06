package com.megaproject.chat.controller;

import com.megaproject.chat.model.Connection;
import com.megaproject.chat.model.Connection.ConnectionStatus;
import com.megaproject.chat.repository.ConnectionRepository;
import com.megaproject.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionRepository connectionRepo;
    private final ProfileRepository profileRepo;

    @PostMapping("/{receiverId}")
    public ResponseEntity<?> sendRequest(
            @PathVariable String receiverId,
            @AuthenticationPrincipal Jwt jwt) {

        String requesterId = jwt.getSubject();
        if (requesterId.equals(receiverId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot connect with yourself"));
        }

        Optional<Connection> existing = connectionRepo.findByUsers(requesterId, receiverId);
        if (existing.isPresent()) {
            Connection c = existing.get();
            if (c.getStatus() == ConnectionStatus.ACCEPTED) {
                return ResponseEntity.ok(Map.of("message", "Already connected", "connection", c));
            }
            if (c.getStatus() == ConnectionStatus.PENDING) {
                return ResponseEntity.ok(Map.of("message", "Request already pending", "connection", c));
            }
            c.setStatus(ConnectionStatus.PENDING);
            c.setRequesterId(requesterId);
            c.setReceiverId(receiverId);
            c.setRespondedAt(null);
            updateNames(c, requesterId, receiverId);
            return ResponseEntity.ok(connectionRepo.save(c));
        }

        Connection conn = Connection.builder()
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(ConnectionStatus.PENDING)
                .build();
        updateNames(conn, requesterId, receiverId);
        return ResponseEntity.status(HttpStatus.CREATED).body(connectionRepo.save(conn));
    }

    @PatchMapping("/{connectionId}/accept")
    public ResponseEntity<?> accept(
            @PathVariable String connectionId,
            @AuthenticationPrincipal Jwt jwt) {
        return respondToRequest(connectionId, jwt.getSubject(), ConnectionStatus.ACCEPTED);
    }

    @PatchMapping("/{connectionId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable String connectionId,
            @AuthenticationPrincipal Jwt jwt) {
        return respondToRequest(connectionId, jwt.getSubject(), ConnectionStatus.REJECTED);
    }

    @GetMapping("/status/{otherUserId}")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal Jwt jwt) {
        Optional<Connection> conn = connectionRepo.findByUsers(jwt.getSubject(), otherUserId);
        if (conn.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "NONE"));
        }
        Connection c = conn.get();
        Map<String, Object> result = new HashMap<>();
        result.put("status", c.getStatus().name());
        result.put("connectionId", c.getId());
        result.put("requesterId", c.getRequesterId());
        result.put("receiverId", c.getReceiverId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Connection>> pendingRequests(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                connectionRepo.findByReceiverIdAndStatus(jwt.getSubject(), ConnectionStatus.PENDING));
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> pendingCount(@AuthenticationPrincipal Jwt jwt) {
        long count = connectionRepo.findByReceiverIdAndStatus(jwt.getSubject(), ConnectionStatus.PENDING).size();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/accepted")
    public ResponseEntity<List<Connection>> myConnections(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(connectionRepo.findAllAcceptedForUser(jwt.getSubject()));
    }

    @GetMapping("/check/{otherUserId}")
    public ResponseEntity<Map<String, Boolean>> isConnected(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal Jwt jwt) {
        boolean connected = connectionRepo.findAcceptedConnection(jwt.getSubject(), otherUserId).isPresent();
        return ResponseEntity.ok(Map.of("connected", connected));
    }

    private ResponseEntity<?> respondToRequest(String connectionId, String userId, ConnectionStatus newStatus) {
        Optional<Connection> opt = connectionRepo.findById(connectionId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Connection c = opt.get();
        if (!c.getReceiverId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the receiver can respond to this request"));
        }
        if (c.getStatus() != ConnectionStatus.PENDING) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request already " + c.getStatus().name().toLowerCase()));
        }
        c.setStatus(newStatus);
        c.setRespondedAt(Instant.now());
        return ResponseEntity.ok(connectionRepo.save(c));
    }

    private void updateNames(Connection c, String requesterId, String receiverId) {
        profileRepo.findByUserId(requesterId).ifPresent(p -> {
            c.setRequesterName(p.getFullName());
            c.setRequesterPhotoUrl(p.getPhotoUrl());
        });
        profileRepo.findByUserId(receiverId).ifPresent(p -> {
            c.setReceiverName(p.getFullName());
            c.setReceiverPhotoUrl(p.getPhotoUrl());
        });
    }
}
