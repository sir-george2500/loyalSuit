package com.loyalsuit.modules.apikeys.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.apikeys.application.dto.CreatedApiKeyResponse;
import com.loyalsuit.modules.apikeys.domain.ApiKey;
import com.loyalsuit.modules.apikeys.domain.port.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock private ApiKeyRepository repository;
    @InjectMocks private ApiKeyService service;

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private static String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    @Test
    void create_returnsPlaintextOnce_andStoresOnlyHash() throws Exception {
        when(repository.existsByKeyHash(any())).thenReturn(false);
        when(repository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatedApiKeyResponse result = service.create(tenantId, userId, "Mobile app");

        ArgumentCaptor<ApiKey> saved = ArgumentCaptor.forClass(ApiKey.class);
        verify(repository).save(saved.capture());
        ApiKey key = saved.getValue();

        // The plaintext is the lsk_-prefixed secret, returned to the caller exactly once.
        assertThat(result.plaintext()).startsWith("lsk_");
        // Only the SHA-256 hash is persisted — never the plaintext.
        assertThat(key.getKeyHash()).isNotEqualTo(result.plaintext());
        assertThat(key.getKeyHash()).isEqualTo(sha256Hex(result.plaintext()));
        // Display hints: a 12-char prefix and the last four of the secret.
        assertThat(key.getKeyPrefix()).isEqualTo(result.plaintext().substring(0, 12));
        assertThat(result.plaintext()).endsWith(key.getLastFour());
        assertThat(key.getName()).isEqualTo("Mobile app");
        assertThat(key.getCreatedBy()).isEqualTo(userId);
    }

    @Test
    void create_trimsName() {
        when(repository.existsByKeyHash(any())).thenReturn(false);
        when(repository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(tenantId, userId, "  Integration  ");

        ArgumentCaptor<ApiKey> saved = ArgumentCaptor.forClass(ApiKey.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Integration");
    }

    @Test
    void revoke_setsRevokedAt_whenActive() {
        ApiKey key = new ApiKey(tenantId);
        when(repository.findByIdAndTenantId(any(), any())).thenReturn(Optional.of(key));
        when(repository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(key.isActive()).isTrue();
        service.revoke(tenantId, UUID.randomUUID());

        assertThat(key.getRevokedAt()).isNotNull();
        assertThat(key.isActive()).isFalse();
    }

    @Test
    void revoke_isIdempotent_whenAlreadyRevoked() {
        ApiKey key = new ApiKey(tenantId);
        Instant originalRevoke = Instant.now().minusSeconds(3600);
        key.setRevokedAt(originalRevoke);
        when(repository.findByIdAndTenantId(any(), any())).thenReturn(Optional.of(key));

        service.revoke(tenantId, UUID.randomUUID());

        // Already revoked → no second write, timestamp preserved.
        verify(repository, never()).save(any());
        assertThat(key.getRevokedAt()).isEqualTo(originalRevoke);
    }

    @Test
    void revoke_rejectsUnknownKey() {
        when(repository.findByIdAndTenantId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(tenantId, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
