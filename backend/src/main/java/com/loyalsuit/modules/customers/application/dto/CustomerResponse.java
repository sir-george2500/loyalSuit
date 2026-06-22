package com.loyalsuit.modules.customers.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A storefront customer with their lifetime order rollup, for the admin customer list. */
public record CustomerResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        boolean active,
        Instant joinedAt,
        long orderCount,
        BigDecimal totalSpent) {}
