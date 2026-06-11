package com.loyalsuit.modules.privacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import com.loyalsuit.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GDPR rights end-to-end: export returns the profile + module sections; account deletion is
 * blocked for owners but anonymises an ordinary user, leaving no PII and no ability to sign in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataPrivacyFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private JsonNode register(String email) throws Exception {
        String body = """
                {"fullName":"Olive Owner","email":"%s","password":"Sup3rSecret!","businessName":"Olive Co"}
                """.formatted(email);
        String json = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).path("data");
    }

    @Test
    void export_returnsTheProfileAndModuleSections() throws Exception {
        JsonNode reg = register("export.me@test.dev");
        String token = reg.path("token").asText();

        String json = mvc.perform(get("/api/v1/privacy/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = mapper.readTree(json).path("data");

        assertThat(data.path("profile").path("email").asText()).isEqualTo("export.me@test.dev");
        assertThat(data.has("notifications")).isTrue();
        assertThat(data.has("orders")).isTrue();
    }

    @Test
    void deleteAccount_isForbiddenForOwners() throws Exception {
        String token = register("owner.delete@test.dev").path("token").asText();

        mvc.perform(post("/api/v1/privacy/delete-account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Sup3rSecret!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAccount_anonymisesAnOrdinaryUser() throws Exception {
        // Arrange — a customer in an existing tenant, with a token of their own
        UUID tenantId = UUID.fromString(register("store.owner@test.dev").path("user").path("tenantId").asText());
        AppUser customer = userRepository.save(new AppUser(
                tenantId, "casey@test.dev", passwordEncoder.encode("Pa55word!"), "Casey Clerk", UserRole.CUSTOMER));
        String token = jwtService.issueToken(customer.getId(), customer.getEmail(), "CUSTOMER", tenantId);

        // Act
        mvc.perform(post("/api/v1/privacy/delete-account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Pa55word!\"}"))
                .andExpect(status().isOk());

        // Assert — no PII remains and the account is deactivated
        AppUser after = userRepository.findById(customer.getId()).orElseThrow();
        assertThat(after.getEmail()).startsWith("deleted-").endsWith("@anonymized.invalid");
        assertThat(after.getFullName()).isEqualTo("Deleted user");
        assertThat(after.isActive()).isFalse();
    }
}
