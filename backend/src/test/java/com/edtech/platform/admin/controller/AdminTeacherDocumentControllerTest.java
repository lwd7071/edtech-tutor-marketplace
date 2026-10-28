package com.edtech.platform.admin.controller;

import com.edtech.platform.common.storage.CredentialProofStorage;
import com.edtech.platform.teacher.service.TeacherDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminTeacherDocumentControllerTest {
    @Test void proofReturnsPrivateBytesWithoutCaching() throws Exception {
        var service = mock(TeacherDocumentService.class);
        var id = UUID.randomUUID();
        when(service.adminProof(id)).thenReturn(new CredentialProofStorage.DownloadedFile("%PDF".getBytes(), "application/pdf"));
        var mvc = MockMvcBuilders.standaloneSetup(new AdminTeacherDocumentController(service)).build();
        mvc.perform(get("/api/admin/teacher-documents/" + id + "/proof"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentType("application/pdf")).andExpect(content().bytes("%PDF".getBytes()));
    }
}
