package com.loyalsuit.modules.auth.domain;

import com.loyalsuit.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single-use 2FA recovery code, stored only as a hash. Issued in a batch when 2FA is enabled
 * and consumed (marked used) when a user logs in with one instead of an authenticator code.
 */
@Entity
@Table(name = "two_factor_recovery_codes")
@Getter
@Setter
@NoArgsConstructor
public class TwoFactorRecoveryCode extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    public TwoFactorRecoveryCode(UUID userId, String codeHash) {
        this.userId = userId;
        this.codeHash = codeHash;
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
