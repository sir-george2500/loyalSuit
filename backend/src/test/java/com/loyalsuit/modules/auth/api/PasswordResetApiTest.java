package com.loyalsuit.modules.auth.api;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.auth.application.AuthService;
import com.loyalsuit.modules.auth.application.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The forgot/reset endpoints must be reachable without authentication, and must not
 * leak whether an account exists. The service is mocked; behavior is unit-tested
 * separately in {@code PasswordResetServiceTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    // AuthService is a collaborator of AuthController; mock it so no real auth wiring runs.
    @MockitoBean
    private AuthService authService;

    @Test
    void forgotPassword_isPublic_andAlwaysSucceeds() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"someone@acme.dev\"}"))
                .andExpect(status().isOk());

        verify(passwordResetService).requestReset(eq("someone@acme.dev"));
    }

    @Test
    void forgotPassword_rejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPassword_isPublic_andSucceedsForValidToken() throws Exception {
        mvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"newPassword\":\"NewPass@1\"}"))
                .andExpect(status().isOk());

        verify(passwordResetService).reset(eq("abc"), eq("NewPass@1"));
    }

    @Test
    void resetPassword_returnsBadRequestForInvalidToken() throws Exception {
        doThrow(new BusinessException("This reset link is invalid or has expired", HttpStatus.BAD_REQUEST))
                .when(passwordResetService).reset(Mockito.anyString(), Mockito.anyString());

        mvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad\",\"newPassword\":\"NewPass@1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_rejectsWeakPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"newPassword\":\"weak\"}"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(passwordResetService);
    }
}
