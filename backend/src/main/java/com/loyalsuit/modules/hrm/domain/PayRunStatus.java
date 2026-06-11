package com.loyalsuit.modules.hrm.domain;

/** The lifecycle of a pay run: a draft can be regenerated and edited, then locked, then paid. */
public enum PayRunStatus {
    DRAFT,
    FINALISED,
    PAID
}
