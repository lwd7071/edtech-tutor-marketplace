package com.edtech.platform.teacher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.edtech.platform.teacher.dto.validation.ValidHttpUrl;
import java.util.List;

public record UpdateTeacherProfileRequest(
        @Pattern(regexp = "^[^<]*$", message = "Bio must not contain HTML tags")
        @Size(max = 5000)
        String bio,
        @Min(0) @Max(80)
        Integer yearsOfExperience,
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> languages,
        @NotNull Boolean supportsOnline,
        @NotNull Boolean supportsOffline,
        @Size(max = 500)
        String locationAddress,
        @Size(max = 500)
        @ValidHttpUrl(message = "Introduction video URL must be a valid HTTP or HTTPS URL")
        String introductionVideoUrl
) {
}
