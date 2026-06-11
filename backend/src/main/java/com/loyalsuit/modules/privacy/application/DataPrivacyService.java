package com.loyalsuit.modules.privacy.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.auth.domain.port.TwoFactorRecoveryCodeRepository;
import com.loyalsuit.modules.privacy.domain.port.PersonalDataContributor;
import com.loyalsuit.modules.privacy.domain.port.PersonalDataEraser;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * GDPR data subject rights. Export assembles the caller's personal data from every module that
 * contributes a section; erasure scrubs the core profile, runs each module's eraser, and clears
 * 2FA — anonymising the account while preserving records required for financial/legal integrity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPrivacyService {

    // Owners can't self-erase here — it would orphan their tenant; they must close/transfer first.
    private static final Set<UserRole> PROTECTED_ROLES = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    private final List<PersonalDataContributor> contributors;
    private final List<PersonalDataEraser> erasers;
    private final AppUserRepository userRepository;
    private final TwoFactorRecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId, UUID tenantId) {
        AppUser user = load(userId);
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", Instant.now().toString());
        export.put("profile", profile(user));
        for (PersonalDataContributor contributor : contributors) {
            export.put(contributor.section(), contributor.export(userId, tenantId));
        }
        auditService.recordSuccess(AuditAction.PERSONAL_DATA_EXPORTED, actorOf(user),
                "USER", user.getId().toString(), "Personal data exported");
        return export;
    }

    @Transactional
    public void deleteAccount(UUID userId, UUID tenantId, String password) {
        AppUser user = load(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("Password is incorrect", HttpStatus.UNAUTHORIZED);
        }
        if (PROTECTED_ROLES.contains(user.getRole())) {
            throw new BusinessException(
                    "Store owners can't delete their account here — close or transfer the store first",
                    HttpStatus.FORBIDDEN);
        }

        // Let each module scrub or delete the personal data it holds.
        erasers.forEach(eraser -> eraser.erase(userId, tenantId));

        // Anonymise the core account: no PII remains, and it can no longer sign in.
        user.setEmail("deleted-" + user.getId() + "@anonymized.invalid");
        user.setFullName("Deleted user");
        user.setPhone(null);
        user.setAvatarUrl(null);
        user.setActive(false);
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        recoveryCodeRepository.deleteByUserId(userId);

        log.info("Account anonymised for userId={}", userId);
        auditService.recordSuccess(AuditAction.ACCOUNT_ANONYMISED,
                AuditActor.of(tenantId, userId, null, user.getRole().name()),
                "USER", userId.toString(), "Account anonymised on user request");
    }

    private Map<String, Object> profile(AppUser user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("phone", user.getPhone());
        profile.put("role", user.getRole().name());
        profile.put("tenantId", user.getTenantId());
        profile.put("twoFactorEnabled", user.isTwoFactorEnabled());
        profile.put("createdAt", user.getCreatedAt());
        return profile;
    }

    private AppUser load(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }

    private static AuditActor actorOf(AppUser user) {
        return AuditActor.of(user.getTenantId(), user.getId(), user.getEmail(), user.getRole().name());
    }
}
