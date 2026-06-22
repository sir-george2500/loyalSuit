package com.loyalsuit.modules.plans.application.dto;

import com.loyalsuit.modules.plans.domain.BillingInterval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        BillingInterval billingInterval,
        Integer maxProducts,
        Integer maxStaff,
        boolean active,
        Instant createdAt) {}
