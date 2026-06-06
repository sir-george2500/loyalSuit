package com.loyalsuit.modules.marketplace.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin records a payout as paid, optionally noting the cash transfer reference. */
@Data
public class PayPayoutRequest {

    @Size(max = 255)
    private String reference;

    @Size(max = 2000)
    private String note;
}
