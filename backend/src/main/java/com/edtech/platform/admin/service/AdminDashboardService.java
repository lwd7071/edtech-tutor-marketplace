package com.edtech.platform.admin.service;

import com.edtech.platform.admin.dto.response.AdminDashboardView;
import com.edtech.platform.admin.repository.AdminDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminDashboardRepository repository;

    public AdminDashboardView getDashboard() {
        return repository.getDashboardStats();
    }
}