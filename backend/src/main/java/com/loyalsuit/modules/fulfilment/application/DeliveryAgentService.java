package com.loyalsuit.modules.fulfilment.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryAgentResponse;
import com.loyalsuit.modules.fulfilment.application.dto.RegisterDeliveryAgentRequest;
import com.loyalsuit.modules.fulfilment.application.dto.UpdateDeliveryAgentRequest;
import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryAgentRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The delivery-agent roster. An admin onboards an existing tenant user as a courier —
 * which grants them the DELIVERY_AGENT role — then edits or deactivates them. Agent
 * names/emails are read from the user record (resolved in batch when listing). An admin
 * can't be turned into a courier by mistake.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryAgentService {

    private final DeliveryAgentRepository agentRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public DeliveryAgentResponse register(UUID tenantId, RegisterDeliveryAgentRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("No user found with that email"));
        if (!tenantId.equals(user.getTenantId())) {
            throw new BusinessException("That user does not belong to this store");
        }
        if (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.TENANT_ADMIN) {
            throw new BusinessException("An administrator can't be onboarded as a delivery agent");
        }
        if (agentRepository.existsByUserId(user.getId())) {
            throw new ConflictException("That user is already a delivery agent");
        }

        DeliveryAgent agent = agentRepository.save(
                new DeliveryAgent(tenantId, user.getId(), request.getPhone().trim(), request.getVehicleType()));

        // Onboarding a courier grants the DELIVERY_AGENT role (admin-only capability grant).
        user.setRole(UserRole.DELIVERY_AGENT);
        userRepository.save(user);

        return DeliveryAgentResponse.from(agent, displayName(user), user.getEmail());
    }

    @Transactional
    public DeliveryAgentResponse update(UUID id, UUID tenantId, UpdateDeliveryAgentRequest request) {
        DeliveryAgent agent = load(id, tenantId);
        agent.setPhone(request.getPhone().trim());
        agent.setVehicleType(request.getVehicleType());
        return toResponse(agentRepository.save(agent));
    }

    @Transactional
    public DeliveryAgentResponse setActive(UUID id, UUID tenantId, boolean active) {
        DeliveryAgent agent = load(id, tenantId);
        agent.setActive(active);
        return toResponse(agentRepository.save(agent));
    }

    public DeliveryAgentResponse getMine(UUID userId) {
        return toResponse(agentRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Delivery agent", userId)));
    }

    public PageResponse<DeliveryAgentResponse> list(UUID tenantId, Boolean active, Pageable pageable) {
        Page<DeliveryAgent> page = active == null
                ? agentRepository.findByTenantId(tenantId, pageable)
                : agentRepository.findByTenantIdAndActive(tenantId, active, pageable);

        Map<UUID, AppUser> users = userRepository.findByIdInAndTenantId(
                page.map(DeliveryAgent::getUserId).toList(), tenantId).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));

        return new PageResponse<>(page.map(agent -> {
            AppUser user = users.get(agent.getUserId());
            return DeliveryAgentResponse.from(agent,
                    user == null ? null : displayName(user),
                    user == null ? null : user.getEmail());
        }));
    }

    private DeliveryAgent load(UUID id, UUID tenantId) {
        return agentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Delivery agent", id));
    }

    /** Single-agent response: resolve the linked user's name/email (null if gone). */
    private DeliveryAgentResponse toResponse(DeliveryAgent agent) {
        return userRepository.findById(agent.getUserId())
                .map(user -> DeliveryAgentResponse.from(agent, displayName(user), user.getEmail()))
                .orElseGet(() -> DeliveryAgentResponse.from(agent, null, null));
    }

    private static String displayName(AppUser user) {
        return StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail();
    }
}
