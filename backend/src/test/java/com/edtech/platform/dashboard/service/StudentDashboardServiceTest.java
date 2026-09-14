package com.edtech.platform.dashboard.service;

import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import com.edtech.platform.dashboard.repository.StudentDashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

    @Mock
    private StudentDashboardRepository repository;

    @InjectMocks
    private StudentDashboardService service;

    @Test
    void delegatesUsingAuthenticatedStudentId() {
        UUID studentId = UUID.randomUUID();
        StudentDashboardView expected = new StudentDashboardView(null, 0, 0, 0, 0);
        when(repository.getDashboard(studentId)).thenReturn(expected);

        assertThat(service.getDashboard(studentId)).isSameAs(expected);
        verify(repository).getDashboard(studentId);
    }
}
