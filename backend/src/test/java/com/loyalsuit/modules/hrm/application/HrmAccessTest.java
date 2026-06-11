package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.tenants.domain.SubscriptionPlan;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrmAccessTest {

    @Mock private TenantRepository tenantRepository;

    @InjectMocks private HrmAccess hrmAccess;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    private void planIs(SubscriptionPlan plan) {
        Tenant tenant = new Tenant("Acme", "acme");
        tenant.setSubscriptionPlan(plan);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    }

    @Test
    void require_allowsProfessionalAndEnterprise() {
        planIs(SubscriptionPlan.PROFESSIONAL);
        assertThatCode(() -> hrmAccess.require(tenantId)).doesNotThrowAnyException();
    }

    @Test
    void require_blocksTheBasicPlan_withA403() {
        // Arrange
        planIs(SubscriptionPlan.BASIC);

        // Act & Assert
        assertThatThrownBy(() -> hrmAccess.require(tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Professional and Enterprise")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void require_aMissingTenant_is404() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hrmAccess.require(tenantId)).isInstanceOf(NotFoundException.class);
    }
}
