package com.loyalsuit.common.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Sends transactional HTML email. Delivery is <em>best-effort</em>: if SMTP is not
 * configured or the send fails, this logs a warning and returns normally so the
 * calling business operation is never blocked by mail infrastructure. The
 * {@link JavaMailSender} is injected optionally — it only exists when
 * {@code spring.mail.host} is set, so the app (and tests) run fine without it.
 */
@Slf4j
@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;
    private final String fromName;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.from:no-reply@loyalsuit.local}") String fromAddress,
            @Value("${app.mail.from-name:LoyalSuit}") String fromName,
            @Value("${spring.mail.username:}") String smtpUsername) {
        this.mailSenderProvider = mailSenderProvider;
        // Gmail (and most providers) require the From to match the authenticated
        // user, so prefer the SMTP username when one is configured.
        this.fromAddress = StringUtils.hasText(smtpUsername) ? smtpUsername : fromAddress;
        this.fromName = fromName;
    }

    /**
     * Sends an HTML email. Returns true if handed off to the mail server, false if
     * mail is not configured or the send failed (already logged).
     */
    public boolean sendHtml(String to, String subject, String htmlBody) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Email skipped (SMTP not configured): subject='{}' to={}", subject, to);
            return false;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            sender.send(message);
            log.info("Email sent: subject='{}' to={}", subject, to);
            return true;
        } catch (UnsupportedEncodingException | org.springframework.mail.MailException | jakarta.mail.MessagingException e) {
            log.warn("Email send failed: subject='{}' to={} cause={}", subject, to, e.getMessage());
            return false;
        }
    }
}
