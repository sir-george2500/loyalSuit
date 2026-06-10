package com.loyalsuit.modules.affiliate.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Edit an affiliate's referral code and reward rate. */
@Data
public class UpdateAffiliateRequest {

    @NotBlank(message = "A referral code is required")
    @Size(max = 64)
    private String code;

    @NotNull(message = "A reward rate is required")
    @DecimalMin(value = "0.00", message = "Reward rate can't be negative")
    private BigDecimal rewardRate;
}
