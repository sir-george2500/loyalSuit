package com.loyalsuit.modules.auth.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.auth.application.event.PasswordResetCompletedEvent;
import com.loyalsuit.modules.auth.application.event.PasswordResetRequestedEvent;
import com.loyalsuit.modules.auth.domain.PasswordResetToken;
import com.loyalsuit.modules.auth.domain.port.PasswordResetTokenRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher events;
    @Mock private AuditService auditService;

    private PasswordResetService service;

    private AppUser user;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, events, auditService);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(service, "expiryMinutes", 30L);

        user = new AppUser(UUID.randomUUID(), "jane@acme.dev", "OLD_HASH", "Jane", UserRole.TENANT_ADMIN);
        ReflectionTestUtils.setField(user, "id", userId);
    }

    // ---- requestReset: no enumeration --------------------------------------

    @Test
    void requestReset_forActiveUser_issuesHashedTokenAndEmailsLink() {
        when(userRepository.findByEmail("jane@acme.dev")).thenReturn(Optional.of(user));

        service.requestReset("  Jane@Acme.dev  ");

        // Old tokens cleared, exactly one new token persisted.
        verify(tokenRepository).deleteByUserId(userId);
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).hasSize(64); // SHA-256 hex
        assertThat(saved.getValue().getTokenHash()).matches("[0-9a-f]{64}");
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);

        // Email carries a link with the RAW token (which is never stored).
        ArgumentCaptor<PasswordResetRequestedEvent> event =
                ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("jane@acme.dev");
        assertThat(event.getValue().resetLink()).contains("/reset-password?token=");
        // The raw token in the link must not equal the stored hash.
        String rawToken = event.getValue().resetLink().substring(
                event.getValue().resetLink().indexOf("token=") + 6);
        assertThat(rawToken).isNotEqualTo(saved.getValue().getTokenHash());
    }

    @Test
    void requestReset_forUnknownEmail_isSilentNoOp() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        service.requestReset("ghost@nowhere.dev");

        verify(tokenRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void requestReset_forInactiveUser_isSilentNoOp() {
        user.setActive(false);
        when(userRepository.findByEmail("jane@acme.dev")).thenReturn(Optional.of(user));

        service.requestReset("jane@acme.dev");

        verify(tokenRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    // ---- reset --------------------------------------------------------------

    @Test
    void reset_withUsableToken_setsNewPassword_marksUsed_andConfirms() {
        PasswordResetToken token = new PasswordResetToken(userId, "hash", Instant.now().plusSeconds(600));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPass@1", "OLD_HASH")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@1")).thenReturn("NEW_HASH");

        service.reset("raw-token", "NewPass@1");

        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
        verify(events).publishEvent(any(PasswordResetCompletedEvent.class));
    }

    @Test
    void reset_withUnknownToken_failsGenericallyAndChangesNothing() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("bad", "NewPass@1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid or has expired");

        verify(userRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void reset_withExpiredToken_failsGenerically() {
        PasswordResetToken expired = new PasswordResetToken(userId, "hash", Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.reset("raw", "NewPass@1"))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void reset_withUsedToken_failsGenerically() {
        PasswordResetToken used = new PasswordResetToken(userId, "hash", Instant.now().plusSeconds(600));
        used.markUsed(Instant.now());
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.reset("raw", "NewPass@1"))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void reset_rejectsReuseOfCurrentPassword() {
        PasswordResetToken token = new PasswordResetToken(userId, "hash", Instant.now().plusSeconds(600));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SamePass@1", "OLD_HASH")).thenReturn(true);

        assertThatThrownBy(() -> service.reset("raw", "SamePass@1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("different");

        verify(userRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }
}
