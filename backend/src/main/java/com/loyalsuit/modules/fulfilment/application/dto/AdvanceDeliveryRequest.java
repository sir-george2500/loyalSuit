package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Move a delivery to the next status; {@code note} is recorded on the timeline (required for FAILED). */
@Data
public class AdvanceDeliveryRequest {

    @NotNull(message = "A target status is required")
    private DeliveryStatus status;

    @Size(max = 500)
    private String note;
}
