package com.edtech.platform.teacher.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateTeacherProfileRequestValidationTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory factory;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void acceptsDocumentedBoundaryValues() {
        var request = new UpdateTeacherProfileRequest(
                "a".repeat(5000), 80, List.of("a".repeat(50)), true, true,
                "a".repeat(500), "https://example.test/video");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsOutOfRangeNumbersAndOversizedCollections() {
        var request = new UpdateTeacherProfileRequest(
                "bio", 81, List.of("a".repeat(51)), true, true, "address", "https://example.test/video");

        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("yearsOfExperience", "languages[0].<list element>");
    }

    @Test
    void rejectsHtmlAndNonHttpVideoUrls() {
        var request = new UpdateTeacherProfileRequest(
                "<script>alert(1)</script>", 0, List.of("Vietnamese"), true, true, "address", "javascript:alert(1)");

        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("bio", "introductionVideoUrl");
    }

    @Test
    void rejectsMalformedHttpVideoUrl() {
        var request = new UpdateTeacherProfileRequest(
                "bio", 0, List.of("Vietnamese"), true, true, "address", "https:// ");

        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("introductionVideoUrl");
    }
}
