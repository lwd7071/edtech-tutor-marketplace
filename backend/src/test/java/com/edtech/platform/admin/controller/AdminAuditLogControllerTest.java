package com.edtech.platform.admin.controller;

import com.edtech.platform.admin.domain.AuditAction;
import com.edtech.platform.admin.dto.response.AuditLogView;
import com.edtech.platform.admin.service.AuditLogQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditLogQueryService auditLogQueryService;

    @InjectMocks
    private AdminAuditLogController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAuditLogs_shouldReturn200() throws Exception {
        UUID logId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        AuditLogView view = new AuditLogView(
                logId, actorId, AuditAction.TEACHER_APPROVED, "TEACHER_PROFILE", targetId,
                Map.of("status", "PENDING_APPROVAL"), Map.of("status", "APPROVED"),
                "127.0.0.1", "Mozilla/5.0", Instant.now()
        );

        when(auditLogQueryService.findAuditLogs(eq(actorId), eq(AuditAction.TEACHER_APPROVED), eq("TEACHER_PROFILE"), any()))
                .thenReturn(new PageImpl<>(List.of(view), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("actorId", actorId.toString())
                        .param("action", "TEACHER_APPROVED")
                        .param("targetType", "TEACHER_PROFILE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].action").value("TEACHER_APPROVED"))
                .andExpect(jsonPath("$.data[0].targetType").value("TEACHER_PROFILE"))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }
}