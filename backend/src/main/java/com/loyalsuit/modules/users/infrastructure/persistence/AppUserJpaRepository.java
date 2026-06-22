package com.loyalsuit.modules.users.infrastructure.persistence;

import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AppUserJpaRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId);
    boolean existsByEmail(String email);
    Optional<AppUser> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<AppUser> findByTenantIdAndRoleInOrderByCreatedAtDesc(
            UUID tenantId, Collection<UserRole> roles, Pageable pageable);

    @Query("""
            SELECT u FROM AppUser u
            WHERE u.tenantId = :tenantId AND u.role = com.loyalsuit.modules.users.domain.UserRole.CUSTOMER
              AND (:search IS NULL
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<AppUser> findCustomers(
            @Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);
}
