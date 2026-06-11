package com.loyalsuit.modules.privacy.domain.port;

import java.util.UUID;

/**
 * A module's handling of erasure (GDPR right-to-be-forgotten): delete or anonymise the personal
 * data it holds for a user. Records needed for financial/legal integrity (e.g. orders) keep the
 * row but scrub the personal fields; purely personal data (e.g. notifications) is deleted.
 */
public interface PersonalDataEraser {

    void erase(UUID userId, UUID tenantId);
}
