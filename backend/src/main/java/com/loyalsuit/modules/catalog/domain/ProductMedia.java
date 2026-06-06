package com.loyalsuit.modules.catalog.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One image in a product's gallery, backed by a Cloudinary asset. {@code publicId}
 * is what we use to delete the asset; {@code url} is the secure delivery URL.
 */
@Entity
@Table(name = "product_media")
@Getter
@Setter
@NoArgsConstructor
public class ProductMedia extends TenantScopedEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "public_id", nullable = false)
    private String publicId;

    @Column(nullable = false)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    public ProductMedia(UUID tenantId, UUID productId, String publicId, String url) {
        this.setTenantId(tenantId);
        this.productId = productId;
        this.publicId = publicId;
        this.url = url;
    }
}
