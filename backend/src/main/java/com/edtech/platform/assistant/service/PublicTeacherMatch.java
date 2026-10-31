package com.edtech.platform.assistant.service;

import java.util.List;

public record PublicTeacherMatch(
        String fullName,
        List<String> subjects,
        List<String> educationLevels,
        int yearsOfExperience,
        long startingPriceVnd,
        double averageRating,
        String profilePath
) {
    public PublicTeacherMatch {
        subjects = List.copyOf(subjects);
        educationLevels = List.copyOf(educationLevels);
    }
}
