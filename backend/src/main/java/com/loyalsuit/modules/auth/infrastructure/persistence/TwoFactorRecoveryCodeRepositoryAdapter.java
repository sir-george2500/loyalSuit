package com.loyalsuit.modules.auth.infrastructure.persistence;

import com.loyalsuit.modules.auth.domain.TwoFactorRecoveryCode;
import com.loyalsuit.modules.auth.domain.port.TwoFactorRecoveryCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TwoFactorRecoveryCodeRepositoryAdapter implements TwoFactorRecoveryCodeRepository {

    private final TwoFactorRecoveryCodeJpaRepository jpa;

    @Override
    public List<TwoFactorRecoveryCode> saveAll(List<TwoFactorRecoveryCode> codes) {
        return jpa.saveAll(codes);
    }

    @Override
    public TwoFactorRecoveryCode save(TwoFactorRecoveryCode code) {
        return jpa.save(code);
    }

    @Override
    public List<TwoFactorRecoveryCode> findActiveByUserId(UUID userId) {
        return jpa.findByUserIdAndUsedAtIsNull(userId);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUserId(userId);
    }
}
