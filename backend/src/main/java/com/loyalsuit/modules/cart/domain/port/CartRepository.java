package com.loyalsuit.modules.cart.domain.port;

import com.loyalsuit.modules.cart.domain.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository {
    Optional<Cart> find(UUID tenantId, String token);
    void save(UUID tenantId, String token, Cart cart);
    void delete(UUID tenantId, String token);
}
