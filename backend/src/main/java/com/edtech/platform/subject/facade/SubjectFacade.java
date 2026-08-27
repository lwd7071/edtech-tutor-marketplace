package com.edtech.platform.subject.facade;

import com.edtech.platform.subject.facade.dto.SubjectSnapshot;

import java.util.UUID;
import java.util.Collection;
import java.util.Map;

public interface SubjectFacade {
    SubjectSnapshot getSubject(UUID subjectId);
    Map<UUID, SubjectSnapshot> getSubjects(Collection<UUID> subjectIds);
    boolean isSubjectActive(UUID subjectId);
}
