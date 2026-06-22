package com.loyalsuit.modules.staff.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.staff.application.dto.StaffResponse;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private StaffService service;

    private UUID tenantId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
    }

    private AppUser member(UUID id, UserRole role) {
        AppUser u = new AppUser(tenantId, "member@store.dev", "HASH", "Sam Staff", role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    // ---- changeRole ---------------------------------------------------------

    @Test
    void changeRole_assignsAllowedRole() {
        UUID targetId = UUID.randomUUID();
        when(appUserRepository.findByIdAndTenantId(targetId, tenantId)).thenReturn(Optional.of(member(targetId, UserRole.STAFF)));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffResponse result = service.changeRole(tenantId, actingUserId, targetId, UserRole.TENANT_ADMIN);

        assertThat(result.role()).isEqualTo(UserRole.TENANT_ADMIN);
    }

    @Test
    void changeRole_rejectsNonAssignableRole() {
        assertThatThrownBy(() -> service.changeRole(tenantId, actingUserId, UUID.randomUUID(), UserRole.SUPER_ADMIN))
                .isInstanceOf(BusinessException.class);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changeRole_rejectsChangingOwnRole() {
        when(appUserRepository.findByIdAndTenantId(actingUserId, tenantId))
                .thenReturn(Optional.of(member(actingUserId, UserRole.STAFF)));

        assertThatThrownBy(() -> service.changeRole(tenantId, actingUserId, actingUserId, UserRole.TENANT_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("your own role");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changeRole_rejectsModifyingStoreOwner() {
        UUID ownerId = UUID.randomUUID();
        when(appUserRepository.findByIdAndTenantId(ownerId, tenantId))
                .thenReturn(Optional.of(member(ownerId, UserRole.SUPER_ADMIN)));

        assertThatThrownBy(() -> service.changeRole(tenantId, actingUserId, ownerId, UserRole.STAFF))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changeRole_rejectsUnknownMember() {
        UUID targetId = UUID.randomUUID();
        when(appUserRepository.findByIdAndTenantId(targetId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(tenantId, actingUserId, targetId, UserRole.STAFF))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- setActive ----------------------------------------------------------

    @Test
    void setActive_togglesAccount() {
        UUID targetId = UUID.randomUUID();
        AppUser target = member(targetId, UserRole.STAFF);
        when(appUserRepository.findByIdAndTenantId(targetId, tenantId)).thenReturn(Optional.of(target));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffResponse result = service.setActive(tenantId, actingUserId, targetId, false);

        assertThat(result.active()).isFalse();
    }

    @Test
    void setActive_rejectsSelf() {
        when(appUserRepository.findByIdAndTenantId(actingUserId, tenantId))
                .thenReturn(Optional.of(member(actingUserId, UserRole.STAFF)));

        assertThatThrownBy(() -> service.setActive(tenantId, actingUserId, actingUserId, false))
                .isInstanceOf(BusinessException.class);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void setActive_rejectsDeactivatingStoreOwner() {
        UUID ownerId = UUID.randomUUID();
        when(appUserRepository.findByIdAndTenantId(ownerId, tenantId))
                .thenReturn(Optional.of(member(ownerId, UserRole.SUPER_ADMIN)));

        assertThatThrownBy(() -> service.setActive(tenantId, actingUserId, ownerId, false))
                .isInstanceOf(BusinessException.class);
        verify(appUserRepository, never()).save(any());
    }
}
