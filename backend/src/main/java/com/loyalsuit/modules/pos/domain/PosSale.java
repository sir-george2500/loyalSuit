package com.loyalsuit.modules.pos.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A completed in-store (POS) sale. The money, line items, and commission live on the
 * linked {@code orders} row — a POS sale is the channel record layered on top: who rang
 * it up ({@code cashierId}), how much cash was taken ({@code amountTendered}) and given
 * back ({@code changeGiven}), and how much went on the card terminal ({@code cardAmount}).
 * The cash that stayed in the drawer is {@code amountTendered − changeGiven}; that, not the
 * total, is what the shift reconciles against.
 *
 * <p>{@code clientSaleId} is supplied by the terminal and is unique per tenant. It makes
 * completing a sale idempotent — a retried submit (the offline-sync seam) maps back to
 * the same sale instead of ringing it up twice.
 */
@Entity
@Table(name = "pos_sales")
@Getter
@Setter
@NoArgsConstructor
public class PosSale extends TenantScopedEntity {

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    /** The linked order's number — the human-facing receipt number (denormalised). */
    @Column(name = "order_number", nullable = false, updatable = false)
    private String orderNumber;

    @Column(name = "cashier_id", nullable = false, updatable = false)
    private UUID cashierId;

    /** The cash-drawer session this sale was rung up against (for reconciliation). */
    @Column(name = "shift_id", nullable = false, updatable = false)
    private UUID shiftId;

    /** Terminal-generated id; unique per tenant so a re-submitted sale is idempotent. */
    @Column(name = "client_sale_id", nullable = false, updatable = false)
    private String clientSaleId;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "amount_tendered", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal amountTendered;

    @Column(name = "change_given", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal changeGiven;

    /** Amount charged on the card terminal; 0 for a cash-only sale. */
    @Column(name = "card_amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal cardAmount;

    @Column(name = "item_count", nullable = false, updatable = false)
    private int itemCount;

    public PosSale(UUID tenantId, UUID orderId, String orderNumber, UUID cashierId, UUID shiftId,
                   String clientSaleId, BigDecimal subtotal, BigDecimal total, BigDecimal amountTendered,
                   BigDecimal changeGiven, BigDecimal cardAmount, int itemCount) {
        this.setTenantId(tenantId);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.cashierId = cashierId;
        this.shiftId = shiftId;
        this.clientSaleId = clientSaleId;
        this.subtotal = subtotal;
        this.total = total;
        this.amountTendered = amountTendered;
        this.changeGiven = changeGiven;
        this.cardAmount = cardAmount;
        this.itemCount = itemCount;
    }
}
