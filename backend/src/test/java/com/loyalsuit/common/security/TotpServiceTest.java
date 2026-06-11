package com.loyalsuit.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totp = new TotpService();

    @Test
    void verifiesACodeItGenerates_andRejectsAWrongOne() {
        // Arrange
        String secret = totp.generateSecret();

        // Act & Assert — the current code round-trips, a clearly wrong one doesn't
        assertThat(totp.verify(secret, totp.currentCode(secret))).isTrue();
        assertThat(totp.verify(secret, "000000")).isFalse();
        assertThat(totp.verify(secret, "abc")).isFalse();
    }

    @Test
    void generatesADistinctBase32Secret_andAProvisioningUri() {
        String secret = totp.generateSecret();
        assertThat(secret).isNotBlank().matches("[A-Z2-7]+");
        assertThat(totp.generateSecret()).isNotEqualTo(secret);

        String uri = totp.otpauthUri("LoyalSuit", "owner@store.com", secret);
        assertThat(uri).startsWith("otpauth://totp/").contains("secret=" + secret).contains("issuer=LoyalSuit");
    }
}
