package com.loyalsuit.modules.cart.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A shopping cart, persisted in Redis (not the database). Holds only product/
 * variant references and quantities — never prices. Prices are always recomputed
 * from current catalog data when the cart is viewed, so a stale or tampered cart
 * can never dictate what a customer is charged.
 */
@Data
@NoArgsConstructor
public class Cart {

    private UUID tenantId;
    private List<CartItem> items = new ArrayList<>();

    public Cart(UUID tenantId) {
        this.tenantId = tenantId;
    }

    /** Same product + same variant (null-safe) is the same line. */
    private static boolean sameLine(CartItem item, UUID productId, UUID variantId) {
        return item.getProductId().equals(productId)
                && java.util.Objects.equals(item.getVariantId(), variantId);
    }

    public void addOrIncrement(UUID productId, UUID variantId, int quantity) {
        for (CartItem item : items) {
            if (sameLine(item, productId, variantId)) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        items.add(new CartItem(productId, variantId, quantity));
    }

    /** Sets an absolute quantity for a line, removing it when quantity <= 0. */
    public void setQuantity(UUID productId, UUID variantId, int quantity) {
        items.removeIf(item -> sameLine(item, productId, variantId));
        if (quantity > 0) {
            items.add(new CartItem(productId, variantId, quantity));
        }
    }

    public void remove(UUID productId, UUID variantId) {
        items.removeIf(item -> sameLine(item, productId, variantId));
    }
}
