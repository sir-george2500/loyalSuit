package com.loyalsuit.modules.tenants.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.inventory.domain.Warehouse;
import com.loyalsuit.modules.inventory.domain.port.WarehouseRepository;
import com.loyalsuit.modules.tenants.application.dto.CompleteOnboardingRequest;
import com.loyalsuit.modules.tenants.application.dto.OnboardingStatusResponse;
import com.loyalsuit.modules.tenants.application.event.TenantOnboardedEvent;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Drives the one-time tenant setup wizard: company profile, localization, and the
 * first warehouse. Completion is recorded on the tenant ({@code onboardedAt}) and
 * is idempotent-by-rejection — a store can only be onboarded once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final TenantRepository tenantRepository;
    private final WarehouseRepository warehouseRepository;
    private final ApplicationEventPublisher events;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(UUID tenantId) {
        return OnboardingStatusResponse.from(loadTenant(tenantId));
    }

    /**
     * Completes onboarding for the given tenant. Persists the company profile,
     * provisions a default warehouse (if none exists yet), marks the tenant
     * onboarded, and raises {@link TenantOnboardedEvent} for after-commit side
     * effects (welcome email).
     *
     * @param adminEmail the acting admin's email — recipient of the welcome email
     */
    @Transactional
    public OnboardingStatusResponse complete(UUID tenantId, String adminEmail, CompleteOnboardingRequest request) {
        Tenant tenant = loadTenant(tenantId);

        if (tenant.isOnboarded()) {
            throw new ConflictException("This store has already been set up");
        }

        tenant.setName(request.getBusinessName().trim());
        tenant.setCurrency(request.getCurrency().toUpperCase(Locale.ROOT));
        tenant.setTimezone(request.getTimezone().trim());
        tenant.setCountry(request.getCountry() != null ? request.getCountry().toUpperCase(Locale.ROOT) : null);
        tenant.setPhone(trimToNull(request.getPhone()));
        tenant.setOnboardedAt(Instant.now());
        tenant = tenantRepository.save(tenant);

        // Provision the first warehouse, but never duplicate one on a retry.
        if (!warehouseRepository.existsByTenantId(tenantId)) {
            warehouseRepository.save(new Warehouse(
                    tenantId,
                    request.getWarehouseName().trim(),
                    trimToNull(request.getWarehouseAddress()),
                    true));
        }

        log.info("Tenant onboarded: tenantId={} currency={}", tenantId, tenant.getCurrency());
        auditService.recordSuccess(AuditAction.TENANT_ONBOARDED,
                AuditActor.email(tenantId, adminEmail),
                "TENANT", tenantId.toString(), "Setup completed; currency=" + tenant.getCurrency());
        events.publishEvent(new TenantOnboardedEvent(
                tenantId, tenant.getName(), adminEmail, tenant.getCurrency()));

        return OnboardingStatusResponse.from(tenant);
    }

    private Tenant loadTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant", tenantId));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
