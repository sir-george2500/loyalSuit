package com.loyalsuit.modules.dashboard.application.dto;

/** Number of orders in a given status. */
public record StatusCount(String status, long count) {
}
