package com.loyalsuit.modules.plans.application.dto;

import com.loyalsuit.modules.plans.domain.BillingInterval;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpsertPlanRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal price;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency = "USD";

    @NotNull
    private BillingInterval billingInterval = BillingInterval.MONTHLY;

    @Positive
    private Integer maxProducts;

    @Positive
    private Integer maxStaff;

    private boolean active = true;
}
