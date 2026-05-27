package com.megaproject.chat.controller;

import com.megaproject.chat.model.Connection;
import com.megaproject.chat.model.Connection.ConnectionStatus;
import com.megaproject.chat.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;

    @PostMapping("/{receiverId}")
    public ResponseEntity<?> sendRequest(
            @PathVariable String receiverId,
            @AuthenticationPrincipal Jwt jwt) {
        String requesterId = jwt.getSubject();
        String requesterRole = jwt.getClaimAsString("role");
        
        Connection connection = connectionService.sendRequest(requesterId, requesterRole, receiverId);
        return ResponseEntity.status(HttpStatus.CREATED).body(connection);
    }

    @PatchMapping("/{connectionId}/accept")
    public ResponseEntity<?> accept(
            @PathVariable String connectionId,
            @AuthenticationPrincipal Jwt jwt) {
        Connection connection = connectionService.respondToRequest(connectionId, jwt.getSubject(), ConnectionStatus.ACCEPTED);
        return ResponseEntity.ok(connection);
    }

    @PatchMapping("/{connectionId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable String connectionId,
            @AuthenticationPrincipal Jwt jwt) {
        Connection connection = connectionService.respondToRequest(connectionId, jwt.getSubject(), ConnectionStatus.REJECTED);
        return ResponseEntity.ok(connection);
    }

    @GetMapping("/status/{otherUserId}")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> status = connectionService.getConnectionStatus(jwt.getSubject(), otherUserId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Connection>> pendingRequests(@AuthenticationPrincipal Jwt jwt) {
        List<Connection> pending = connectionService.getPendingRequests(jwt.getSubject());
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> pendingCount(@AuthenticationPrincipal Jwt jwt) {
        long count = connectionService.getPendingCount(jwt.getSubject());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/accepted")
    public ResponseEntity<List<Connection>> myConnections(@AuthenticationPrincipal Jwt jwt) {
        List<Connection> accepted = connectionService.getAcceptedConnections(jwt.getSubject());
        return ResponseEntity.ok(accepted);
    }

    @GetMapping("/check/{otherUserId}")
    public ResponseEntity<Map<String, Boolean>> isConnected(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal Jwt jwt) {
        boolean connected = connectionService.isConnected(jwt.getSubject(), otherUserId);
        return ResponseEntity.ok(Map.of("connected", connected));
    }
}
