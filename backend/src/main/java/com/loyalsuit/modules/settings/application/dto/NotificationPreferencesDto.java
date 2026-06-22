package com.loyalsuit.modules.settings.application.dto;

/** Notification toggles — used for both reading and updating a tenant's preferences. */
public record NotificationPreferencesDto(
        boolean orderConfirmationEmail,
        boolean orderStatusEmail,
        boolean lowStockAlert,
        boolean newReviewAlert,
        boolean payoutAlert,
        boolean marketingEmail) {}
