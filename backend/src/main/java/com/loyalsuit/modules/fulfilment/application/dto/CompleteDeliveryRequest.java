package com.loyalsuit.modules.fulfilment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Proof of delivery captured when a courier completes a drop-off. */
@Data
public class CompleteDeliveryRequest {

    @NotBlank(message = "Who received the delivery is required")
    @Size(max = 255)
    private String recipientName;

    @Size(max = 500)
    private String note;

    /** Optional signature/photo URL (uploaded to Cloudinary by the client). */
    @Size(max = 500)
    private String proofImageUrl;
}
