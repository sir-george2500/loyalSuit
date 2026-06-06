package com.loyalsuit.modules.marketplace.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin rejects a payout request, with a required reason for the audit trail. */
@Data
public class RejectPayoutRequest {

    @NotBlank(message = "A reason is required to reject a payout")
    @Size(max = 2000)
    private String note;
}
