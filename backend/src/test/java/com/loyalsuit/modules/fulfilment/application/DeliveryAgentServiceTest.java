package com.loyalsuit.modules.fulfilment.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryAgentResponse;
import com.loyalsuit.modules.fulfilment.application.dto.RegisterDeliveryAgentRequest;
import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;
import com.loyalsuit.modules.fulfilment.domain.VehicleType;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryAgentRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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
class DeliveryAgentServiceTest {

    @Mock private DeliveryAgentRepository agentRepository;
    @Mock private AppUserRepository userRepository;

    @InjectMocks private DeliveryAgentService service;

    private UUID tenantId;
    private UUID userId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        agentId = UUID.randomUUID();
    }

    private AppUser user(UUID tenant, UserRole role) {
        AppUser user = new AppUser(tenant, "rider@store.dev", "hash", "Rider Rick", role);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private DeliveryAgent agent() {
        DeliveryAgent agent = new DeliveryAgent(tenantId, userId, "555-0100", VehicleType.MOTORBIKE);
        ReflectionTestUtils.setField(agent, "id", agentId);
        return agent;
    }

    private RegisterDeliveryAgentRequest registerRequest() {
        RegisterDeliveryAgentRequest request = new RegisterDeliveryAgentRequest();
        request.setEmail("rider@store.dev");
        request.setPhone("555-0100");
        request.setVehicleType(VehicleType.MOTORBIKE);
        return request;
    }

    private void stubAgentSave() {
        when(agentRepository.save(any(DeliveryAgent.class))).thenAnswer(inv -> {
            DeliveryAgent a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", agentId);
            return a;
        });
    }

    // ---- register -----------------------------------------------------------

    @Test
    void register_onboardsTheUser_andGrantsTheDeliveryAgentRole() {
        // Arrange — a plain customer of this tenant
        AppUser user = user(tenantId, UserRole.CUSTOMER);
        when(userRepository.findByEmail("rider@store.dev")).thenReturn(Optional.of(user));
        when(agentRepository.existsByUserId(userId)).thenReturn(false);
        stubAgentSave();

        // Act
        DeliveryAgentResponse agent = service.register(tenantId, registerRequest());

        // Assert — profile carries the user's name/email; the user is now a DELIVERY_AGENT
        assertThat(agent.name()).isEqualTo("Rider Rick");
        assertThat(agent.email()).isEqualTo("rider@store.dev");
        assertThat(agent.vehicleType()).isEqualTo("MOTORBIKE");
        assertThat(agent.active()).isTrue();
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.DELIVERY_AGENT);
    }

    @Test
    void register_isRejected_whenTheUserBelongsToAnotherTenant() {
        // Arrange
        when(userRepository.findByEmail("rider@store.dev")).thenReturn(Optional.of(user(UUID.randomUUID(), UserRole.CUSTOMER)));

        // Act & Assert
        assertThatThrownBy(() -> service.register(tenantId, registerRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to this store");
        verify(agentRepository, never()).save(any());
    }

    @Test
    void register_refusesToTurnAnAdministratorIntoACourier() {
        // Arrange
        when(userRepository.findByEmail("rider@store.dev")).thenReturn(Optional.of(user(tenantId, UserRole.TENANT_ADMIN)));

        // Act & Assert
        assertThatThrownBy(() -> service.register(tenantId, registerRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("administrator");
        verify(agentRepository, never()).save(any());
    }

    @Test
    void register_isRejected_whenTheUserIsAlreadyAnAgent() {
        // Arrange
        when(userRepository.findByEmail("rider@store.dev")).thenReturn(Optional.of(user(tenantId, UserRole.CUSTOMER)));
        when(agentRepository.existsByUserId(userId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.register(tenantId, registerRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a delivery agent");
        verify(agentRepository, never()).save(any());
    }

    // ---- update / activate --------------------------------------------------

    @Test
    void setActive_deactivatesTheAgent() {
        // Arrange
        when(agentRepository.findByIdAndTenantId(agentId, tenantId)).thenReturn(Optional.of(agent()));
        when(agentRepository.save(any(DeliveryAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(tenantId, UserRole.DELIVERY_AGENT)));

        // Act
        DeliveryAgentResponse agent = service.setActive(agentId, tenantId, false);

        // Assert
        assertThat(agent.active()).isFalse();
    }

    // ---- getMine ------------------------------------------------------------

    @Test
    void getMine_is404_whenTheUserHasNoAgentProfile() {
        // Arrange
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getMine(userId)).isInstanceOf(NotFoundException.class);
    }

    // ---- list ---------------------------------------------------------------

    @Test
    void list_resolvesAgentNames_fromTheLinkedUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(agentRepository.findByTenantId(tenantId, pageable))
                .thenReturn(new PageImpl<>(List.of(agent())));
        when(userRepository.findByIdInAndTenantId(anyCollection(), eq(tenantId)))
                .thenReturn(List.of(user(tenantId, UserRole.DELIVERY_AGENT)));

        // Act
        PageResponse<DeliveryAgentResponse> page = service.list(tenantId, null, pageable);

        // Assert
        assertThat(page.getContent()).singleElement()
                .satisfies(agent -> {
                    assertThat(agent.name()).isEqualTo("Rider Rick");
                    assertThat(agent.phone()).isEqualTo("555-0100");
                });
    }
}
