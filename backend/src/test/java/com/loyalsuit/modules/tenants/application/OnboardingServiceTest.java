package com.loyalsuit.modules.tenants.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.inventory.domain.Warehouse;
import com.loyalsuit.modules.inventory.domain.port.WarehouseRepository;
import com.loyalsuit.modules.tenants.application.dto.CompleteOnboardingRequest;
import com.loyalsuit.modules.tenants.application.dto.OnboardingStatusResponse;
import com.loyalsuit.modules.tenants.application.event.TenantOnboardedEvent;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ApplicationEventPublisher events;
    @Mock private AuditService auditService;

    @InjectMocks private OnboardingService service;

    private CompleteOnboardingRequest request() {
        CompleteOnboardingRequest r = new CompleteOnboardingRequest();
        r.setBusinessName("  Acme Stores  ");
        r.setCurrency("usd");
        r.setCountry("us");
        r.setTimezone("America/New_York");
        r.setPhone("  +1 555 0100 ");
        r.setWarehouseName("  Main Warehouse ");
        r.setWarehouseAddress("  ");
        return r;
    }

    @Test
    void complete_persistsProfile_provisionsWarehouse_marksOnboarded_andPublishesEvent() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Old Name", "old-slug");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseRepository.existsByTenantId(tenantId)).thenReturn(false);

        OnboardingStatusResponse result = service.complete(tenantId, "owner@acme.dev", request());

        // Profile normalized and persisted.
        assertThat(tenant.getName()).isEqualTo("Acme Stores");
        assertThat(tenant.getCurrency()).isEqualTo("USD");
        assertThat(tenant.getCountry()).isEqualTo("US");
        assertThat(tenant.getTimezone()).isEqualTo("America/New_York");
        assertThat(tenant.getPhone()).isEqualTo("+1 555 0100");
        assertThat(tenant.isOnboarded()).isTrue();
        assertThat(result.onboarded()).isTrue();

        // Default warehouse provisioned, blank address normalized to null.
        ArgumentCaptor<Warehouse> warehouse = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseRepository).save(warehouse.capture());
        assertThat(warehouse.getValue().getName()).isEqualTo("Main Warehouse");
        assertThat(warehouse.getValue().getAddress()).isNull();
        assertThat(warehouse.getValue().isDefault()).isTrue();
        assertThat(warehouse.getValue().getTenantId()).isEqualTo(tenantId);

        // Event raised for after-commit side effects.
        ArgumentCaptor<TenantOnboardedEvent> event = ArgumentCaptor.forClass(TenantOnboardedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().adminEmail()).isEqualTo("owner@acme.dev");
        assertThat(event.getValue().currency()).isEqualTo("USD");
        assertThat(event.getValue().businessName()).isEqualTo("Acme Stores");
    }

    @Test
    void complete_isRejectedWhenAlreadyOnboarded() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Acme", "acme");
        tenant.setOnboardedAt(Instant.now());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.complete(tenantId, "owner@acme.dev", request()))
                .isInstanceOf(ConflictException.class);

        verify(warehouseRepository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void complete_doesNotDuplicateWarehouseWhenOneExists() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Acme", "acme");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseRepository.existsByTenantId(tenantId)).thenReturn(true);

        service.complete(tenantId, "owner@acme.dev", request());

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void getStatus_mapsTenantState() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Acme", "acme");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        OnboardingStatusResponse status = service.getStatus(tenantId);

        assertThat(status.onboarded()).isFalse();
        assertThat(status.businessName()).isEqualTo("Acme");
        assertThat(status.currency()).isEqualTo("USD");
    }
}
