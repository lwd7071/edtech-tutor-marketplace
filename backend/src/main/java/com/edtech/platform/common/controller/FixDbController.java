package com.edtech.platform.common.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FixDbController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/public/fix-db")
    public String fixDb() {
        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN deleted DROP NOT NULL");
        } catch (Exception e) {}
        try {
            jdbcTemplate.execute("ALTER TABLE teacher_stats ALTER COLUMN deleted DROP NOT NULL");
        } catch (Exception e) {}
        try {
            jdbcTemplate.execute("ALTER TABLE reviews ALTER COLUMN deleted DROP NOT NULL");
        } catch (Exception e) {}
        return "DB Fixed!";
    }
}
