package com.loyalsuit.modules.pos.application.dto;

import com.loyalsuit.modules.catalog.domain.Product;

import java.math.BigDecimal;
import java.util.UUID;

/** A lean catalog row for the terminal's product grid / scan lookup. */
public record PosProductResponse(
        UUID id,
        String name,
        String sku,
        String barcode,
        BigDecimal price) {

    public static PosProductResponse from(Product product) {
        return new PosProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getPrice());
    }
}
