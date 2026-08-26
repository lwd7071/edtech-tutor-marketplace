package com.edtech.platform.subject.service;

import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.ProposalStatus;
import com.edtech.platform.subject.domain.SubjectProposal;
import com.edtech.platform.subject.dto.CreateSubjectProposalRequest;
import com.edtech.platform.subject.dto.SubjectProposalView;
import com.edtech.platform.subject.repository.SubjectProposalRepository;
import com.edtech.platform.teacher.domain.ProfileStatus;
import com.edtech.platform.teacher.facade.TeacherFacade;
import com.edtech.platform.teacher.facade.dto.TeacherSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubjectProposalServiceTest {

    @Mock
    private SubjectProposalRepository subjectProposalRepository;

    @Mock
    private TeacherFacade teacherFacade;

    @InjectMocks
    private SubjectProposalService subjectProposalService;

    private UUID userId;
    private UUID teacherId;
    private TeacherSnapshot teacherSnapshot;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        teacherSnapshot = new TeacherSnapshot(teacherId, userId, "APPROVED", true, true, null, null, null, 0, true, false, java.util.List.of(), null, null);
    }

    @Test
    void createProposal_Success() {
        when(teacherFacade.getTeacherByUserId(userId)).thenReturn(teacherSnapshot);
        
        CreateSubjectProposalRequest request = new CreateSubjectProposalRequest(
                "Lý thuyết chuỗi",
                EducationLevel.UNIVERSITY,
                "Môn học chuyên ngành vật lý"
        );

        SubjectProposal savedProposal = mock(SubjectProposal.class);
        when(savedProposal.getId()).thenReturn(UUID.randomUUID());
        when(savedProposal.getProposedName()).thenReturn("Lý thuyết chuỗi");
        when(savedProposal.getEducationLevel()).thenReturn(EducationLevel.UNIVERSITY);
        when(savedProposal.getStatus()).thenReturn(ProposalStatus.PENDING);
        
        when(subjectProposalRepository.save(any(SubjectProposal.class))).thenReturn(savedProposal);

        SubjectProposalView result = subjectProposalService.createProposal(userId, request);
        
        assertNotNull(result);
        assertEquals("Lý thuyết chuỗi", result.proposedName());
    }
}
