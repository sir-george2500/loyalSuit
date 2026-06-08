package com.loyalsuit.modules.pos.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * A request to ring up and complete an in-store cash sale. The client supplies a
 * {@code clientSaleId} (a UUID it generates) so a retried submit is idempotent — the
 * seam offline sync builds on. Prices are NOT taken from this request; the server
 * prices every line from the current catalog.
 */
@Getter
@Setter
public class CompleteSaleRequest {

    @NotEmpty
    @Size(max = 100)
    private String clientSaleId;

    @NotEmpty
    @Valid
    private List<SaleLineRequest> items;

    /** Cash handed over by the customer. Must cover the total; the difference is change. */
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amountTendered;
}
