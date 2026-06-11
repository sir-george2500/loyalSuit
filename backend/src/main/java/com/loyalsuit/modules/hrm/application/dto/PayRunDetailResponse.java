package com.loyalsuit.modules.hrm.application.dto;

import java.util.List;

/** A pay run together with its payslips. */
public record PayRunDetailResponse(
        PayRunResponse run,
        List<PayslipResponse> payslips) {
}
