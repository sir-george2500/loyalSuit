package com.loyalsuit.config;

import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds one user per role into the flagship LoyalSuit marketplace so the app is usable out of the
 * box in development — including an admin who can approve vendor applications. Runs only under the
 * "dev" profile and is idempotent. (The marketplace tenant itself is ensured by MarketplaceBootstrap.)
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "Admin@Test123";

    private final MarketplaceProperties marketplaceProperties;
    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private record SeedUser(String email, String fullName, UserRole role) {}

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("superadmin@loyalsuit.dev", "Super Admin", UserRole.SUPER_ADMIN),
            new SeedUser("tenantadmin@loyalsuit.dev", "Tenant Admin", UserRole.TENANT_ADMIN),
            new SeedUser("vendor@loyalsuit.dev", "Vendor User", UserRole.VENDOR),
            new SeedUser("customer@loyalsuit.dev", "Test Customer", UserRole.CUSTOMER),
            new SeedUser("staff@loyalsuit.dev", "Staff Member", UserRole.STAFF)
    );

    @Override
    @Transactional
    public void run(String... args) {
        // Seed users into the flagship marketplace (find-or-create — order-independent of the bootstrap).
        Tenant tenant = tenantRepository.findBySlug(marketplaceProperties.getSlug())
                .orElseGet(() -> tenantRepository.save(
                        new Tenant(marketplaceProperties.getName(), marketplaceProperties.getSlug())));

        // The flagship store ships pre-configured, so seeded owner accounts land on the
        // dashboard instead of the setup wizard. Idempotent.
        if (!tenant.isOnboarded()) {
            tenant.setCurrency("USD");
            tenant.setTimezone("UTC");
            tenant.setOnboardedAt(java.time.Instant.now());
            tenant = tenantRepository.save(tenant);
        }

        String hash = passwordEncoder.encode(DEFAULT_PASSWORD);
        int created = 0;

        for (SeedUser seed : SEED_USERS) {
            if (userRepository.existsByEmail(seed.email())) {
                continue;
            }
            userRepository.save(new AppUser(
                    tenant.getId(), seed.email(), hash, seed.fullName(), seed.role()));
            created++;
        }

        if (created > 0) {
            log.info("DevDataSeeder: created {} test user(s). Login password: {}", created, DEFAULT_PASSWORD);
        } else {
            log.info("DevDataSeeder: test users already present, nothing to seed.");
        }
    }
}
