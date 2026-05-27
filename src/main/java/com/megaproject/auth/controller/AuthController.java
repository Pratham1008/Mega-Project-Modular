package com.megaproject.auth.controller;

import com.megaproject.auth.dto.*;
import com.megaproject.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        String userId = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "userId", userId,
                        "message", "Registration successful. Please verify your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required in request body");
        }
        return ResponseEntity.ok(authService.refreshToken(token));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody OtpVerificationRequest req) {
        authService.verifyEmail(req.email(), req.otp());
        return ResponseEntity.ok(Map.of("success", true, "message", "Email verified successfully"));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestParam String email) {
        authService.sendVerificationOtp(email);
        return ResponseEntity.ok(Map.of("success", true, "message", "OTP sent to " + email));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestParam String email) {
        authService.initiatePasswordReset(email);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset OTP sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successful"));
    }
}
