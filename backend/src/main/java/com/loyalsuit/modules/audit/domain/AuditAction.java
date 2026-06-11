package com.loyalsuit.modules.audit.domain;

/**
 * The set of auditable actions. Kept as a typed enum so the audit trail is a
 * controlled vocabulary, not free text. Add new values as new flows are audited.
 */
public enum AuditAction {
    USER_REGISTERED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    TWO_FACTOR_ENABLED,
    TWO_FACTOR_DISABLED,
    PERSONAL_DATA_EXPORTED,
    ACCOUNT_ANONYMISED,
    TENANT_ONBOARDED,
    PAYOUT_REQUESTED,
    PAYOUT_PAID,
    PAYOUT_REJECTED
}
