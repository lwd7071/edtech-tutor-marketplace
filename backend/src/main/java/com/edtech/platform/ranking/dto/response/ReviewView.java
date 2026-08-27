package com.edtech.platform.ranking.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.UUID;

@Value
@Builder
public class ReviewView {
    
    private UUID id;
    private Short rating;
    private String comment;
    private ZonedDateTime createdAt;
    private StudentDto student;

    @Value
    @Builder
    public static class StudentDto {
        private UUID id;
        private String fullName;
        private String avatarUrl;
    }
}
