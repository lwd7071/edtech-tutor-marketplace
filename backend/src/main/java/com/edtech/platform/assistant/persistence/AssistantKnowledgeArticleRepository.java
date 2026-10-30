package com.edtech.platform.assistant.persistence;

import com.edtech.platform.assistant.service.AssistantKnowledgeArticle;
import com.edtech.platform.assistant.service.AssistantKnowledgeSearch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Repository
public class AssistantKnowledgeArticleRepository implements AssistantKnowledgeSearch {
    private static final String FIND_PUBLISHED = """
            SELECT title, content, aliases, source_path, sort_order
            FROM assistant_knowledge_articles
            WHERE is_published = true
            ORDER BY sort_order, slug
            LIMIT 100
            """;

    private final JdbcTemplate jdbcTemplate;

    public AssistantKnowledgeArticleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AssistantKnowledgeArticle> findPublished() {
        return jdbcTemplate.query(FIND_PUBLISHED, AssistantKnowledgeArticleRepository::mapArticle);
    }

    private static AssistantKnowledgeArticle mapArticle(ResultSet resultSet, int rowNumber) throws SQLException {
        Array sqlAliases = resultSet.getArray("aliases");
        List<String> aliases = sqlAliases == null ? List.of() : Arrays.stream((Object[]) sqlAliases.getArray())
                .map(String::valueOf)
                .toList();
        return new AssistantKnowledgeArticle(
                resultSet.getString("title"),
                resultSet.getString("content"),
                aliases,
                resultSet.getString("source_path"),
                resultSet.getInt("sort_order"));
    }
}
