package com.megaproject.auth.service;

import com.megaproject.auth.model.Otp;
import com.megaproject.auth.model.OtpPurpose;
import com.megaproject.auth.repository.OtpRepository;
import com.megaproject.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    private static final long OTP_EXPIRY_SECONDS = 600; // 10 minutes

    public void generateAndSend(String userId, String email, OtpPurpose purpose) {
        // Delete any existing OTP for this user + purpose
        otpRepository.deleteAllByUserIdAndPurpose(userId, purpose);

        String code = String.format("%06d", new SecureRandom().nextInt(999999));

        Otp otp = Otp.builder()
                .userId(userId)
                .code(code)
                .purpose(purpose)
                .expiryDate(Instant.now().plusSeconds(OTP_EXPIRY_SECONDS))
                .build();

        otpRepository.save(otp);

        // Send email directly (no Kafka - direct service call)
        if (purpose == OtpPurpose.VERIFICATION) {
            emailService.sendVerificationOtp(email, code);
        } else {
            emailService.sendPasswordResetOtp(email, code);
        }
    }

    public void verifyOtp(String userId, String code, OtpPurpose purpose) {
        Otp otp = otpRepository.findByUserIdAndCodeAndPurpose(userId, code, purpose)
                .orElseThrow(() -> new com.megaproject.auth.exception.AuthException("Invalid OTP code"));

        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new com.megaproject.auth.exception.AuthException("OTP has expired");
        }

        otpRepository.delete(otp); // consume the OTP
    }
}
