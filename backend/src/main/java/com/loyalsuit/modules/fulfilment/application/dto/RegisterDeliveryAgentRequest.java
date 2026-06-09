package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Onboard an existing tenant user (identified by email) as a delivery agent. */
@Data
public class RegisterDeliveryAgentRequest {

    @NotBlank(message = "The agent's email is required")
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank(message = "A contact phone is required")
    @Size(max = 30)
    private String phone;

    @NotNull(message = "A vehicle type is required")
    private VehicleType vehicleType;
}
