package com.loyalsuit.modules.auth.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.config.MarketplaceProperties;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.auth.application.dto.ChangePasswordRequest;
import com.loyalsuit.modules.auth.application.dto.RegisterRequest;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import com.loyalsuit.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;
    @Mock private MarketplaceProperties marketplaceProperties;

    @InjectMocks private AuthService authService;

    private UUID userId;
    private AppUser user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new AppUser(UUID.randomUUID(), "owner@store.dev", "HASHED_CURRENT", "Owner", UserRole.TENANT_ADMIN);
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    void register_createsACustomerInTheMarketplace_withoutProvisioningAStore() {
        // Arrange — the flagship store exists; a new person signs up as a shopper
        UUID marketplaceId = UUID.randomUUID();
        Tenant marketplace = new Tenant("LoyalSuit", "loyalsuit");
        ReflectionTestUtils.setField(marketplace, "id", marketplaceId);

        var request = new RegisterRequest();
        request.setEmail("Shopper@Test.dev");
        request.setPassword("Pa55word!");
        request.setFullName("Sam Shopper");

        when(userRepository.existsByEmail("shopper@test.dev")).thenReturn(false);
        when(marketplaceProperties.getSlug()).thenReturn("loyalsuit");
        when(tenantRepository.findBySlug("loyalsuit")).thenReturn(Optional.of(marketplace));
        when(passwordEncoder.encode("Pa55word!")).thenReturn("HASH");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID()); // mimic JPA assigning the id
            return u;
        });
        when(jwtService.issueToken(any(), any(), any(), any())).thenReturn("token");

        // Act
        authService.register(request);

        // Assert — a CUSTOMER lands in the marketplace tenant; no new store is provisioned
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(saved.getTenantId()).isEqualTo(marketplaceId);
        assertThat(saved.getEmail()).isEqualTo("shopper@test.dev");
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void changePassword_updatesHash_whenCurrentPasswordMatches() {
        // Arrange
        var request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass@123");
        request.setNewPassword("NewPass@456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "HASHED_CURRENT")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "HASHED_CURRENT")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("HASHED_NEW");

        // Act
        authService.changePassword(userId, request);

        // Assert
        assertThat(user.getPasswordHash()).isEqualTo("HASHED_NEW");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_throwsUnauthorized_whenCurrentPasswordWrong() {
        // Arrange
        var request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPass@1");
        request.setNewPassword("NewPass@456");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass@1", "HASHED_CURRENT")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Current password is incorrect");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_rejectsReuseOfSamePassword() {
        // Arrange
        var request = new ChangePasswordRequest();
        request.setCurrentPassword("SamePass@123");
        request.setNewPassword("SamePass@123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SamePass@123", "HASHED_CURRENT")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be different");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_throwsNotFound_whenUserMissing() {
        // Arrange
        var request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass@123");
        request.setNewPassword("NewPass@456");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(NotFoundException.class);
    }
}
