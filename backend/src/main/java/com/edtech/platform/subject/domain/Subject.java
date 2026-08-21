package com.edtech.platform.subject.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE subjects SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Subject extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 50)
    private EducationLevel educationLevel;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_source", nullable = false, length = 20)
    private SubjectCreatedSource createdSource = SubjectCreatedSource.ADMIN;

    @Builder
    public Subject(String code, String name, String slug, EducationLevel educationLevel, String description, SubjectCreatedSource createdSource) {
        this.code = code;
        this.name = name;
        this.slug = slug;
        this.educationLevel = educationLevel;
        this.description = description;
        if (createdSource != null) {
            this.createdSource = createdSource;
        }
    }
}
