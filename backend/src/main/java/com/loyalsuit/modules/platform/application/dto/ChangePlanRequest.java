package com.loyalsuit.modules.platform.application.dto;

import com.loyalsuit.modules.tenants.domain.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(@NotNull SubscriptionPlan plan) {}
