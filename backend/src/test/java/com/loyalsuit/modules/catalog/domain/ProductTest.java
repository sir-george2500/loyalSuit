package com.loyalsuit.modules.catalog.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private Product product() {
        return new Product(UUID.randomUUID(), "Widget", "widget", new BigDecimal("20.00"));
    }

    @Test
    void effectivePrice_isTheListPrice_whenNoSaleIsSet() {
        Product product = product();
        assertThat(product.onSale(Instant.now())).isFalse();
        assertThat(product.effectivePrice(Instant.now())).isEqualByComparingTo("20.00");
    }

    @Test
    void effectivePrice_isTheSalePrice_insideTheWindow() {
        // Arrange — a sale running now
        Product product = product();
        product.setSalePrice(new BigDecimal("15.00"));
        product.setSaleStartsAt(Instant.now().minusSeconds(60));
        product.setSaleEndsAt(Instant.now().plusSeconds(60));

        // Act & Assert
        assertThat(product.onSale(Instant.now())).isTrue();
        assertThat(product.effectivePrice(Instant.now())).isEqualByComparingTo("15.00");
    }

    @Test
    void effectivePrice_revertsToList_outsideTheWindow() {
        // Arrange — a sale that has already ended
        Product product = product();
        product.setSalePrice(new BigDecimal("15.00"));
        product.setSaleEndsAt(Instant.now().minusSeconds(60));

        // Act & Assert
        assertThat(product.onSale(Instant.now())).isFalse();
        assertThat(product.effectivePrice(Instant.now())).isEqualByComparingTo("20.00");
    }

    @Test
    void onSale_withNoWindow_runsIndefinitely() {
        // A sale price with no start/end is always on.
        Product product = product();
        product.setSalePrice(new BigDecimal("15.00"));
        assertThat(product.onSale(Instant.now())).isTrue();
    }
}
