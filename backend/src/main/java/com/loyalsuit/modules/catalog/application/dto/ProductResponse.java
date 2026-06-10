package com.loyalsuit.modules.catalog.application.dto;

import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class ProductResponse {

    private final UUID id;
    private final String name;
    private final String slug;
    private final String description;
    private final BigDecimal price;
    private final BigDecimal compareAtPrice;
    private final BigDecimal salePrice;
    private final Instant saleStartsAt;
    private final Instant saleEndsAt;
    private final boolean onSale;
    private final String sku;
    private final String barcode;
    private final UUID categoryId;
    private final UUID vendorId;
    private final boolean digital;
    private final ProductStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.slug = product.getSlug();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.compareAtPrice = product.getCompareAtPrice();
        this.salePrice = product.getSalePrice();
        this.saleStartsAt = product.getSaleStartsAt();
        this.saleEndsAt = product.getSaleEndsAt();
        this.onSale = product.onSale(Instant.now());
        this.sku = product.getSku();
        this.barcode = product.getBarcode();
        this.categoryId = product.getCategoryId();
        this.vendorId = product.getVendorId();
        this.digital = product.isDigital();
        this.status = product.getStatus();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
    }
}
