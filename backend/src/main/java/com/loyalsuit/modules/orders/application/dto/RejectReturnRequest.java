package com.loyalsuit.modules.orders.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectReturnRequest {

    @Size(max = 2000)
    private String note;
}
