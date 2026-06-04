package com.loyalsuit.modules.tenants.application.event;

import com.loyalsuit.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the welcome email after a tenant completes onboarding. Runs only after the
 * onboarding transaction commits, and asynchronously on the mail pool, so email
 * latency or failure never touches the API response.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantOnboardedListener {

    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantOnboarded(TenantOnboardedEvent event) {
        String subject = "Welcome to LoyalSuit — " + event.businessName() + " is ready";
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#111827">Your store is live 🎉</h2>
                  <p>Hi there,</p>
                  <p><strong>%s</strong> is now set up on LoyalSuit. Your default currency is
                     <strong>%s</strong> and your first warehouse is ready to stock.</p>
                  <p>Next, add your first products and start selling.</p>
                  <p style="margin:24px 0">
                    <a href="%s/admin/dashboard"
                       style="background:#4f46e5;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none">
                       Go to your dashboard
                    </a>
                  </p>
                  <p style="color:#6b7280;font-size:13px">If you didn't create this account, you can ignore this email.</p>
                </div>
                """.formatted(event.businessName(), event.currency(), frontendUrl);

        emailService.sendHtml(event.adminEmail(), subject, html);
    }
}
