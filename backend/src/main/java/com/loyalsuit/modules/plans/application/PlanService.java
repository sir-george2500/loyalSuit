package com.loyalsuit.modules.plans.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.plans.application.dto.PlanResponse;
import com.loyalsuit.modules.plans.application.dto.UpsertPlanRequest;
import com.loyalsuit.modules.plans.domain.PlatformPlan;
import com.loyalsuit.modules.plans.domain.port.PlatformPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Manages the global subscription-plan catalogue (platform owner only). */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlatformPlanRepository repository;

    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        return repository.findAllOrdered().stream().map(PlanService::toResponse).toList();
    }

    @Transactional
    public PlanResponse create(UpsertPlanRequest request) {
        String code = request.getCode().trim();
        if (repository.existsByCode(code)) {
            throw new ConflictException("A plan with code '" + code + "' already exists.");
        }
        PlatformPlan plan = new PlatformPlan();
        plan.setCode(code);
        apply(plan, request);
        return toResponse(repository.save(plan));
    }

    @Transactional
    public PlanResponse update(UUID id, UpsertPlanRequest request) {
        PlatformPlan plan = require(id);
        String code = request.getCode().trim();
        if (!plan.getCode().equals(code) && repository.existsByCode(code)) {
            throw new ConflictException("A plan with code '" + code + "' already exists.");
        }
        plan.setCode(code);
        apply(plan, request);
        return toResponse(repository.save(plan));
    }

    @Transactional
    public PlanResponse setActive(UUID id, boolean active) {
        PlatformPlan plan = require(id);
        plan.setActive(active);
        return toResponse(repository.save(plan));
    }

    private void apply(PlatformPlan plan, UpsertPlanRequest request) {
        plan.setName(request.getName().trim());
        plan.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        plan.setPrice(request.getPrice());
        plan.setCurrency(request.getCurrency().toUpperCase());
        plan.setBillingInterval(request.getBillingInterval());
        plan.setMaxProducts(request.getMaxProducts());
        plan.setMaxStaff(request.getMaxStaff());
        plan.setActive(request.isActive());
    }

    private PlatformPlan require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Plan", id));
    }

    private static PlanResponse toResponse(PlatformPlan p) {
        return new PlanResponse(
                p.getId(), p.getCode(), p.getName(), p.getDescription(), p.getPrice(), p.getCurrency(),
                p.getBillingInterval(), p.getMaxProducts(), p.getMaxStaff(), p.isActive(), p.getCreatedAt());
    }
}
