package com.megaproject.notification.controller;

import com.megaproject.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {
        String userId = jwt.getSubject();
        String token = body.get("token");
        String platform = body.getOrDefault("platform", "ANDROID");
        fcmService.registerToken(userId, token, platform);
        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    @DeleteMapping("/unregister")
    public ResponseEntity<Map<String, String>> unregisterToken(
            @RequestBody Map<String, String> body) {
        String token = body.get("token");
        fcmService.unregisterToken(token);
        return ResponseEntity.ok(Map.of("status", "unregistered"));
    }
}
