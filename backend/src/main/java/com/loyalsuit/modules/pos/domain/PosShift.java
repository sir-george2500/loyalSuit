package com.loyalsuit.modules.pos.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A cash-drawer session for one cashier. The cashier opens it with a starting float,
 * rings up cash sales against it, then closes it by counting the drawer. Closing
 * reconciles the count against what the till should hold:
 *
 * <pre>expected = openingFloat + salesTotal;  variance = countedCash − expected.</pre>
 *
 * <p>The opening time is the row's {@code createdAt}. {@code version} guards the close
 * so a drawer can't be reconciled twice concurrently.
 */
@Entity
@Table(name = "pos_shifts")
@Getter
@Setter
@NoArgsConstructor
public class PosShift extends TenantScopedEntity {

    @Column(name = "cashier_id", nullable = false, updatable = false)
    private UUID cashierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PosShiftStatus status = PosShiftStatus.OPEN;

    @Column(name = "opening_float", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal openingFloat;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Sum of the cash sales rung up during the shift; set at close. */
    @Column(name = "sales_total", precision = 10, scale = 2)
    private BigDecimal salesTotal;

    @Column(name = "sale_count")
    private int saleCount;

    /** What the drawer should hold at close: openingFloat + salesTotal. */
    @Column(name = "expected_cash", precision = 10, scale = 2)
    private BigDecimal expectedCash;

    /** What the cashier actually counted. */
    @Column(name = "counted_cash", precision = 10, scale = 2)
    private BigDecimal countedCash;

    /** countedCash − expectedCash (negative = short, positive = over). */
    @Column(precision = 10, scale = 2)
    private BigDecimal variance;

    @Version
    @Column(nullable = false)
    private long version;

    public PosShift(UUID tenantId, UUID cashierId, BigDecimal openingFloat) {
        this.setTenantId(tenantId);
        this.cashierId = cashierId;
        this.openingFloat = openingFloat;
        this.status = PosShiftStatus.OPEN;
    }

    public boolean isOpen() {
        return status == PosShiftStatus.OPEN;
    }

    /** Reconciles and closes the drawer against the counted cash. */
    public void close(BigDecimal countedCash, BigDecimal salesTotal, int saleCount) {
        this.salesTotal = salesTotal;
        this.saleCount = saleCount;
        this.expectedCash = openingFloat.add(salesTotal);
        this.countedCash = countedCash;
        this.variance = countedCash.subtract(this.expectedCash);
        this.status = PosShiftStatus.CLOSED;
        this.closedAt = Instant.now();
    }
}
