package com.megaproject.auth.service;

import com.megaproject.auth.dto.*;
import com.megaproject.auth.exception.AuthException;
import com.megaproject.auth.exception.EmailAlreadyInUseException;
import com.megaproject.auth.exception.ResourceNotFoundException;
import com.megaproject.auth.model.*;
import com.megaproject.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;

    public String register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new EmailAlreadyInUseException("Email already in use: " + req.email());
        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.USER)
                .verified(false)
                .build();
        User saved = userRepository.save(user);
        otpService.generateAndSend(saved.getId(), saved.getEmail(), OtpPurpose.VERIFICATION);
        return saved.getId();
    }

    public JwtResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new AuthException("Invalid credentials");
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refresh = refreshTokenService.create(user.getId());
        return new JwtResponse(accessToken, refresh.getToken(), user.getId(), user.getRole(), user.isVerified());
    }

    public JwtResponse refreshToken(String rawToken) {
        RefreshToken rt = refreshTokenService.verifyAndGet(rawToken);
        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String newAccess = jwtService.generateAccessToken(user);
        return new JwtResponse(newAccess, rt.getToken(), user.getId(), user.getRole(), user.isVerified());
    }

    public void verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.verifyOtp(user.getId(), code, OtpPurpose.VERIFICATION);
        user.setVerified(true);
        userRepository.save(user);
    }

    public void sendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.generateAndSend(user.getId(), email, OtpPurpose.VERIFICATION);
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.generateAndSend(user.getId(), email, OtpPurpose.PASSWORD_RESET);
    }

    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.verifyOtp(user.getId(), req.code(), OtpPurpose.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public void updateUserRole(String userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(role);
        userRepository.save(user);
    }
}
