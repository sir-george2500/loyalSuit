package com.loyalsuit.modules.cart.infrastructure;

import com.loyalsuit.modules.cart.domain.Cart;
import com.loyalsuit.modules.cart.domain.port.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed cart store. Keys are namespaced by tenant so carts can never
 * collide across stores. Carts expire after a period of inactivity (refreshed on
 * every save).
 */
@Repository
@RequiredArgsConstructor
public class RedisCartRepository implements CartRepository {

    private static final Duration TTL = Duration.ofDays(30);

    private final RedisTemplate<String, Object> redisTemplate;

    private static String key(UUID tenantId, String token) {
        return "cart:" + tenantId + ":" + token;
    }

    @Override
    public Optional<Cart> find(UUID tenantId, String token) {
        Object value = redisTemplate.opsForValue().get(key(tenantId, token));
        return value instanceof Cart cart ? Optional.of(cart) : Optional.empty();
    }

    @Override
    public void save(UUID tenantId, String token, Cart cart) {
        redisTemplate.opsForValue().set(key(tenantId, token), cart, TTL);
    }

    @Override
    public void delete(UUID tenantId, String token) {
        redisTemplate.delete(key(tenantId, token));
    }
}
