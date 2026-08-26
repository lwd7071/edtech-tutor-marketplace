package com.edtech.platform.catalog.controller;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicTeacherControllerTest {

    @Mock
    private TeacherMarketplaceService teacherMarketplaceService;

    @InjectMocks
    private PublicTeacherController publicTeacherController;

    @Test
    void searchTeachers_ShouldReturnListOfTeacherCards() {
        // Arrange
        TeacherSearchParams params = new TeacherSearchParams(
                "keyword", null, null, null, null, null, null, 0, 10
        );
        TeacherCard card = new TeacherCard(
                java.util.UUID.randomUUID(), "FullName", "url", "bio", 5, true, true, false, 
                java.util.List.of(), 500000L, 4.5, 4.5, 10, 1
        );
        when(teacherMarketplaceService.searchTeachers(any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(card)));

        // Act
        com.edtech.platform.common.response.ApiResponse<List<TeacherCard>> response = publicTeacherController.searchTeachers(params);

        // Assert
        org.junit.jupiter.api.Assertions.assertTrue(response.success());
        assertEquals(1, response.data().size());
        assertEquals("FullName", response.data().get(0).fullName());
    }
}
