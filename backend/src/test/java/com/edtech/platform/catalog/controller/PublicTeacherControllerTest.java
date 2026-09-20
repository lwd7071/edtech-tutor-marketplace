package com.edtech.platform.catalog.controller;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.catalog.service.TeacherMarketplaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                "keyword", null, null, null, null, null, null, null,
                null, null, null, null, 0, 10
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

    @Test
    void searchTeachers_ShouldBindQueryParametersThroughSpringMvc() throws Exception {
        when(teacherMarketplaceService.searchTeachers(any()))
                .thenReturn(new PageImpl<>(List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(publicTeacherController).build();

        mockMvc.perform(get("/api/public/teachers")
                        .param("sort", "rating_desc")
                        .param("page", "0")
                        .param("size", "6"))
                .andExpect(status().isOk());
    }
}
