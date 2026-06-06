package com.loyalsuit.modules.catalog.domain;

import com.loyalsuit.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String name;

    @Column
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Stock is tracked per-warehouse in the inventory module's `stock` table, not
    // on the variant — there is no denormalized quantity here.

    public ProductVariant(UUID productId, String name, BigDecimal price) {
        this.productId = productId;
        this.name = name;
        this.price = price;
    }
}
