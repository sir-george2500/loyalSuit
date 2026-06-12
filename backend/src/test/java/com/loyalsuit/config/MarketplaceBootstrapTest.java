package com.loyalsuit.config;

import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The flagship marketplace store must exist after the app boots, and re-running the bootstrap must
 * never create a duplicate (it would breach the unique slug). This is what lets the public
 * storefront always resolve a real "LoyalSuit" tenant in every environment.
 */
@SpringBootTest
@ActiveProfiles("test")
class MarketplaceBootstrapTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private MarketplaceProperties properties;
    @Autowired private MarketplaceBootstrap bootstrap;

    @Test
    void flagshipStore_existsAfterStartup() {
        // Assert — the bootstrap ran during context startup
        assertThat(tenantRepository.findBySlug(properties.getSlug()))
                .isPresent()
                .get()
                .satisfies(t -> {
                    assertThat(t.getName()).isEqualTo(properties.getName());
                    assertThat(t.isActive()).isTrue();
                    assertThat(t.isOnboarded()).isTrue();
                });
    }

    @Test
    void runningAgain_isIdempotent() {
        // Arrange
        Tenant first = tenantRepository.findBySlug(properties.getSlug()).orElseThrow();

        // Act — a second boot pass must find, not re-create
        bootstrap.run();

        // Assert — same store, no duplicate / no unique-constraint blow-up
        assertThat(tenantRepository.findBySlug(properties.getSlug()))
                .get()
                .extracting(Tenant::getId)
                .isEqualTo(first.getId());
    }
}
