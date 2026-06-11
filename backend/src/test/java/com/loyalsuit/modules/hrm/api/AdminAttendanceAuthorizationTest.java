package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.modules.hrm.application.AttendanceService;
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

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HRM attendance is owner-only (SUPER_ADMIN / TENANT_ADMIN); the plan gate is enforced
 * separately in the service. With the service mocked, this asserts the authorization wall only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAttendanceAuthorizationTest {

    private static final Set<UserRole> OWNERS = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AttendanceService attendanceService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void recordAttendance(UserRole role) throws Exception {
        String body = "{\"employeeId\":\"" + UUID.randomUUID() + "\",\"workDate\":\"2026-06-01\",\"status\":\"PRESENT\"}";
        mvc.perform(post("/api/v1/admin/attendance")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void clockIn(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/admin/attendance/" + UUID.randomUUID() + "/clock-in")
                        .header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void timesheet(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/attendance/timesheet")
                        .param("employeeId", UUID.randomUUID().toString())
                        .param("from", "2026-06-01").param("to", "2026-06-30")
                        .header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/attendance/timesheet")
                        .param("employeeId", UUID.randomUUID().toString())
                        .param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().is4xxClientError());
    }
}
