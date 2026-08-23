package com.edtech.platform.subject.facade.dto;


import java.util.UUID;

public record SubjectSnapshot(
        UUID id,
        String code,
        String name,
        String educationLevel,
        boolean isActive
) {
}
