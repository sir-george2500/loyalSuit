package com.loyalsuit.modules.reviews.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A customer's rating + optional comment for a product they bought. Created only after a
 * delivered purchase (verified in the service), one per customer per product. The selling
 * {@code vendorId} is snapshotted from the purchase so a seller can read reviews of their
 * products without a join back to orders; it is null for a house product. {@code authorName}
 * is a privacy-minimised display name ("Jane D.") captured at write time.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review extends TenantScopedEntity {

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "vendor_id", updatable = false)
    private UUID vendorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    public Review(UUID tenantId, UUID productId, UUID customerId, UUID vendorId,
                  String authorName, int rating, String comment) {
        this.setTenantId(tenantId);
        this.productId = productId;
        this.customerId = customerId;
        this.vendorId = vendorId;
        this.authorName = authorName;
        this.rating = rating;
        this.comment = comment;
    }
}
