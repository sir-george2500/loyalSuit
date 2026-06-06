package com.loyalsuit.modules.cart.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartView(
        List<CartItemView> items,
        BigDecimal subtotal,
        int itemCount,
        String currency) {
}
