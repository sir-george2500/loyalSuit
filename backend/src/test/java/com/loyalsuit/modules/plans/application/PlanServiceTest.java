package com.loyalsuit.modules.plans.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.plans.application.dto.PlanResponse;
import com.loyalsuit.modules.plans.application.dto.UpsertPlanRequest;
import com.loyalsuit.modules.plans.domain.BillingInterval;
import com.loyalsuit.modules.plans.domain.PlatformPlan;
import com.loyalsuit.modules.plans.domain.port.PlatformPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock private PlatformPlanRepository repository;
    @InjectMocks private PlanService service;

    private UpsertPlanRequest request(String code) {
        UpsertPlanRequest r = new UpsertPlanRequest();
        r.setCode(code);
        r.setName("Professional");
        r.setDescription(" growing stores ");
        r.setPrice(new BigDecimal("29900"));
        r.setCurrency("rwf");
        r.setBillingInterval(BillingInterval.MONTHLY);
        r.setMaxProducts(500);
        r.setMaxStaff(10);
        r.setActive(true);
        return r;
    }

    @Test
    void create_persistsPlan_normalisingFields() {
        when(repository.existsByCode("PRO")).thenReturn(false);
        when(repository.save(any(PlatformPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponse result = service.create(request(" PRO "));

        assertThat(result.code()).isEqualTo("PRO");          // trimmed
        assertThat(result.currency()).isEqualTo("RWF");        // upper-cased
        assertThat(result.description()).isEqualTo("growing stores"); // trimmed
        assertThat(result.maxProducts()).isEqualTo(500);
    }

    @Test
    void create_rejectsDuplicateCode() {
        when(repository.existsByCode("PRO")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("PRO")))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_rejectsRenameOntoExistingCode() {
        UUID id = UUID.randomUUID();
        PlatformPlan existing = new PlatformPlan();
        existing.setCode("BASIC");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByCode("PRO")).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request("PRO")))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_allowsKeepingSameCode() {
        UUID id = UUID.randomUUID();
        PlatformPlan existing = new PlatformPlan();
        existing.setCode("PRO");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(PlatformPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanResponse result = service.update(id, request("PRO"));

        assertThat(result.name()).isEqualTo("Professional");
        // existsByCode is not consulted when the code is unchanged.
        verify(repository, never()).existsByCode(any());
    }

    @Test
    void update_rejectsUnknownPlan() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request("PRO")))
                .isInstanceOf(NotFoundException.class);
    }
}
