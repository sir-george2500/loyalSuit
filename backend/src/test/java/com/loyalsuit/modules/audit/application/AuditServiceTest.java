package com.loyalsuit.modules.audit.application;

import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.audit.domain.AuditLog;
import com.loyalsuit.modules.audit.domain.AuditOutcome;
import com.loyalsuit.modules.audit.domain.port.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository repository;
    @InjectMocks private AuditService service;

    @Test
    void recordSuccess_buildsAndPersistsEntry() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AuditActor actor = AuditActor.of(tenantId, userId, "owner@store.dev", "TENANT_ADMIN");

        service.recordSuccess(AuditAction.LOGIN_SUCCEEDED, actor, "USER", userId.toString(), "Login succeeded");

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        AuditLog entry = saved.getValue();
        assertThat(entry.getTenantId()).isEqualTo(tenantId);
        assertThat(entry.getActorId()).isEqualTo(userId);
        assertThat(entry.getActorEmail()).isEqualTo("owner@store.dev");
        assertThat(entry.getActorRole()).isEqualTo("TENANT_ADMIN");
        assertThat(entry.getAction()).isEqualTo(AuditAction.LOGIN_SUCCEEDED);
        assertThat(entry.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(entry.getResourceType()).isEqualTo("USER");
        assertThat(entry.getResourceId()).isEqualTo(userId.toString());
        assertThat(entry.getDetail()).isEqualTo("Login succeeded");
        assertThat(entry.getOccurredAt()).isNotNull();
    }

    @Test
    void recordFailure_marksOutcomeFailure() {
        service.recordFailure(AuditAction.LOGIN_FAILED, AuditActor.email(null, "ghost@x.dev"),
                "USER", null, "No account");

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(saved.getValue().getTenantId()).isNull();
        assertThat(saved.getValue().getActorId()).isNull();
    }

    @Test
    void record_neverPropagatesRepositoryFailure() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        // Auditing must never break the operation it describes.
        assertThatCode(() -> service.recordSuccess(
                AuditAction.PASSWORD_CHANGED, AuditActor.email(UUID.randomUUID(), "a@b.dev"),
                "USER", "1", "changed"))
                .doesNotThrowAnyException();
    }

    @Test
    void query_delegatesToRepository() {
        UUID tenantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 25);
        when(repository.search(eq(tenantId), eq(AuditAction.LOGIN_FAILED), eq(pageable)))
                .thenReturn(Page.empty());

        Page<AuditLog> result = service.query(tenantId, AuditAction.LOGIN_FAILED, pageable);

        assertThat(result).isEmpty();
        verify(repository).search(tenantId, AuditAction.LOGIN_FAILED, pageable);
    }
}
