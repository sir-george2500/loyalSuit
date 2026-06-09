package com.loyalsuit.modules.fulfilment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** Assign an order's delivery to a courier, by order number. */
@Data
public class AssignDeliveryRequest {

    @NotBlank(message = "The order number is required")
    @Size(max = 100)
    private String orderNumber;

    @NotNull(message = "An agent is required")
    private UUID agentId;
}
