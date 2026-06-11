package com.loyalsuit.modules.hrm.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** An approve / reject decision, with an optional note explaining it. */
@Data
public class LeaveDecision {

    @Size(max = 500)
    private String note;
}
