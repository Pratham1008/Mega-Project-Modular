package com.megaproject.notification.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final Resend resend;
    private final boolean useResend;

    @Value("${app.mail.sender-email:noreply@prathameshcorporations.site}")
    private String senderEmail;

    public EmailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${resend.api-key:}") String resendApiKey) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;

        if (resendApiKey != null && !resendApiKey.isBlank()) {
            this.resend = new Resend(resendApiKey);
            this.useResend = true;
        } else {
            this.resend = null;
            this.useResend = false;
        }
    }

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

    @Async
    public void sendDonationReminderEmail(String toEmail, String alumniName, int phase) {
        Context ctx = new Context();
        ctx.setVariable("name", alumniName != null ? alumniName : "Alumnus");
        ctx.setVariable("phase", phase);
        String subject = phase == 1 ? "Support Current Students at KIT" : "Reminder: We Still Need Your Support at KIT";
        sendHtmlEmail(toEmail, subject, "DonationReminder", ctx);
    }

    private void sendHtmlEmail(String to, String subject, String template, Context ctx) {
        String htmlBody = templateEngine.process(template, ctx);

        if (useResend) {
            sendViaResend(to, subject, htmlBody);
        } else {
            sendViaSmtp(to, subject, htmlBody);
        }
    }

    private void sendViaResend(String to, String subject, String htmlBody) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(senderEmail)
                    .to(to)
                    .subject(subject)
                    .html(htmlBody)
                    .build();
            resend.emails().send(params);
        } catch (ResendException e) {
            log.error("Resend email failed → {} | {}", to, e.getMessage());
        }
    }

    private void sendViaSmtp(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Email failed → {} | {}", to, e.getMessage());
        }
    }
}
