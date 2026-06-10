package com.loyalsuit.modules.affiliate.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateResponse;
import com.loyalsuit.modules.affiliate.application.dto.RegisterAffiliateRequest;
import com.loyalsuit.modules.affiliate.domain.Affiliate;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffiliateServiceTest {

    @Mock private AffiliateRepository affiliateRepository;
    @Mock private AppUserRepository userRepository;

    @InjectMocks private AffiliateService service;

    private UUID tenantId;
    private UUID userId;
    private UUID affiliateId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        affiliateId = UUID.randomUUID();
    }

    private AppUser user(UUID tenant) {
        AppUser user = new AppUser(tenant, "ref@store.dev", "hash", "Ref Riley", UserRole.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Affiliate affiliate() {
        Affiliate affiliate = new Affiliate(tenantId, userId, "RILEY", new BigDecimal("5.00"));
        ReflectionTestUtils.setField(affiliate, "id", affiliateId);
        return affiliate;
    }

    private RegisterAffiliateRequest request(String rate) {
        RegisterAffiliateRequest request = new RegisterAffiliateRequest();
        request.setEmail("ref@store.dev");
        request.setCode("riley");
        request.setRewardRate(new BigDecimal(rate));
        return request;
    }

    @Test
    void register_enrolsTheUser_withANormalizedCode() {
        // Arrange
        when(userRepository.findByEmail("ref@store.dev")).thenReturn(Optional.of(user(tenantId)));
        when(affiliateRepository.existsByUserId(userId)).thenReturn(false);
        when(affiliateRepository.existsByTenantIdAndCode(tenantId, "RILEY")).thenReturn(false);
        when(affiliateRepository.save(any(Affiliate.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AffiliateResponse affiliate = service.register(tenantId, request("5.00"));

        // Assert
        assertThat(affiliate.code()).isEqualTo("RILEY");
        assertThat(affiliate.name()).isEqualTo("Ref Riley");
        assertThat(affiliate.rewardRate()).isEqualByComparingTo("5.00");
    }

    @Test
    void register_aDuplicateCode_isRejected() {
        // Arrange
        when(userRepository.findByEmail("ref@store.dev")).thenReturn(Optional.of(user(tenantId)));
        when(affiliateRepository.existsByUserId(userId)).thenReturn(false);
        when(affiliateRepository.existsByTenantIdAndCode(tenantId, "RILEY")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.register(tenantId, request("5.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("code");
        verify(affiliateRepository, never()).save(any());
    }

    @Test
    void register_anAlreadyEnrolledUser_isRejected() {
        // Arrange
        when(userRepository.findByEmail("ref@store.dev")).thenReturn(Optional.of(user(tenantId)));
        when(affiliateRepository.existsByUserId(userId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.register(tenantId, request("5.00")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already an affiliate");
        verify(affiliateRepository, never()).save(any());
    }

    @Test
    void register_aUserFromAnotherTenant_isRejected() {
        // Arrange
        when(userRepository.findByEmail("ref@store.dev")).thenReturn(Optional.of(user(UUID.randomUUID())));

        // Act & Assert
        assertThatThrownBy(() -> service.register(tenantId, request("5.00")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("does not belong to this store");
    }

    @Test
    void register_aRateOver100_isRejected() {
        // Arrange
        when(userRepository.findByEmail("ref@store.dev")).thenReturn(Optional.of(user(tenantId)));
        when(affiliateRepository.existsByUserId(userId)).thenReturn(false);
        when(affiliateRepository.existsByTenantIdAndCode(tenantId, "RILEY")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.register(tenantId, request("150.00")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("between 0 and 100");
        verify(affiliateRepository, never()).save(any());
    }

    @Test
    void list_resolvesAffiliateNames_fromTheUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(affiliateRepository.findByTenantId(tenantId, pageable)).thenReturn(new PageImpl<>(List.of(affiliate())));
        when(userRepository.findByIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of(user(tenantId)));

        // Act
        PageResponse<AffiliateResponse> page = service.list(tenantId, pageable);

        // Assert
        assertThat(page.getContent()).singleElement()
                .satisfies(a -> assertThat(a.name()).isEqualTo("Ref Riley"));
    }
}
