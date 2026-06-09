package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Edit a delivery agent's contact details. Activation is toggled via its own endpoints. */
@Data
public class UpdateDeliveryAgentRequest {

    @NotBlank(message = "A contact phone is required")
    @Size(max = 30)
    private String phone;

    @NotNull(message = "A vehicle type is required")
    private VehicleType vehicleType;
}
