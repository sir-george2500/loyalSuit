package com.loyalsuit.modules.auth.domain.port;

import com.loyalsuit.modules.auth.domain.TwoFactorRecoveryCode;

import java.util.List;
import java.util.UUID;

public interface TwoFactorRecoveryCodeRepository {
    List<TwoFactorRecoveryCode> saveAll(List<TwoFactorRecoveryCode> codes);
    TwoFactorRecoveryCode save(TwoFactorRecoveryCode code);
    List<TwoFactorRecoveryCode> findActiveByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
