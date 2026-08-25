package com.edtech.platform.subject.facade.dto;

import java.util.UUID;

public record SubjectResolutionCommand(
        Resolution resolution, UUID existingSubjectId, String code, String name,
        String educationLevel, String description, String note) {
    public enum Resolution { CREATE_NEW, LINK_EXISTING }
}
