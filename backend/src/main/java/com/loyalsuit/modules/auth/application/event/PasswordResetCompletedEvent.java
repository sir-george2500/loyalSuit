package com.loyalsuit.modules.auth.application.event;

/** Raised after a password is successfully reset, to send a confirmation email. */
public record PasswordResetCompletedEvent(String email, String name) {
}
