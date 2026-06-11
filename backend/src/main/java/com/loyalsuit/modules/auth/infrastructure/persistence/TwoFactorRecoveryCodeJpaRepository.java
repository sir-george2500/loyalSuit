package com.loyalsuit.modules.auth.infrastructure.persistence;

import com.loyalsuit.modules.auth.domain.TwoFactorRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface TwoFactorRecoveryCodeJpaRepository extends JpaRepository<TwoFactorRecoveryCode, UUID> {
    List<TwoFactorRecoveryCode> findByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying
    @Query("delete from TwoFactorRecoveryCode c where c.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
