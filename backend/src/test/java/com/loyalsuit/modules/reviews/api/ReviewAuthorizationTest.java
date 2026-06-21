package com.loyalsuit.modules.reviews.api;

import com.loyalsuit.modules.marketplace.application.VendorService;
import com.loyalsuit.modules.reviews.application.ReviewService;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Writing a review needs any authenticated user (the delivered-purchase gate is in the
 * service); reading a product's reviews is public; the seller "Reviews" feed is VENDOR-only.
 * Services are mocked — this test pins the security wiring only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewAuthorizationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private ReviewService reviewService;
    @MockitoBean private VendorService vendorService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher vendorOnly(UserRole role) {
        return role == UserRole.VENDOR ? status().is2xxSuccessful() : status().isForbidden();
    }

    // ---- public product reviews ---------------------------------------------

    @Test
    void productReviews_arePublic() throws Exception {
        mvc.perform(get("/api/v1/store/products/" + UUID.randomUUID() + "/reviews"))
                .andExpect(status().is2xxSuccessful());
    }

    // ---- writing a review ---------------------------------------------------

    @Test
    void writeReview_isDeniedToAnonymous() throws Exception {
        // Stateless JWT security denies unauthenticated requests with 403 (no 401 entry point).
        mvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"rating\":5}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void writeReview_allowedForAnyAuthenticatedUser(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"rating\":5}"))
                .andExpect(status().is2xxSuccessful());
    }

    // ---- seller reviews feed ------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void sellerReviews_areVendorOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/vendor/reviews").header("Authorization", bearer(role)))
                .andExpect(vendorOnly(role));
    }
}
