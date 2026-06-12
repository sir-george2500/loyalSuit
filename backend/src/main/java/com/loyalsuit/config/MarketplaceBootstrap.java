package com.loyalsuit.config;

import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Ensures the flagship marketplace store exists on startup. LoyalSuit is a single-marketplace
 * platform: this canonical tenant is the storefront everyone shops, and the store new sellers
 * join as vendors. Idempotent (find-or-create by slug) so it is safe on every boot and in every
 * environment — including tests, which build a fresh schema and have no Flyway seed to rely on.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketplaceBootstrap implements CommandLineRunner {

    private final MarketplaceProperties properties;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public void run(String... args) {
        tenantRepository.findBySlug(properties.getSlug()).ifPresentOrElse(
                existing -> log.debug("Flagship marketplace store '{}' present (id={})",
                        existing.getSlug(), existing.getId()),
                this::seedFlagship);
    }

    private void seedFlagship() {
        Tenant flagship = new Tenant(properties.getName(), properties.getSlug());
        flagship.setCurrency(properties.getCurrency());
        flagship.setActive(true);
        // Pre-onboarded: the flagship store is operator-run, never sent through the setup wizard.
        flagship.setOnboardedAt(Instant.now());
        Tenant saved = tenantRepository.save(flagship);
        log.info("Seeded flagship marketplace store '{}' (id={})", saved.getSlug(), saved.getId());
    }
}
