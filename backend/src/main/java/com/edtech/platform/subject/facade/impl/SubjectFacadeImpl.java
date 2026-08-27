package com.edtech.platform.subject.facade.impl;

import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.facade.SubjectFacade;
import com.edtech.platform.subject.facade.dto.SubjectSnapshot;
import com.edtech.platform.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectFacadeImpl implements SubjectFacade {

    private final SubjectRepository subjectRepository;

    @Override
    public SubjectSnapshot getSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        return new SubjectSnapshot(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getEducationLevel() != null ? subject.getEducationLevel().name() : null,
                subject.isActive()
        );
    }

    @Override
    public Map<UUID, SubjectSnapshot> getSubjects(Collection<UUID> subjectIds) {
        return subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, this::toSnapshot));
    }

    private SubjectSnapshot toSnapshot(Subject subject) {
        return new SubjectSnapshot(subject.getId(), subject.getCode(), subject.getName(),
                subject.getEducationLevel() != null ? subject.getEducationLevel().name() : null,
                subject.isActive());
    }

    @Override
    public boolean isSubjectActive(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .map(Subject::isActive)
                .orElse(false);
    }
}
