package com.loyalsuit.modules.notifications.infrastructure.persistence;

import com.loyalsuit.modules.notifications.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {
    Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Notification> findByTenantIdAndRecipientIdOrderByCreatedAtDesc(
            UUID tenantId, UUID recipientId, Pageable pageable);
    long countByTenantIdAndRecipientIdAndReadFalse(UUID tenantId, UUID recipientId);
    List<Notification> findByTenantIdAndRecipientIdOrderByCreatedAtDesc(UUID tenantId, UUID recipientId);

    @Modifying(clearAutomatically = true)
    int deleteByTenantIdAndRecipientId(UUID tenantId, UUID recipientId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now "
            + "WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.read = false")
    int markAllRead(@Param("tenantId") UUID tenantId, @Param("recipientId") UUID recipientId,
                    @Param("now") Instant now);
}
