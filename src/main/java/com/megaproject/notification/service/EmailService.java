package com.megaproject.notification.service;

import com.resend.Resend;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final Resend resend;
    private final boolean useResend;

    private final ReentrantLock resendLock = new ReentrantLock(true);
    private volatile long lastResendCallMillis = 0;
    private static final long MIN_GAP_MS = 600;
    private static final int MAX_RETRIES = 3;

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
    public void sendCredentialsEmail(String toEmail, String name, String password) {
        Context ctx = new Context();
        ctx.setVariable("name", name != null ? name : "Member");
        ctx.setVariable("userEmail", toEmail);
        ctx.setVariable("password", password);
        sendHtmlEmail(toEmail, "Welcome to KIT AlumniConnect — Your Account Credentials", "CredentialsEmail", ctx);
    }

    @Async
    public void sendDonationReminderEmail(String toEmail, String alumniName, int phase) {
        Context ctx = new Context();
        ctx.setVariable("name", alumniName != null ? alumniName : "Alumnus");
        ctx.setVariable("phase", phase);
        String subject = phase == 1 ? "Support Current Students at KIT" : "Reminder: We Still Need Your Support at KIT";
        sendHtmlEmail(toEmail, subject, "DonationReminder", ctx);
    }

    @Async
    public void sendConnectionReminderEmail(String toEmail, String name, int count, List<Map<String, String>> requesters) {
        Context ctx = new Context();
        ctx.setVariable("name", name != null ? name : "Member");
        ctx.setVariable("count", count);
        ctx.setVariable("requesters", requesters);
        String subject = count == 1
                ? "You have 1 pending connection request on KIT AlumniConnect"
                : "You have " + count + " pending connection requests on KIT AlumniConnect";
        sendHtmlEmail(toEmail, subject, "ConnectionReminder", ctx);
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
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(senderEmail)
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .build();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            resendLock.lock();
            try {
                long elapsed = System.currentTimeMillis() - lastResendCallMillis;
                if (elapsed < MIN_GAP_MS) {
                    Thread.sleep(MIN_GAP_MS - elapsed);
                }
                resend.emails().send(params);
                lastResendCallMillis = System.currentTimeMillis();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Resend email interrupted → {}", to);
                return;
            } catch (Exception e) {
                lastResendCallMillis = System.currentTimeMillis();
                if (e.getMessage() != null && e.getMessage().contains("429") && attempt < MAX_RETRIES) {
                    long backoff = attempt * 1500L;
                    log.warn("Resend rate-limited → {} | retry {}/{} in {}ms", to, attempt, MAX_RETRIES, backoff);
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    log.error("Resend email failed → {} | {}", to, e.getMessage());
                    return;
                }
            } finally {
                resendLock.unlock();
            }
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
