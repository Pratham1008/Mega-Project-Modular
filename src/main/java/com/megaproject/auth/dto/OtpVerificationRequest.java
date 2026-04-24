package com.megaproject.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record OtpVerificationRequest(
        @NotBlank @Email String email,
        @NotBlank String otp
) {}
