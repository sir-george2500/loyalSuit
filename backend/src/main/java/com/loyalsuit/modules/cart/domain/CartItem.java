package com.loyalsuit.modules.cart.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** One line in a cart: a product (optionally a variant) and a quantity. No price. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private UUID productId;
    private UUID variantId;
    private int quantity;
}
