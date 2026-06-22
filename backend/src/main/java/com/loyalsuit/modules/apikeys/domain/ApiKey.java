package com.loyalsuit.modules.apikeys.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A tenant API key. Only the SHA-256 hash of the key is persisted; the plaintext is returned
 * to the caller once at creation and cannot be recovered. The prefix + last four characters
 * let the UI identify a key without exposing it.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
public class ApiKey extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public ApiKey(UUID tenantId) {
        setTenantId(tenantId);
    }

    public boolean isActive() {
        return revokedAt == null;
    }
}
