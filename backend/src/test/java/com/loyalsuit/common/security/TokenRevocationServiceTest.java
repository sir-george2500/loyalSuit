package com.loyalsuit.common.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRevocationServiceTest {

    private final TokenRevocationService service = new TokenRevocationService(86400);

    @Test
    void revokesTokensIssuedBeforeTheCutoff_butNotAfter() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Instant old = Instant.now().minusSeconds(30);
        Instant future = Instant.now().plusSeconds(30);

        // Act
        service.revokeAllTokens(userId);

        // Assert — the pre-existing token is revoked; one issued later is not
        assertThat(service.isRevoked(userId, old)).isTrue();
        assertThat(service.isRevoked(userId, future)).isFalse();
    }

    @Test
    void doesNotRevokeTokensForOtherUsers() {
        UUID revoked = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        service.revokeAllTokens(revoked);

        assertThat(service.isRevoked(other, Instant.now().minusSeconds(30))).isFalse();
    }

    @Test
    void treatsNullsAsNotRevoked() {
        assertThat(service.isRevoked(null, Instant.now())).isFalse();
        assertThat(service.isRevoked(UUID.randomUUID(), null)).isFalse();
    }
}
