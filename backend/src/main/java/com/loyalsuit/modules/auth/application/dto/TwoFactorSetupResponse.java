package com.loyalsuit.modules.auth.application.dto;

/** The secret and provisioning URI to show (as text + QR) when starting 2FA enrolment. */
public record TwoFactorSetupResponse(String secret, String otpauthUri) {
}
