package com.edtech.platform.admin.dto.request;

import com.edtech.platform.subject.facade.dto.SubjectResolutionCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ApproveSubjectProposalRequest(
        @NotNull SubjectResolutionCommand.Resolution resolution,
        UUID existingSubjectId,
        @Size(max = 50) String code,
        @Size(max = 150) String name,
        @Pattern(regexp = "ELEMENTARY|MIDDLE_SCHOOL|HIGH_SCHOOL|UNIVERSITY|OTHER") String educationLevel,
        @Size(max = 2000) String description,
        @Size(max = 1000) String note,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Long version) {

    @JsonIgnore
    @AssertTrue(message = "Dữ liệu resolution không hợp lệ")
    public boolean isResolutionValid() {
        if (resolution == null) return true;
        if (resolution == SubjectResolutionCommand.Resolution.LINK_EXISTING) {
            return existingSubjectId != null && (code == null || code.isBlank()) && (name == null || name.isBlank());
        }
        return existingSubjectId == null && code != null && !code.isBlank();
    }

    public SubjectResolutionCommand toCommand() {
        return new SubjectResolutionCommand(resolution, existingSubjectId, code, name,
                educationLevel, description, note, version);
    }
}
