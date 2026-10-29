package com.edtech.platform.teacher.web;

import com.edtech.platform.auth.service.SessionService;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.security.AuthenticatedUser;
import com.edtech.platform.common.security.JwtTokenProvider;
import com.edtech.platform.common.security.UserStatusCacheService;
import com.edtech.platform.teacher.domain.DocumentType;
import com.edtech.platform.teacher.domain.VerificationStatus;
import com.edtech.platform.teacher.dto.TeacherDocumentView;
import com.edtech.platform.teacher.service.TeacherDocumentService;
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

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
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

@WebMvcTest(TeacherDocumentsPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherDocumentsPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TeacherDocumentService documents;
    @MockBean private JwtTokenProvider tokens;
    @MockBean private UserStatusCacheService statuses;
    @MockBean private SessionService sessions;
    @MockBean private Clock clock;
    private final UUID teacherId = UUID.randomUUID();
    private final UUID pendingId = UUID.randomUUID();
    private final UUID verifiedId = UUID.randomUUID();

    @BeforeEach
    void authenticateTeacher() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(teacherId, "teacher@example.test", "TEACHER"), null, List.of()));
        when(documents.getDocuments(teacherId)).thenReturn(List.of(
                new TeacherDocumentView(pendingId, DocumentType.IDENTITY, "Mặt trước CCCD", "https://private.example.test/proof", "image/jpeg", 1536000L, VerificationStatus.PENDING, null),
                new TeacherDocumentView(verifiedId, DocumentType.IDENTITY, "Giấy tờ đã duyệt", "https://private.example.test/verified", "application/pdf", 2048L, VerificationStatus.VERIFIED, Instant.parse("2026-10-28T12:00:00Z"))));
    }

    @Test
    void listsMetadataWithoutRenderingPrivateFileUrlAndSetsNoStore() throws Exception {
        mockMvc.perform(get("/teacher/documents"))
                .andExpect(status().isOk())
                .andExpect(view().name("workspace/teacher-documents"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mặt trước CCCD")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đang chờ xác minh")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã xác minh")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private.example.test"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("multipart/form-data")));
        verify(documents).getDocuments(teacherId);
    }

    @Test
    void uploadsIdentityDocumentWithCsrfAndRedirects() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "id-card.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/teacher/documents/upload").file(file).with(csrf()).param("title", "Giấy tờ định danh"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/documents"))
                .andExpect(flash().attribute("documentsNotice", "Đã tải giấy tờ lên để xác minh."));
        verify(documents).uploadDocument(teacherId, file, DocumentType.IDENTITY, "Giấy tờ định danh");
    }

    @Test
    void mapsOversizeAndUnsupportedFilesToSafeMessages() throws Exception {
        MockMultipartFile tooLarge = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[]{1});
        when(documents.uploadDocument(teacherId, tooLarge, DocumentType.IDENTITY, null))
                .thenThrow(new BusinessException(ErrorCode.FILE_TOO_LARGE));
        mockMvc.perform(multipart("/teacher/documents/upload").file(tooLarge).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("documentsError", "Tệp vượt quá giới hạn 10 MB."));
    }

    @Test
    void deletesPendingButDoesNotRenderDeleteActionForVerified() throws Exception {
        mockMvc.perform(post("/teacher/documents/{id}/delete", pendingId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/documents"));
        verify(documents).deleteDocument(teacherId, pendingId);
        mockMvc.perform(get("/teacher/documents"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đã khóa")));
        verify(documents, never()).deleteDocument(teacherId, verifiedId);
    }
}
