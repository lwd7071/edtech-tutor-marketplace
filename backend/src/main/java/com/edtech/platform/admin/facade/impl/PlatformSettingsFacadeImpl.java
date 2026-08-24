package com.edtech.platform.admin.facade.impl;

import com.edtech.platform.admin.facade.PlatformSettingsFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformSettingsFacadeImpl implements PlatformSettingsFacade {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int getBayesianMinimumReviews() {
        Integer bayesianMinReviews = jdbcTemplate.queryForObject(
                "SELECT bayesian_minimum_reviews FROM platform_settings WHERE is_singleton = true", Integer.class);
        return bayesianMinReviews != null ? bayesianMinReviews : 10;
    }
}
