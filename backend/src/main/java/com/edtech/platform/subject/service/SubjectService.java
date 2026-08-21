package com.edtech.platform.subject.service;

import com.edtech.platform.subject.domain.EducationLevel;
import com.edtech.platform.subject.domain.Subject;
import com.edtech.platform.subject.dto.SubjectSummary;
import com.edtech.platform.subject.repository.SubjectRepository;
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
    @Cacheable(value = "SUBJECT_ACTIVE_LIST", key = "(#keyword == null ? 'ALL' : #keyword) + '_' + (#educationLevel == null ? 'ALL' : #educationLevel.name()) + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
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
        return (root, query, cb) -> cb.equal(root.get("educationLevel"), educationLevel);
    }

    private SubjectSummary toSummary(Subject subject) {
        return new SubjectSummary(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getSlug(),
                subject.getEducationLevel(),
                subject.getDescription()
        );
    }
}
