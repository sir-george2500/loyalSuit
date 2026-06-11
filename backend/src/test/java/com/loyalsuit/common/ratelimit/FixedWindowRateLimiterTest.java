package com.loyalsuit.common.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowRateLimiterTest {

    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter();

    @Test
    void allowsUpToTheLimit_thenBlocks() {
        // Arrange — a budget of 3 in a 60s window
        // Act & Assert — first three are allowed
        assertThat(limiter.check("ip-a", 3, 60).allowed()).isTrue();
        assertThat(limiter.check("ip-a", 3, 60).allowed()).isTrue();
        assertThat(limiter.check("ip-a", 3, 60).allowed()).isTrue();

        // the fourth is refused, with a positive retry-after
        FixedWindowRateLimiter.Decision blocked = limiter.check("ip-a", 3, 60);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void countsEachKeyIndependently() {
        // Arrange — exhaust one key
        limiter.check("ip-b", 1, 60);
        assertThat(limiter.check("ip-b", 1, 60).allowed()).isFalse();

        // Act & Assert — a different key has its own budget
        assertThat(limiter.check("ip-c", 1, 60).allowed()).isTrue();
    }
}
