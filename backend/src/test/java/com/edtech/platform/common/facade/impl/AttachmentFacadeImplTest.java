package com.edtech.platform.common.facade.impl;

import com.edtech.platform.common.domain.AttachableType;
import com.edtech.platform.common.domain.Attachment;
import com.edtech.platform.common.dto.response.AttachmentView;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.common.repository.AttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentFacadeImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @InjectMocks
    private AttachmentFacadeImpl attachmentFacade;

    private UUID attachmentId;
    private UUID ownerId;
    private UUID attachableId;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        attachmentId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        attachableId = UUID.randomUUID();
        
        attachment = Attachment.builder()
                .ownerId(ownerId)
                .attachableType(AttachableType.MESSAGE)
                .originalFilename("test.pdf")
                .secureUrl("https://example.com/test.pdf")
                .mimeType("application/pdf")
                .fileSize(1024L)
                .build();
        
        // Simulating a pending attachment (attachableId == attachmentId)
        ReflectionTestUtils.setField(attachment, "id", attachmentId);
        attachment.setAttachableId(attachmentId);
    }

    @Test
    void validateAndBind_ShouldSucceed_WhenValidPendingAttachment() {
        // Arrange
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        // Act
        attachmentFacade.validateAndBind(attachmentId, ownerId, "MESSAGE", attachableId);

        // Assert
        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertEquals(attachableId, captor.getValue().getAttachableId());
    }

    @Test
    void validateAndBind_ShouldThrowException_WhenAttachmentNotFound() {
        // Arrange
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> attachmentFacade.validateAndBind(attachmentId, ownerId, "MESSAGE", attachableId));
        assertEquals(ErrorCode.ATTACHMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void validateAndBind_ShouldThrowException_WhenOwnerMismatch() {
        // Arrange
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        UUID wrongOwnerId = UUID.randomUUID();

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> attachmentFacade.validateAndBind(attachmentId, wrongOwnerId, "MESSAGE", attachableId));
        assertEquals(ErrorCode.ATTACHMENT_CONTEXT_INVALID, ex.getErrorCode());
    }

    @Test
    void validateAndBind_ShouldThrowException_WhenTypeMismatch() {
        // Arrange
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> attachmentFacade.validateAndBind(attachmentId, ownerId, "ASSIGNMENT", attachableId));
        assertEquals(ErrorCode.ATTACHMENT_CONTEXT_INVALID, ex.getErrorCode());
    }

    @Test
    void validateAndBind_ShouldSucceed_WhenAlreadyBoundToSameId() {
        // Arrange
        // Simulate already bound attachment to the exact same attachableId
        attachment.setAttachableId(attachableId); 
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        // Act
        attachmentFacade.validateAndBind(attachmentId, ownerId, "MESSAGE", attachableId);

        // Assert
        // The method should return without throwing any exception
        verify(attachmentRepository, never()).save(any(Attachment.class));
    }

    @Test
    void validateAndBind_ShouldThrowException_WhenAlreadyBoundToDifferentId() {
        // Arrange
        // Simulate already bound attachment (attachableId != attachmentId)
        attachment.setAttachableId(UUID.randomUUID()); 
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> attachmentFacade.validateAndBind(attachmentId, ownerId, "MESSAGE", attachableId));
        assertEquals(ErrorCode.ATTACHMENT_CONTEXT_INVALID, ex.getErrorCode());
    }

    @Test
    void getAttachmentView_ShouldReturnView_WhenAttachmentExists() {
        // Arrange
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        // Act
        AttachmentView view = attachmentFacade.getAttachmentView(attachmentId);

        // Assert
        assertNotNull(view);
        assertEquals(attachmentId, view.getId());
        assertEquals("https://example.com/test.pdf", view.getSecureUrl());
        assertEquals("test.pdf", view.getOriginalFilename());
        assertEquals("application/pdf", view.getMimeType());
        assertEquals(1024L, view.getFileSize());
    }
}
