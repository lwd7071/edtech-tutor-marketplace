package com.edtech.platform.assistant.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantKnowledgeArticleRepositoryTest {
    @Test
    void databaseQueryReadsPublishedArticlesOnly() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();

        new AssistantKnowledgeArticleRepository(jdbcTemplate).findPublished();

        assertTrue(jdbcTemplate.sql.contains("WHERE is_published = true"));
        assertTrue(jdbcTemplate.sql.contains("LIMIT 100"));
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private String sql;

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
            this.sql = sql;
            return List.of();
        }
    }
}
