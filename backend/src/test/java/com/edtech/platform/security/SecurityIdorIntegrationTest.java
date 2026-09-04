package com.edtech.platform.security;

import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Task 9.1 - Security & IDOR Hardening Integration Test")
@AutoConfigureMockMvc
class SecurityIdorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String teacher1Token;
    private String teacher2Token;
    private String student1Token;
    private String student2Token;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                new AuthenticatedUser(UUID.fromString("a0000000-0000-0000-0000-000000000001"), "admin@edtech.vn", "ADMIN")
        );
        teacher1Token = "Bearer " + jwtTokenProvider.generateAccessToken(
                new AuthenticatedUser(UUID.fromString("b0000000-0000-0000-0000-000000000001"), "teacher.math@edtech.vn", "TEACHER")
        );
        teacher2Token = "Bearer " + jwtTokenProvider.generateAccessToken(
                new AuthenticatedUser(UUID.fromString("b0000000-0000-0000-0000-000000000002"), "teacher.english@edtech.vn", "TEACHER")
        );
        student1Token = "Bearer " + jwtTokenProvider.generateAccessToken(
                new AuthenticatedUser(UUID.fromString("10000000-0000-0000-0000-000000000001"), "student.an@edtech.vn", "STUDENT")
        );
        student2Token = "Bearer " + jwtTokenProvider.generateAccessToken(
                new AuthenticatedUser(UUID.fromString("10000000-0000-0000-0000-000000000002"), "student.binh@edtech.vn", "STUDENT")
        );
    }

    @Test
    @DisplayName("IDOR: Student 2 cannot access Student 1's package")
    void idor_student2CannotAccessStudent1Package() throws Exception {
        UUID student1PackageId = UUID.fromString("40000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/student/packages/" + student1PackageId)
                        .header("Authorization", student2Token))
                .andExpect(status().isNotFound()); // Or 403 / 404 masked
    }

    @Test
    @DisplayName("IDOR: Student 2 cannot access Student 1's invoice")
    void idor_student2CannotAccessStudent1Invoice() throws Exception {
        UUID student1InvoiceId = UUID.fromString("50000000-0000-0000-0000-000000000001");

        mockMvc.perform(get("/api/student/invoices/" + student1InvoiceId)
                        .header("Authorization", student2Token))
                .andExpect(status().isNotFound()); // Or 403 / 404 masked
    }

    @Test
    @DisplayName("IDOR: Teacher 2 cannot complete Teacher 1's booking")
    void idor_teacher2CannotCompleteTeacher1Booking() throws Exception {
        UUID teacher1BookingId = UUID.fromString("60000000-0000-0000-0000-000000000002");

        mockMvc.perform(post("/api/teacher/bookings/" + teacher1BookingId + "/complete")
                        .header("Authorization", teacher2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"report\":{\"content\":\"Lesson done\",\"feedback\":\"Good\",\"teacherSelfRating\":5}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("IDOR: Teacher 2 cannot cancel Teacher 1's booking")
    void idor_teacher2CannotCancelTeacher1Booking() throws Exception {
        UUID teacher1BookingId = UUID.fromString("60000000-0000-0000-0000-000000000002");

        mockMvc.perform(post("/api/teacher/bookings/" + teacher1BookingId + "/cancel")
                        .header("Authorization", teacher2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"reason\":\"Cancel by teacher\",\"initiatedBy\":\"TEACHER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("IDOR: Teacher 2 cannot delete Teacher 1's bank account")
    void idor_teacher2CannotDeleteTeacher1BankAccount() throws Exception {
        UUID teacher1BankId = UUID.fromString("30000000-0000-0000-0000-000000000001");

        mockMvc.perform(delete("/api/teacher/bank-accounts/" + teacher1BankId)
                        .header("Authorization", teacher2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RBAC: Student cannot access Admin Dashboard")
    void rbac_studentCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", student1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Student cannot access Admin Platform Settings")
    void rbac_studentCannotAccessAdminSettings() throws Exception {
        mockMvc.perform(get("/api/admin/settings")
                        .header("Authorization", student1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Teacher cannot access Admin Payouts")
    void rbac_teacherCannotAccessAdminPayouts() throws Exception {
        mockMvc.perform(get("/api/admin/payout-requests")
                        .header("Authorization", teacher1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Teacher cannot access Admin Refunds")
    void rbac_teacherCannotAccessAdminRefunds() throws Exception {
        mockMvc.perform(get("/api/admin/refund-requests")
                        .header("Authorization", teacher2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Anonymous user cannot access Student Packages")
    void rbac_anonymousCannotAccessStudentPackages() throws Exception {
        mockMvc.perform(get("/api/student/packages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RBAC: Admin has full access to Admin Dashboard")
    void rbac_adminHasAccessToAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }
}

