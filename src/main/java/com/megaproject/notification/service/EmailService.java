package com.megaproject.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private static final String SENDER_EMAIL = "noreply@prathameshcorporations.site";

    @Async
    public void sendVerificationOtp(String toEmail, String otp) {
        Context ctx = new Context();
        ctx.setVariable("name", "Member");
        ctx.setVariable("userEmail", toEmail);
        ctx.setVariable("otpCode", otp);
        sendHtmlEmail(toEmail, "Action Required: Confirm Your Email", "WelcomeEmail", ctx);
    }

    @Async
    public void sendPasswordResetOtp(String toEmail, String otp) {
        Context ctx = new Context();
        ctx.setVariable("name", "Member");
        ctx.setVariable("userEmail", toEmail);
        ctx.setVariable("otpCode", otp);
        sendHtmlEmail(toEmail, "Action Required: Verify OTP to Reset Password", "ResetPassword", ctx);
    }

    private void sendHtmlEmail(String to, String subject, String template, Context ctx) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(SENDER_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, ctx), true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Email failed → {} | template: {} | {}", to, template, e.getMessage());
        }
    }
}
