package com.loyalsuit.modules.auth.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.auth.application.event.PasswordResetCompletedEvent;
import com.loyalsuit.modules.auth.application.event.PasswordResetRequestedEvent;
import com.loyalsuit.modules.auth.domain.PasswordResetToken;
import com.loyalsuit.modules.auth.domain.port.PasswordResetTokenRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

/**
 * Forgot-password / reset flow. Security properties:
 * <ul>
 *   <li><b>No user enumeration</b> — a reset request behaves identically whether or
 *       not the email exists; the caller always gets the same generic response.</li>
 *   <li><b>Tokens are never stored in the clear</b> — only the SHA-256 hash is
 *       persisted, so a DB leak yields no usable tokens.</li>
 *   <li><b>Single-use and time-limited</b> — a token works once, before it expires,
 *       and prior tokens are invalidated when a new one is issued.</li>
 *   <li><b>Generic failures</b> — invalid/expired/used tokens all report the same
 *       message, leaking nothing about why.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String GENERIC_INVALID = "This reset link is invalid or has expired";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.security.password-reset-expiry-minutes:30}")
    private long expiryMinutes;

    /**
     * Issues a reset token and emails a link, if (and only if) an active account
     * exists. Always returns normally — callers must not branch on the outcome.
     */
    @Transactional
    public void requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(email)
                .filter(AppUser::isActive)
                .ifPresentOrElse(user -> {
                    // One active token per user: clear any outstanding ones first.
                    tokenRepository.deleteByUserId(user.getId());

                    String rawToken = generateToken();
                    PasswordResetToken token = new PasswordResetToken(
                            user.getId(),
                            sha256(rawToken),
                            Instant.now().plus(Duration.ofMinutes(expiryMinutes)));
                    tokenRepository.save(token);

                    String link = frontendUrl + "/reset-password?token=" + rawToken;
                    events.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), user.getFullName(), link));
                    auditService.recordSuccess(AuditAction.PASSWORD_RESET_REQUESTED,
                            AuditActor.of(user.getTenantId(), user.getId(), user.getEmail(), user.getRole().name()),
                            "USER", user.getId().toString(), "Reset link issued");
                    log.info("Password reset requested for email={}", email);
                }, () -> log.info("Password reset requested for unknown/inactive email={} (no-op)", email));
    }

    /**
     * Consumes a token and sets a new password. Invalid, expired, used, or
     * orphaned tokens all fail with the same generic error.
     */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> t.isUsable(Instant.now()))
                .orElseThrow(() -> new BusinessException(GENERIC_INVALID, HttpStatus.BAD_REQUEST));

        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(GENERIC_INVALID, HttpStatus.BAD_REQUEST));

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("New password must be different from your current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markUsed(Instant.now());
        tokenRepository.save(token);

        log.info("Password reset completed for email={}", user.getEmail());
        auditService.recordSuccess(AuditAction.PASSWORD_RESET_COMPLETED,
                AuditActor.of(user.getTenantId(), user.getId(), user.getEmail(), user.getRole().name()),
                "USER", user.getId().toString(), "Password reset via token");
        events.publishEvent(new PasswordResetCompletedEvent(user.getEmail(), user.getFullName()));
    }

    /** 256 bits of entropy, URL-safe, no padding. */
    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
