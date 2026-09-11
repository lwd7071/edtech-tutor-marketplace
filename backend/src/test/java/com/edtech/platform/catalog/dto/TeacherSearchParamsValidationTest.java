package com.edtech.platform.catalog.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherSearchParamsValidationTest {

    @Test
    void rejectsKeywordLongerThanOneHundredCharacters() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var params = new TeacherSearchParams(
                    "a".repeat(101), null, null, null, null, null, null,
                    null, null, null, 0, 20
            );

            assertThat(validatorFactory.getValidator().validate(params))
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("keyword"));
        }
    }
}
