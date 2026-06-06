package com.loyalsuit.modules.orders.application.dto;

import com.loyalsuit.modules.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransitionRequest {

    @NotNull(message = "Target status is required")
    private OrderStatus status;
}
