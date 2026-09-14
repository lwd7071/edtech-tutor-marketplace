package com.edtech.platform.dashboard.service;

import com.edtech.platform.dashboard.dto.response.StudentDashboardView;
import com.edtech.platform.dashboard.repository.StudentDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentDashboardService {

    private final StudentDashboardRepository repository;

    @Transactional(readOnly = true)
    public StudentDashboardView getDashboard(UUID studentId) {
        return repository.getDashboard(studentId);
    }
}
