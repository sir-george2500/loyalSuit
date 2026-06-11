package com.loyalsuit.common.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Revokes already-issued JWTs at the user level. Stateless tokens can't be "deleted", so we
 * record the instant a user's tokens were invalidated; any token issued before it is refused.
 * Used to close the gap after account deletion (and available for "sign out everywhere").
 *
 * <p>In-memory and per-instance — like the rate limiter, it degrades gracefully with no external
 * dependency; a multi-node deployment would share this through Redis. Entries self-expire once no
 * token issued before the cutoff could still be valid.
 */
@Service
public class TokenRevocationService {

    private final long maxTokenLifetimeSeconds;
    private final ConcurrentHashMap<java.util.UUID, Long> revokedBefore = new ConcurrentHashMap<>();

    public TokenRevocationService(
            @org.springframework.beans.factory.annotation.Value("${app.jwt.expiration-seconds:86400}")
            long maxTokenLifetimeSeconds) {
        this.maxTokenLifetimeSeconds = maxTokenLifetimeSeconds;
    }

    /** Invalidate every token already issued for this user (those issued later stay valid). */
    public void revokeAllTokens(java.util.UUID userId) {
        // +1 so tokens minted in the same second as the revocation are also caught.
        revokedBefore.put(userId, Instant.now().getEpochSecond() + 1);
    }

    /** True if a token issued at {@code issuedAt} for this user has since been revoked. */
    public boolean isRevoked(java.util.UUID userId, Instant issuedAt) {
        if (userId == null || issuedAt == null) {
            return false;
        }
        evictStale();
        Long cutoff = revokedBefore.get(userId);
        return cutoff != null && issuedAt.getEpochSecond() < cutoff;
    }

    /** Drop cutoffs old enough that no token issued before them could still be unexpired. */
    private void evictStale() {
        long horizon = Instant.now().getEpochSecond() - maxTokenLifetimeSeconds;
        revokedBefore.values().removeIf(cutoff -> cutoff < horizon);
    }
}
