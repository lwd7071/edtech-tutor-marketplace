package com.edtech.platform.subject.facade;

import com.edtech.platform.subject.facade.dto.SubjectSnapshot;

import java.util.UUID;

public interface SubjectFacade {
    SubjectSnapshot getSubject(UUID subjectId);
    boolean isSubjectActive(UUID subjectId);
}
