package com.edtech.platform.catalog.repository;

import com.edtech.platform.catalog.dto.SubjectDto;
import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TeacherSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String PRICE_JOIN = """
            LEFT JOIN LATERAL (
                SELECT MIN(pp.price_vnd) AS min_price
                FROM pricing_packages pp
                WHERE pp.teacher_id = tp.id
                  AND pp.status = 'ACTIVE'
                  AND pp.is_deleted = false
            ) price ON true
            """;

    private static final String BASE_FROM = """
            FROM teacher_profiles tp
            JOIN users u ON tp.user_id = u.id
            LEFT JOIN teacher_stats ts ON ts.teacher_id = tp.id
            """;

    private static final String BASE_WHERE = """
            WHERE tp.profile_status = 'APPROVED'
              AND tp.is_visible = true
              AND tp.is_deleted = false
              AND u.status = 'ACTIVE'
              AND u.is_deleted = false
            """;

    public Page<TeacherCard> searchTeachers(TeacherSearchParams params, PageRequest pageRequest) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                tp.id, 
                u.full_name, 
                u.avatar_url, 
                tp.bio, 
                tp.years_of_experience,
                tp.verified_badge, 
                tp.supports_online, 
                tp.supports_offline,
                COALESCE(ts.average_rating, 0.0) AS average_rating,
                COALESCE(ts.bayesian_rating, 0.0) AS bayesian_rating,
                COALESCE(ts.review_count, 0) AS review_count,
                COALESCE(ts.global_rank, 999999) AS global_rank,
                price.min_price
        """);
        sql.append(BASE_FROM).append(PRICE_JOIN).append(BASE_WHERE);

        MapSqlParameterSource sqlParams = new MapSqlParameterSource();

        appendFilters(sql, params, sqlParams);

        boolean filtersByPrice = params.minPrice() != null || params.maxPrice() != null;
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) ").append(BASE_FROM);
        if (filtersByPrice) {
            countSql.append(PRICE_JOIN);
        }
        countSql.append(BASE_WHERE);
        appendFilters(countSql, params, sqlParams);

        Long totalElements = jdbcTemplate.queryForObject(countSql.toString(), sqlParams, Long.class);
        if (totalElements == null) totalElements = 0L;

        appendSorting(sql, params);
        sql.append(" LIMIT :limit OFFSET :offset");
        sqlParams.addValue("limit", pageRequest.getPageSize());
        sqlParams.addValue("offset", pageRequest.getOffset());

        List<TeacherCard> cards = queryCards(sql, sqlParams);
        populateSubjects(cards);

        return new PageImpl<>(cards, pageRequest, totalElements);
    }

    private void appendFilters(StringBuilder sql, TeacherSearchParams params, MapSqlParameterSource sqlParams) {

        if (params.keyword() != null && !params.keyword().isBlank()) {
            sql.append("""
                    AND tp.id IN (
                        SELECT keyword_tp.id
                        FROM teacher_profiles keyword_tp
                        JOIN users keyword_u ON keyword_tp.user_id = keyword_u.id
                        WHERE keyword_tp.profile_status = 'APPROVED'
                          AND keyword_tp.is_visible = true
                          AND keyword_tp.is_deleted = false
                          AND keyword_u.status = 'ACTIVE'
                          AND keyword_u.is_deleted = false
                          AND public.f_unaccent_immutable(keyword_u.full_name)
                              LIKE '%' || public.f_unaccent_immutable(:keyword) || '%'
                        UNION
                        SELECT keyword_tp.id
                        FROM teacher_profiles keyword_tp
                        WHERE keyword_tp.profile_status = 'APPROVED'
                          AND keyword_tp.is_visible = true
                          AND keyword_tp.is_deleted = false
                          AND public.f_unaccent_immutable(keyword_tp.bio)
                              LIKE '%' || public.f_unaccent_immutable(:keyword) || '%'
                    )
                    """);
            sqlParams.addValue("keyword", params.keyword().trim());
        }

        if (params.subjectId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM teacher_subjects tsub JOIN subjects s ON tsub.subject_id = s.id WHERE tsub.teacher_id = tp.id AND tsub.subject_id = :subjectId AND tsub.is_deleted = false AND tsub.is_active = true AND s.is_deleted = false AND s.is_active = true)");
            sqlParams.addValue("subjectId", params.subjectId());
        }

        if (params.minPrice() != null) {
            sql.append(" AND price.min_price >= :minPrice");
            sqlParams.addValue("minPrice", params.minPrice());
        }

        if (params.maxPrice() != null) {
            sql.append(" AND price.min_price <= :maxPrice");
            sqlParams.addValue("maxPrice", params.maxPrice());
        }

        // Time filtering
        if (params.dayOfWeek() != null && params.startTime() != null && params.endTime() != null) {
            sql.append("""
                 AND EXISTS (
                    SELECT 1 FROM teacher_availabilities ta 
                    WHERE ta.teacher_id = tp.id 
                    AND ta.day_of_week = :dayOfWeek
                    AND ta.start_time <= :startTime
                    AND ta.end_time >= :endTime
                    AND ta.is_deleted = false
                    AND ta.is_active = true
                )
            """);
            sqlParams.addValue("dayOfWeek", params.dayOfWeek().name());
            sqlParams.addValue("startTime", params.startTime());
            sqlParams.addValue("endTime", params.endTime());
        }

        if (params.minRating() != null) {
            sql.append(" AND COALESCE(ts.average_rating, 0.0) >= :minRating");
            sqlParams.addValue("minRating", params.minRating());
        }

        if (params.deliveryMode() != null) {
            if ("ONLINE".equalsIgnoreCase(params.deliveryMode())) {
                sql.append(" AND tp.supports_online = true");
            } else if ("OFFLINE".equalsIgnoreCase(params.deliveryMode())) {
                sql.append(" AND tp.supports_offline = true");
            }
        }

    }

    private void appendSorting(StringBuilder sql, TeacherSearchParams params) {
        if (params.sort() != null) {
            switch (params.sort().toLowerCase()) {
                case "price_asc":
                    sql.append(" ORDER BY price.min_price ASC, tp.id ASC ");
                    break;
                case "price_desc":
                    sql.append(" ORDER BY price.min_price DESC, tp.id ASC ");
                    break;
                case "rating_desc":
                    sql.append(" ORDER BY COALESCE(ts.average_rating, 0.0) DESC, tp.id ASC ");
                    break;
                case "experience_desc":
                    sql.append(" ORDER BY tp.years_of_experience DESC NULLS LAST, tp.id ASC ");
                    break;
                default:
                    sql.append(" ORDER BY COALESCE(ts.global_rank, 999999) ASC, tp.id ASC ");
            }
        } else {
            sql.append(" ORDER BY COALESCE(ts.global_rank, 999999) ASC, tp.id ASC ");
        }
    }

    private List<TeacherCard> queryCards(StringBuilder sql, MapSqlParameterSource sqlParams) {
        return jdbcTemplate.query(sql.toString(), sqlParams, (rs, rowNum) -> {
            UUID teacherId = (UUID) rs.getObject("id");
            return new TeacherCard(
                    teacherId,
                    rs.getString("full_name"),
                    rs.getString("avatar_url"),
                    rs.getString("bio"),
                    rs.getInt("years_of_experience"),
                    rs.getBoolean("verified_badge"),
                    rs.getBoolean("supports_online"),
                    rs.getBoolean("supports_offline"),
                    new ArrayList<>(), // Subjects will be populated in batch
                    rs.getLong("min_price"),
                    rs.getDouble("average_rating"),
                    rs.getDouble("bayesian_rating"),
                    rs.getInt("review_count"),
                    rs.getInt("global_rank")
            );
        });
    }

    private void populateSubjects(List<TeacherCard> cards) {
        if (!cards.isEmpty()) {
            List<UUID> teacherIds = cards.stream().map(TeacherCard::id).collect(Collectors.toList());
            String subjectSql = """
                SELECT tsub.teacher_id, s.id as subject_id, s.name as subject_name 
                FROM teacher_subjects tsub
                JOIN subjects s ON tsub.subject_id = s.id
                WHERE tsub.teacher_id IN (:teacherIds)
                AND tsub.is_deleted = false 
                AND tsub.is_active = true
                AND s.is_deleted = false 
                AND s.is_active = true
            """;
            MapSqlParameterSource subjParams = new MapSqlParameterSource("teacherIds", teacherIds);
            
            Map<UUID, List<SubjectDto>> subjectsByTeacher = new HashMap<>();
            jdbcTemplate.query(subjectSql, subjParams, rs -> {
                UUID tid = (UUID) rs.getObject("teacher_id");
                UUID sid = (UUID) rs.getObject("subject_id");
                String sName = rs.getString("subject_name");
                subjectsByTeacher.computeIfAbsent(tid, k -> new ArrayList<>()).add(new SubjectDto(sid, sName));
            });
            
            for (int i = 0; i < cards.size(); i++) {
                TeacherCard original = cards.get(i);
                List<SubjectDto> subjs = subjectsByTeacher.getOrDefault(original.id(), Collections.emptyList());
                cards.set(i, new TeacherCard(
                        original.id(), original.fullName(), original.avatarUrl(), original.bioExcerpt(),
                        original.yearsOfExperience(), original.verifiedBadge(), original.supportsOnline(),
                        original.supportsOffline(), subjs, original.startingPriceVnd(),
                        original.averageRating(), original.bayesianRating(), original.reviewCount(), original.globalRank()
                ));
            }
        }
    }
}
