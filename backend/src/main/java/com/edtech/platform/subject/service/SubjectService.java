package com.edtech.platform.subject.service;

import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.dto.SubjectSummary;
import com.edtech.platform.subject.repository.SubjectRepository;
import com.edtech.platform.common.exception.BusinessException;
import com.edtech.platform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public Page<SubjectSummary> getPublicSubjects(String keyword, EducationLevel educationLevel, Pageable pageable) {
        Specification<Subject> spec = Specification.where(isActiveTrue());

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(hasKeyword(keyword));
        }

        if (educationLevel != null) {
            spec = spec.and(hasEducationLevel(educationLevel));
        }

        return subjectRepository.findAll(spec, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public SubjectSummary getPublicSubject(java.util.UUID id) {
        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        return toSummary(subject);
    }

    private Specification<Subject> isActiveTrue() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    private Specification<Subject> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("code")), likePattern)
            );
        };
    }

    private Specification<Subject> hasEducationLevel(EducationLevel educationLevel) {
        if (educationLevel == EducationLevel.ELEMENTARY || educationLevel == EducationLevel.PRIMARY_SCHOOL) {
            return (root, query, cb) -> cb.or(
                    cb.equal(root.get("educationLevel"), EducationLevel.ELEMENTARY),
                    cb.equal(root.get("educationLevel"), EducationLevel.PRIMARY_SCHOOL)
            );
        }
        return (root, query, cb) -> cb.equal(root.get("educationLevel"), educationLevel);
    }

    private SubjectSummary toSummary(Subject subject) {
        EducationLevel level = subject.getEducationLevel();
        if (level == EducationLevel.PRIMARY_SCHOOL) {
            level = EducationLevel.ELEMENTARY;
        }
        return new SubjectSummary(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getSlug(),
                level,
                subject.getDescription()
        );
    }
}
