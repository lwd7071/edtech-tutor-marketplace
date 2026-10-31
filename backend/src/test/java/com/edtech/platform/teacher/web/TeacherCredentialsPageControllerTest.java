package com.edtech.platform.teacher.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.domain.CredentialStatus;
import com.edtech.platform.teacher.dto.TeacherCredentialView;
import com.edtech.platform.teacher.service.TeacherCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TeacherCredentialsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherCredentialsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherCredentialService credentials;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();
    private final UUID credentialId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(credentials.list(teacherId)).thenReturn(List.of(
                new TeacherCredentialView(credentialId, "Chứng chỉ sư phạm", CredentialStatus.APPROVED,
                        "/api/teacher/credentials/" + credentialId + "/proof", null,
                        Instant.parse("2026-10-28T12:00:00Z"), 4L)));
    }

    @Test
    void rendersStatusAndInternalProofRouteWithoutSerializingApiProofUrl() throws Exception {
        mockMvc.perform(get("/teacher/credentials"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-credentials"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã duyệt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tên mục đang hiển thị")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/teacher/credentials/" + credentialId + "/proof")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/api/teacher/credentials/"))));
    }

    @Test
    void renderedCreateFormCarriesCsrfToken() throws Exception {
        String html = mockMvc.perform(get("/teacher/credentials").with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int actionIndex = html.indexOf("action=\"/teacher/credentials\"");
        int formStart = html.lastIndexOf("<form", actionIndex);
        int formEnd = html.indexOf("</form>", actionIndex);

        assertTrue(actionIndex >= 0 && formStart >= 0 && formEnd > actionIndex,
                "create credential form should be present in the rendered page");
        assertTrue(html.substring(formStart, formEnd).contains("name=\"_csrf\""),
                "create credential form should include its CSRF field");
    }

    @Test
    void editPageCarriesVersionAndWarnsAboutApprovedVisibility() throws Exception {
        mockMvc.perform(get("/teacher/credentials").param("edit", credentialId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\" value=\"4\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sẽ tạm ẩn khỏi hồ sơ công khai")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bỏ trống file nếu giữ file hiện tại")));
    }

    @Test
    void createNeedsCsrfAndUsesPostRedirectGet() throws Exception {
        MockMultipartFile proof = new MockMultipartFile("proof", "degree.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(credentials.create(teacherId, "Bằng cử nhân", proof)).thenReturn(null);
        mockMvc.perform(multipart("/teacher/credentials").file(proof).with(csrf()).param("label", "Bằng cử nhân"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/credentials"))
                .andExpect(flash().attribute("credentialNotice", "Đã gửi minh chứng để xét duyệt."));
        verify(credentials).create(teacherId, "Bằng cử nhân", proof);
    }

    @Test
    void createValidationKeepsLabelAndDoesNotCallService() throws Exception {
        MockMultipartFile proof = new MockMultipartFile("proof", "degree.pdf", "application/pdf", new byte[]{1});
        mockMvc.perform(multipart("/teacher/credentials").file(proof).with(csrf()).param("label", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-credentials"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập tên chứng chỉ hoặc bằng cấp")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"label\" value=\" \"")));
    }

    @Test
    void updateSendsExpectedVersionAndOptionalReplacementFile() throws Exception {
        MockMultipartFile proof = new MockMultipartFile("proof", "new.pdf", "application/pdf", new byte[]{1});
        when(credentials.update(teacherId, credentialId, "Bằng sư phạm", proof, 4L)).thenReturn(null);
        mockMvc.perform(multipart("/teacher/credentials/{id}/update", credentialId).file(proof).with(csrf())
                        .param("label", "Bằng sư phạm").param("version", "4"))
                .andExpect(status().is3xxRedirection());
        verify(credentials).update(teacherId, credentialId, "Bằng sư phạm", proof, 4L);
    }

    @Test
    void conflictShowsSafeErrorAndPreservesEditForm() throws Exception {
        when(credentials.update(teacherId, credentialId, "Đã sửa tên", null, 3L))
                .thenThrow(new BusinessException(ErrorCode.CONCURRENT_MODIFICATION));
        mockMvc.perform(multipart("/teacher/credentials/{id}/update", credentialId).with(csrf())
                        .param("label", "Đã sửa tên").param("version", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("đã được cập nhật ở nơi khác")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Đ&atilde; sửa t&ecirc;n\"")));
    }

    @Test
    void deleteUsesVersionedPostAndRedirect() throws Exception {
        mockMvc.perform(post("/teacher/credentials/{id}/delete", credentialId).with(csrf()).param("version", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/credentials"));
        verify(credentials).delete(teacherId, credentialId, 4L);
    }

    @Test
    void proofProxyReturnsBytesNoStoreAndInlineMime() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3, 4};
        when(credentials.proof(teacherId, credentialId)).thenReturn(new CredentialProofStorage.DownloadedFile(bytes, "application/pdf"));
        mockMvc.perform(get("/teacher/credentials/{id}/proof", credentialId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "inline"))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(bytes));
        verify(credentials).proof(teacherId, credentialId);
    }
}
