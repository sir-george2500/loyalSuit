package com.loyalsuit.modules.platform.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.platform.application.dto.TenantAdminResponse;
import com.loyalsuit.modules.tenants.domain.SubscriptionPlan;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Platform-level tenant administration: list every store, toggle access, change its plan. */
@Service
@RequiredArgsConstructor
public class PlatformTenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public PageResponse<TenantAdminResponse> list(Pageable pageable) {
        return new PageResponse<>(tenantRepository.findAll(pageable).map(PlatformTenantService::toResponse));
    }

    @Transactional
    public TenantAdminResponse setActive(UUID id, boolean active) {
        Tenant tenant = require(id);
        tenant.setActive(active);
        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantAdminResponse changePlan(UUID id, SubscriptionPlan plan) {
        Tenant tenant = require(id);
        tenant.setSubscriptionPlan(plan);
        return toResponse(tenantRepository.save(tenant));
    }

    private Tenant require(UUID id) {
        return tenantRepository.findById(id).orElseThrow(() -> new NotFoundException("Tenant", id));
    }

    private static TenantAdminResponse toResponse(Tenant t) {
        return new TenantAdminResponse(
                t.getId(), t.getName(), t.getSlug(), t.getSubscriptionPlan(), t.isActive(),
                t.getCurrency(), t.getCountry(), t.isOnboarded(), t.getCreatedAt());
    }
}
