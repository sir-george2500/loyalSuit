package com.loyalsuit.modules.auth.application.dto;

import java.util.List;

/** The one-time recovery codes, shown once when 2FA is enabled. */
public record TwoFactorEnableResponse(List<String> recoveryCodes) {
}
