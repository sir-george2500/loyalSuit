package com.loyalsuit.modules.privacy.domain.port;

import java.util.UUID;

/**
 * A module's contribution to a user's personal-data export (GDPR right-to-access). Each module
 * that holds personal data implements one, so the privacy module never has to depend on them.
 */
public interface PersonalDataContributor {

    /** The key this section appears under in the export (e.g. "orders", "notifications"). */
    String section();

    /** The user's data held by this module, as a serialisable value. */
    Object export(UUID userId, UUID tenantId);
}
