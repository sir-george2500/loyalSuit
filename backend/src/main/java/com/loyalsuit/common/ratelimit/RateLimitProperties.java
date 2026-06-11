package com.loyalsuit.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunable rate-limit policy. Auth endpoints (login, register, password reset) get a tight
 * window to blunt credential stuffing; the rest of the API gets a looser per-client budget.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    /** Master switch; disabled in tests so the suite isn't throttled. */
    private boolean enabled = true;

    private int authLimit = 10;
    private int authWindowSeconds = 60;

    private int apiLimit = 150;
    private int apiWindowSeconds = 60;
}
