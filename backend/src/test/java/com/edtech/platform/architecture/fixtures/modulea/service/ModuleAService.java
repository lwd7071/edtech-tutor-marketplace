package com.edtech.platform.architecture.fixtures.modulea.service;

import com.edtech.platform.architecture.fixtures.moduleb.repository.ModuleBRepository;
import com.edtech.platform.architecture.fixtures.moduleb.domain.ModuleBEntity;
import org.springframework.jdbc.core.JdbcTemplate;

public class ModuleAService {
    private ModuleBRepository repository;
    private ModuleBEntity entity;
    private JdbcTemplate jdbcTemplate;
}
