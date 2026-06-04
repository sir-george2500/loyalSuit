package com.loyalsuit.modules.auth.application.event;

/**
 * Raised when a user asks to reset their password. The raw token lives only in
 * {@code resetLink} (never persisted). Consumed after commit to send the email.
 */
public record PasswordResetRequestedEvent(String email, String name, String resetLink) {
}
