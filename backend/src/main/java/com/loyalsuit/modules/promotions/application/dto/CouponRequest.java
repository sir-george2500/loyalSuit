package com.loyalsuit.modules.promotions.application.dto;

import com.loyalsuit.modules.promotions.domain.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/** Create or edit a coupon. Cross-field rules (percent range, window order) are checked in the service. */
@Data
public class CouponRequest {

    @NotBlank(message = "A code is required")
    @Size(max = 64)
    private String code;

    @Size(max = 255)
    private String description;

    @NotNull(message = "A discount type is required")
    private DiscountType discountType;

    @NotNull(message = "A value is required")
    @DecimalMin(value = "0.00", message = "Value can't be negative")
    private BigDecimal value;

    @DecimalMin(value = "0.00", message = "Minimum subtotal can't be negative")
    private BigDecimal minOrderSubtotal;

    @DecimalMin(value = "0.00", message = "Max discount can't be negative")
    private BigDecimal maxDiscount;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Per-customer limit must be at least 1")
    private Integer perCustomerLimit;

    private Instant startsAt;

    private Instant endsAt;

    private Boolean active;
}
