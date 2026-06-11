package com.loyalsuit.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The rate-limit filter is off in the test profile; this class re-enables it with a tiny auth
 * budget and verifies the throttle fires with a 429 + Retry-After once the budget is spent.
 */
@SpringBootTest(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.auth-limit=3",
        "app.rate-limit.auth-window-seconds=60",
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitFilterIntegrationTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String BODY = "{\"email\":\"nobody@test.dev\",\"password\":\"wrong-password\"}";

    @Autowired
    private MockMvc mvc;

    private int login() throws Exception {
        return mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void throttlesAuthEndpoints_onceTheBudgetIsSpent() throws Exception {
        // Arrange / Act — spend the budget of 3 (bad credentials, never 429 yet)
        for (int i = 0; i < 3; i++) {
            assertThat(login()).isNotEqualTo(429);
        }

        // Assert — the next request is throttled, with a Retry-After
        MvcResult throttled = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andReturn();
        assertThat(throttled.getResponse().getStatus()).isEqualTo(429);
        assertThat(throttled.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }
}
