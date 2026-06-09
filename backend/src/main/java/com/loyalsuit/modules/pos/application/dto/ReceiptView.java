package com.loyalsuit.modules.pos.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * The assembled, presentation-ready data for one printed receipt — everything the PDF
 * renderer needs and nothing it has to look up. Built by {@code PosReceiptService} from
 * the sale, its order lines, the catalog, and the tenant; consumed by the renderer.
 *
 * @param cashierName who rang the sale up; null if the user can no longer be resolved.
 */
public record ReceiptView(
        String storeName,
        String currency,
        ZoneId timezone,
        String orderNumber,
        Instant soldAt,
        String cashierName,
        List<Line> lines,
        BigDecimal subtotal,
        BigDecimal total,
        BigDecimal amountTendered,
        BigDecimal changeGiven,
        int itemCount) {

    /** One line item on the receipt; {@code name} falls back to a placeholder if the
     *  product was deleted since the sale (order lines don't snapshot the name). */
    public record Line(String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
