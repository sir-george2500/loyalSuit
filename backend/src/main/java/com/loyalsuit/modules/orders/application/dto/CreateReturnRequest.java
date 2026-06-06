package com.loyalsuit.modules.orders.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** A guest's request to return a delivered order; verified against the order's email. */
@Data
public class CreateReturnRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Please tell us why you're returning the order")
    @Size(max = 2000)
    private String reason;
}
