package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.tenants.domain.SubscriptionPlan;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * The HRM feature gate. HRM is a paid add-on, available on PROFESSIONAL and ENTERPRISE
 * plans; a BASIC tenant is told to upgrade (403) rather than shown any data. Every HRM
 * service calls {@link #require} before doing work, so the gate is enforced in one place.
 */
@Component
@RequiredArgsConstructor
public class HrmAccess {

    private static final Set<SubscriptionPlan> HRM_PLANS =
            Set.of(SubscriptionPlan.PROFESSIONAL, SubscriptionPlan.ENTERPRISE);

    private final TenantRepository tenantRepository;

    public void require(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Store", tenantId));
        if (!HRM_PLANS.contains(tenant.getSubscriptionPlan())) {
            throw new BusinessException(
                    "HRM is available on the Professional and Enterprise plans — upgrade to use it",
                    HttpStatus.FORBIDDEN);
        }
    }
}
