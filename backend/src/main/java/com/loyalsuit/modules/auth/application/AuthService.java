package com.loyalsuit.modules.auth.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.auth.application.dto.AuthResponse;
import com.loyalsuit.modules.auth.application.dto.ChangePasswordRequest;
import com.loyalsuit.modules.auth.application.dto.LoginRequest;
import com.loyalsuit.modules.auth.application.dto.MfaLoginRequest;
import com.loyalsuit.modules.auth.application.dto.RegisterRequest;
import com.loyalsuit.modules.auth.application.dto.UserProfile;
import com.loyalsuit.config.MarketplaceProperties;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import com.loyalsuit.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final TwoFactorService twoFactorService;
    private final MarketplaceProperties marketplaceProperties;

    /**
     * Registers a new shopper on the LoyalSuit marketplace. Single-marketplace model: everyone signs
     * up as a CUSTOMER of the one flagship store — signing up no longer spins up a separate store.
     * Selling is a deliberate, approved step afterwards ("Sell on LoyalSuit" → vendor application).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        Tenant marketplace = tenantRepository.findBySlug(marketplaceProperties.getSlug())
                .orElseThrow(() -> new BusinessException(
                        "The marketplace is not available right now", HttpStatus.SERVICE_UNAVAILABLE));

        AppUser user = new AppUser(
                marketplace.getId(),
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                UserRole.CUSTOMER
        );
        user = userRepository.save(user);

        log.info("New shopper registered on '{}': {}", marketplace.getSlug(), email);
        auditService.recordSuccess(AuditAction.USER_REGISTERED, actorOf(user),
                "USER", user.getId().toString(), "Registered on the LoyalSuit marketplace");
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Failed login: no account for email={}", email);
                    auditService.recordFailure(AuditAction.LOGIN_FAILED, AuditActor.email(null, email),
                            "USER", null, "No account for that email");
                    return new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login: bad password for email={}", email);
            auditService.recordFailure(AuditAction.LOGIN_FAILED, actorOf(user),
                    "USER", user.getId().toString(), "Invalid password");
            throw new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isActive()) {
            log.warn("Failed login: deactivated account email={}", email);
            auditService.recordFailure(AuditAction.LOGIN_FAILED, actorOf(user),
                    "USER", user.getId().toString(), "Account is deactivated");
            throw new BusinessException("This account has been deactivated", HttpStatus.FORBIDDEN);
        }

        // Password is correct; if 2FA is on, hold the token behind a short-lived challenge.
        if (user.isTwoFactorEnabled()) {
            log.info("Login password ok; 2FA challenge issued for email={}", email);
            return AuthResponse.mfaChallenge(jwtService.issueMfaChallenge(user.getId()));
        }

        log.info("Login success: email={} role={}", email, user.getRole());
        auditService.recordSuccess(AuditAction.LOGIN_SUCCEEDED, actorOf(user),
                "USER", user.getId().toString(), "Login succeeded");
        return buildAuthResponse(user);
    }

    /**
     * Completes a 2FA login: validates the challenge token, then the authenticator/recovery code.
     * The challenge token alone is never a credential — it can only be redeemed here.
     */
    @Transactional
    public AuthResponse completeMfaLogin(MfaLoginRequest request) {
        UUID userId;
        try {
            userId = jwtService.parseMfaChallenge(request.getMfaToken());
        } catch (RuntimeException e) {
            throw new BusinessException("Your verification session has expired — sign in again",
                    HttpStatus.UNAUTHORIZED);
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!user.isActive() || !user.isTwoFactorEnabled()) {
            throw new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (!twoFactorService.verifySecondFactor(user, request.getCode().trim())) {
            log.warn("Failed 2FA: bad code for email={}", user.getEmail());
            auditService.recordFailure(AuditAction.LOGIN_FAILED, actorOf(user),
                    "USER", user.getId().toString(), "Invalid two-factor code");
            throw new BusinessException("That code isn't valid", HttpStatus.UNAUTHORIZED);
        }

        log.info("Login success (2FA): email={} role={}", user.getEmail(), user.getRole());
        auditService.recordSuccess(AuditAction.LOGIN_SUCCEEDED, actorOf(user),
                "USER", user.getId().toString(), "Login succeeded with 2FA");
        return buildAuthResponse(user);
    }

    /**
     * Changes the authenticated user's password after verifying the current one.
     * Rejects reuse of the same password. Never logs password material.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change rejected: wrong current password for email={}", user.getEmail());
            throw new BusinessException("Current password is incorrect", HttpStatus.UNAUTHORIZED);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("New password must be different from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for email={}", user.getEmail());
        auditService.recordSuccess(AuditAction.PASSWORD_CHANGED, actorOf(user),
                "USER", user.getId().toString(), "Password changed");
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID userId) {
        return userRepository.findById(userId)
                .map(UserProfile::new)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    private static AuditActor actorOf(AppUser user) {
        return AuditActor.of(user.getTenantId(), user.getId(), user.getEmail(), user.getRole().name());
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        String token = jwtService.issueToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.getTenantId());
        return new AuthResponse(token, jwtService.getExpirationSeconds(), new UserProfile(user));
    }
}
