package com.loyalsuit.modules.pos.domain;

/** A cash-drawer session is OPEN while a cashier is ringing up, then CLOSED once counted. */
public enum PosShiftStatus {
    OPEN,
    CLOSED
}
