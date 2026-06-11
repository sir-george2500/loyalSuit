package com.loyalsuit.modules.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalsuit.common.security.TotpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end TOTP 2FA: enrol, confirm, then a login that is held behind the second factor and
 * completed with a generated code. Also asserts the challenge token is not itself a credential.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TwoFactorFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private TotpService totp;

    private static final String EMAIL = "owner.2fa@test.dev";
    private static final String PASSWORD = "Sup3rSecret!";

    private JsonNode postJson(String url, String body, String bearer) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (bearer != null) {
            req = req.header("Authorization", "Bearer " + bearer);
        }
        String json = mvc.perform(req).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).path("data");
    }

    @Test
    void enrols_thenRequiresTheSecondFactorAtLogin() throws Exception {
        // Arrange — register a fresh owner and capture the token
        String register = """
                {"fullName":"Olive Owner","email":"%s","password":"%s","businessName":"Olive Co"}
                """.formatted(EMAIL, PASSWORD);
        String token = postJson("/api/v1/auth/register", register, null).path("token").asText();

        // Act — start 2FA setup and confirm it with a generated code
        String secret = postJson("/api/v1/auth/2fa/setup", "{}", token).path("secret").asText();
        assertThat(secret).isNotBlank();
        JsonNode enabled = postJson("/api/v1/auth/2fa/enable",
                "{\"code\":\"" + totp.currentCode(secret) + "\"}", token);
        assertThat(enabled.path("recoveryCodes")).hasSize(10);

        // Assert — a password login is now held behind the second factor
        JsonNode challenge = postJson("/api/v1/auth/login",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}", null);
        assertThat(challenge.path("mfaRequired").asBoolean()).isTrue();
        assertThat(challenge.path("token").isNull() || challenge.path("token").asText().isEmpty()).isTrue();
        String mfaToken = challenge.path("mfaToken").asText();
        assertThat(mfaToken).isNotBlank();

        // the challenge token alone cannot authenticate a request
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + mfaToken))
                .andExpect(status().is4xxClientError());

        // completing the challenge with a valid code yields a real token
        JsonNode completed = postJson("/api/v1/auth/login/2fa",
                "{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"" + totp.currentCode(secret) + "\"}", null);
        assertThat(completed.path("token").asText()).isNotBlank();
        assertThat(completed.path("mfaRequired").asBoolean()).isFalse();
    }

    @Test
    void rejectsLoginCompletion_withABadCode() throws Exception {
        // Arrange — register + enrol a second owner
        String email = "owner.2fa.bad@test.dev";
        String register = """
                {"fullName":"Otto Owner","email":"%s","password":"%s","businessName":"Otto Co"}
                """.formatted(email, PASSWORD);
        String token = postJson("/api/v1/auth/register", register, null).path("token").asText();
        String secret = postJson("/api/v1/auth/2fa/setup", "{}", token).path("secret").asText();
        postJson("/api/v1/auth/2fa/enable", "{\"code\":\"" + totp.currentCode(secret) + "\"}", token);

        JsonNode challenge = postJson("/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}", null);
        String mfaToken = challenge.path("mfaToken").asText();

        // Act & Assert — a wrong code is refused
        mvc.perform(post("/api/v1/auth/login/2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }
}
