package com.loyalsuit.modules.catalog.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    /** A time-boxed flash-deal price; in effect only within [saleStartsAt, saleEndsAt]. */
    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "sale_starts_at")
    private Instant saleStartsAt;

    @Column(name = "sale_ends_at")
    private Instant saleEndsAt;

    @Column
    private String sku;

    @Column
    private String barcode;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "vendor_id")
    private UUID vendorId;

    @Column(name = "is_digital", nullable = false)
    private boolean digital = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

    // Variants are loaded via ProductVariantRepository by productId — consistent with
    // the UUID-FK approach used across all modules (no bidirectional JPA associations).

    public Product(UUID tenantId, String name, String slug, BigDecimal price) {
        this.setTenantId(tenantId);
        this.name = name;
        this.slug = slug;
        this.price = price;
    }

    /** True when a flash-deal price is set and the moment falls inside its window. */
    public boolean onSale(Instant now) {
        return salePrice != null
                && (saleStartsAt == null || !now.isBefore(saleStartsAt))
                && (saleEndsAt == null || !now.isAfter(saleEndsAt));
    }

    /** The price to charge now: the flash-deal price while on sale, otherwise the list price. */
    public BigDecimal effectivePrice(Instant now) {
        return onSale(now) ? salePrice : price;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void archive() {
        this.status = ProductStatus.ARCHIVED;
    }
}
