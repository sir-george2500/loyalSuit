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
 * A request to ring up and complete an in-store sale. The client supplies a
 * {@code clientSaleId} (a UUID it generates) so a retried submit is idempotent — the
 * seam offline sync builds on. Prices are NOT taken from this request; the server
 * prices every line from the current catalog.
 *
 * <p>A sale is paid with cash, card, or both (split). {@code amountTendered} is the cash
 * handed over and {@code cardAmount} is the amount charged on the card terminal; together
 * they must cover the total. Change is only ever returned from the cash portion.
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

    /** Cash handed over by the customer; 0 for a card-only sale. Change comes from this. */
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amountTendered;

    /** Amount charged on the card terminal; 0 (or omitted) for a cash-only sale. */
    @DecimalMin("0.00")
    private BigDecimal cardAmount;
}
