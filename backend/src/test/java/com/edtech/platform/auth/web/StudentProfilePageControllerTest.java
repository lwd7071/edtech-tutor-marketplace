package com.edtech.platform.auth.web;

import com.edtech.platform.auth.dto.request.UpdateParentContactRequest;
import com.edtech.platform.auth.dto.response.ParentContactResponse;
import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.auth.facade.dto.IdentitySnapshot;
import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.auth.service.ParentContactService;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Clock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StudentProfilePageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentProfilePageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ParentContactService parentContacts;
    @MockBean private IdentityFacade identities;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserStatusCacheService userStatusCacheService;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(studentId, "student@example.test", "STUDENT"), null, List.of()));
    }

    @Test
    void rendersAccountAndParentContactAsSsrWithoutJavascript() throws Exception {
        when(identities.getIdentity(eq(studentId))).thenReturn(Optional.of(new IdentitySnapshot(
                studentId, "student@example.test", "Nguyễn Minh An", "STUDENT", "ACTIVE",
                null, false, null)));
        when(parentContacts.get(eq(studentId))).thenReturn(new ParentContactResponse(
                "Nguyễn Thị Mai", "0901234567", "parent@example.test", true, Instant.parse("2026-10-28T00:00:00Z")));

        mockMvc.perform(get("/student/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Minh An")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Thị Mai")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("parent@example.test")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/student/profile\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
    }

    @Test
    void showsRetryStateWhenContactReadFails() throws Exception {
        when(parentContacts.get(eq(studentId))).thenThrow(new IllegalStateException("unavailable"));

        mockMvc.perform(get("/student/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa tải được thông tin liên hệ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/student/profile\"")));
    }

    @Test
    void invalidFormKeepsValuesAndDoesNotUpdateService() throws Exception {
        mockMvc.perform(post("/student/profile")
                        .param("parentFullName", "A")
                        .param("parentPhone", "123")
                        .param("parentEmail", "not-an-email")
                        .param("notifyParent", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập số điện thoại gồm 10 hoặc 11 chữ số")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Địa chỉ email chưa đúng định dạng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"not-an-email\"")));

        verify(parentContacts, never()).update(eq(studentId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validPostUpdatesOwnedContactAndUsesPrg() throws Exception {
        when(parentContacts.update(eq(studentId), org.mockito.ArgumentMatchers.any(UpdateParentContactRequest.class)))
                .thenReturn(new ParentContactResponse("Phụ huynh", "0901234567", "parent@example.test", true,
                        Instant.parse("2026-10-28T00:00:00Z")));

        mockMvc.perform(post("/student/profile")
                        .param("parentFullName", "  Phụ huynh  ")
                        .param("parentPhone", "0901234567")
                        .param("parentEmail", "parent@example.test")
                        .param("notifyParent", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/profile"));

        verify(parentContacts).update(eq(studentId), eq(new UpdateParentContactRequest(
                "Phụ huynh", "0901234567", "parent@example.test", true)));
    }
}
