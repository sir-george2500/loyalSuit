package com.loyalsuit.modules.auth.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.security.TotpService;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.auth.application.dto.TwoFactorEnableResponse;
import com.loyalsuit.modules.auth.application.dto.TwoFactorSetupResponse;
import com.loyalsuit.modules.auth.domain.TwoFactorRecoveryCode;
import com.loyalsuit.modules.auth.domain.port.TwoFactorRecoveryCodeRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TOTP two-factor auth: enrolment, confirmation, disabling, and verifying the second factor at
 * login (an authenticator code or a single-use recovery code). The secret is stored on the
 * user; recovery codes are stored only as hashes and consumed when used.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final String ISSUER = "LoyalSuit";
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final AppUserRepository userRepository;
    private final TwoFactorRecoveryCodeRepository recoveryCodeRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    /** Start enrolment: mint a secret (2FA stays off until confirmed) and return it + the QR URI. */
    @Transactional
    public TwoFactorSetupResponse setup(UUID userId) {
        AppUser user = load(userId);
        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("Two-factor authentication is already enabled", HttpStatus.CONFLICT);
        }
        String secret = totpService.generateSecret();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);
        return new TwoFactorSetupResponse(secret, totpService.otpauthUri(ISSUER, user.getEmail(), secret));
    }

    /** Confirm enrolment with a code, turn 2FA on, and issue fresh recovery codes (shown once). */
    @Transactional
    public TwoFactorEnableResponse enable(UUID userId, String code) {
        AppUser user = load(userId);
        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("Two-factor authentication is already enabled", HttpStatus.CONFLICT);
        }
        if (user.getTwoFactorSecret() == null) {
            throw new BusinessException("Start two-factor setup first", HttpStatus.BAD_REQUEST);
        }
        if (!totpService.verify(user.getTwoFactorSecret(), code)) {
            throw new BusinessException("That code isn't valid — check your authenticator and try again",
                    HttpStatus.UNAUTHORIZED);
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        List<String> plaintext = issueRecoveryCodes(userId);
        log.info("2FA enabled for email={}", user.getEmail());
        auditService.recordSuccess(AuditAction.TWO_FACTOR_ENABLED, actorOf(user),
                "USER", user.getId().toString(), "Two-factor authentication enabled");
        return new TwoFactorEnableResponse(plaintext);
    }

    /** Turn 2FA off after re-authenticating with the password; clears the secret and codes. */
    @Transactional
    public void disable(UUID userId, String password) {
        AppUser user = load(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("Password is incorrect", HttpStatus.UNAUTHORIZED);
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        recoveryCodeRepository.deleteByUserId(userId);
        log.info("2FA disabled for email={}", user.getEmail());
        auditService.recordSuccess(AuditAction.TWO_FACTOR_DISABLED, actorOf(user),
                "USER", user.getId().toString(), "Two-factor authentication disabled");
    }

    /**
     * Verifies the second factor at login: an authenticator code, or a single-use recovery code
     * (which is consumed on success). Returns whether verification passed.
     */
    @Transactional
    public boolean verifySecondFactor(AppUser user, String code) {
        if (totpService.verify(user.getTwoFactorSecret(), code)) {
            return true;
        }
        for (TwoFactorRecoveryCode recovery : recoveryCodeRepository.findActiveByUserId(user.getId())) {
            if (passwordEncoder.matches(code, recovery.getCodeHash())) {
                recovery.setUsedAt(java.time.Instant.now());
                recoveryCodeRepository.save(recovery);
                log.info("2FA recovery code used for email={}", user.getEmail());
                return true;
            }
        }
        return false;
    }

    private List<String> issueRecoveryCodes(UUID userId) {
        recoveryCodeRepository.deleteByUserId(userId);
        List<String> plaintext = new ArrayList<>();
        List<TwoFactorRecoveryCode> entities = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = randomRecoveryCode();
            plaintext.add(code);
            entities.add(new TwoFactorRecoveryCode(userId, passwordEncoder.encode(code)));
        }
        recoveryCodeRepository.saveAll(entities);
        return plaintext;
    }

    /** A grouped code like ABCD-EFGH, from an alphabet that avoids look-alike characters. */
    private String randomRecoveryCode() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(RECOVERY_ALPHABET.charAt(random.nextInt(RECOVERY_ALPHABET.length())));
        }
        return sb.toString();
    }

    private AppUser load(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    private static AuditActor actorOf(AppUser user) {
        return AuditActor.of(user.getTenantId(), user.getId(), user.getEmail(), user.getRole().name());
    }
}
