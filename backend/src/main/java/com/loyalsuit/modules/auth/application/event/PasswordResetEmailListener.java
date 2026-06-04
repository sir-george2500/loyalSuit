package com.loyalsuit.modules.auth.application.event;

import com.loyalsuit.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends password-reset emails after the triggering transaction commits and off the
 * request thread, so mail latency or failure never affects the API response.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetEmailListener {

    private final EmailService emailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(PasswordResetRequestedEvent event) {
        String greeting = event.name() != null && !event.name().isBlank() ? event.name() : "there";
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#111827">Reset your password</h2>
                  <p>Hi %s,</p>
                  <p>We received a request to reset your LoyalSuit password. Click below to choose
                     a new one. This link expires shortly and can be used once.</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#4f46e5;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none">
                       Reset password
                    </a>
                  </p>
                  <p style="color:#6b7280;font-size:13px">If you didn't request this, you can safely
                     ignore this email — your password won't change.</p>
                </div>
                """.formatted(greeting, event.resetLink());
        emailService.sendHtml(event.email(), "Reset your LoyalSuit password", html);
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(PasswordResetCompletedEvent event) {
        String greeting = event.name() != null && !event.name().isBlank() ? event.name() : "there";
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#111827">Your password was changed</h2>
                  <p>Hi %s,</p>
                  <p>This is a confirmation that your LoyalSuit password was just reset.</p>
                  <p style="color:#b91c1c;font-size:13px">If this wasn't you, contact support immediately —
                     your account may be compromised.</p>
                </div>
                """.formatted(greeting);
        emailService.sendHtml(event.email(), "Your LoyalSuit password was changed", html);
    }
}
