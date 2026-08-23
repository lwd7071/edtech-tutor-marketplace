package com.edtech.platform.common.service;

import com.edtech.platform.auth.facade.IdentityFacade;
import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.domain.Attachment;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.repository.AttachmentRepository;
import com.edtech.platform.common.storage.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private IdentityFacade identityFacade;

    @Mock
    private FileStoragePort fileStoragePort;

    @InjectMocks
    private AttachmentService attachmentService;

    private UUID testOwnerId;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        testOwnerId = UUID.randomUUID();
        // A valid dummy PDF file for Tika to detect properly
        validFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", 
                "%PDF-1.4\n%EOF\n".getBytes()
        );
    }

    @Test
    void uploadAttachment_ThrowsNotFound_WhenOwnerDoesNotExist() throws IOException {
        when(identityFacade.existsById(testOwnerId)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            attachmentService.uploadAttachment(testOwnerId, AttachableType.MESSAGE, validFile);
        });

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verify(fileStoragePort, never()).upload(any(), any());
    }

    @Test
    void uploadAttachment_Success() throws IOException {
        when(identityFacade.existsById(testOwnerId)).thenReturn(true);
        FileStoragePort.UploadResult uploadResult = new FileStoragePort.UploadResult("public_id", "https://secure.url", "application/pdf", 10L);
        when(fileStoragePort.upload(any(), any())).thenReturn(uploadResult);
        
        Attachment savedAttachment = Attachment.builder()
                .ownerId(testOwnerId)
                .attachableType(AttachableType.MESSAGE)
                .attachableId(UUID.randomUUID())
                .cloudinaryPublicId("public_id")
                .secureUrl("https://secure.url")
                .originalFilename("test.pdf")
                .mimeType("application/pdf")
                .fileSize(10L)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(savedAttachment, "id", UUID.randomUUID());
        
        when(attachmentRepository.saveAndFlush(any(Attachment.class))).thenReturn(savedAttachment);

        AttachmentView result = attachmentService.uploadAttachment(testOwnerId, AttachableType.MESSAGE, validFile);

        assertNotNull(result);
        assertEquals("https://secure.url", result.getSecureUrl());
        
        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).saveAndFlush(attachmentCaptor.capture());
        
        Attachment captured = attachmentCaptor.getValue();
        assertEquals(testOwnerId, captured.getOwnerId());
    }
}
