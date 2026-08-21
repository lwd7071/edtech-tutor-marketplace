package com.edtech.platform.catalog.service;

import com.edtech.platform.catalog.dto.TeacherCard;
import com.edtech.platform.catalog.dto.TeacherPublicDetail;
import com.edtech.platform.catalog.dto.TeacherSearchParams;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherMarketplaceService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<TeacherCard> searchTeachers(TeacherSearchParams params) {
        StringBuilder sql = new StringBuilder("""
            SELECT tp.id, u.full_name, u.avatar_url, tp.bio, tp.years_of_experience,
                   (
                       SELECT string_agg(s.name, ',')
                       FROM teacher_subjects ts
                       JOIN subjects s ON ts.subject_id = s.id
                       WHERE ts.teacher_id = tp.id AND ts.is_active = true AND ts.is_deleted = false
                   ) as subjects_csv,
                   (
                       SELECT COALESCE(MIN(pp.price_vnd), 0)
                       FROM pricing_packages pp
                       WHERE pp.teacher_id = tp.id AND pp.status = 'ACTIVE' AND pp.is_deleted = false
                   ) as min_price,
                   COALESCE(ts.average_rating, 0.0) as avg_rating, 
                   COALESCE(ts.bayesian_rating, 0.0) as bayesian_rating, 
                   COALESCE(ts.review_count, 0) as review_count, 
                   ts.global_rank as global_rank
            FROM teacher_profiles tp
            JOIN users u ON tp.user_id = u.id
            LEFT JOIN teacher_stats ts ON ts.teacher_id = tp.id
            WHERE u.status = 'ACTIVE' AND u.is_deleted = false
              AND tp.profile_status = 'APPROVED' AND tp.is_visible = true AND tp.is_deleted = false
        """);

        List<Object> args = new ArrayList<>();

        if (params.keyword() != null && !params.keyword().isBlank()) {
            sql.append(" AND (u.full_name ILIKE ? OR tp.bio ILIKE ?) ");
            args.add("%" + params.keyword() + "%");
            args.add("%" + params.keyword() + "%");
        }

        if (params.subjectId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM teacher_subjects ts WHERE ts.teacher_id = tp.id AND ts.subject_id = ? AND ts.is_active = true AND ts.is_deleted = false) ");
            args.add(params.subjectId());
        }

        if (params.dayOfWeek() != null && params.startTime() != null && params.endTime() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM teacher_availabilities ta WHERE ta.teacher_id = tp.id AND ta.day_of_week = ? AND ta.start_time <= ? AND ta.end_time >= ? AND ta.is_active = true AND ta.is_deleted = false) ");
            args.add(params.dayOfWeek().name());
            args.add(params.startTime());
            args.add(params.endTime());
        }

        if (params.minPrice() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM pricing_packages pp WHERE pp.teacher_id = tp.id AND pp.price_vnd >= ? AND pp.status = 'ACTIVE' AND pp.is_deleted = false) ");
            args.add(params.minPrice());
        }

        if (params.maxPrice() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM pricing_packages pp WHERE pp.teacher_id = tp.id AND pp.price_vnd <= ? AND pp.status = 'ACTIVE' AND pp.is_deleted = false) ");
            args.add(params.maxPrice());
        }

        int size = params.size() != null ? params.size() : 20;
        int page = params.page() != null ? params.page() : 0;
        
        sql.append(" ORDER BY COALESCE(ts.global_rank, 999999) ASC ");
        sql.append(" LIMIT ? OFFSET ? ");
        args.add(size);
        args.add(page * size);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new TeacherCard(
                UUID.fromString(rs.getString("id")),
                rs.getString("full_name"),
                rs.getString("avatar_url"),
                rs.getString("bio"),
                rs.getInt("years_of_experience"),
                rs.getString("subjects_csv") != null ? Arrays.asList(rs.getString("subjects_csv").split(",")) : List.of(),
                rs.getLong("min_price"),
                rs.getDouble("avg_rating"),
                rs.getDouble("bayesian_rating"),
                rs.getInt("review_count"),
                (Integer) rs.getObject("global_rank")
        ), args.toArray());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "TEACHER_PUBLIC_PROFILE", key = "#teacherId")
    public TeacherPublicDetail getTeacherDetail(UUID teacherId) {
        String sql = """
            SELECT tp.id, u.full_name, u.avatar_url, tp.bio, tp.years_of_experience,
                   tp.languages, tp.supports_online, tp.supports_offline, tp.location_address, tp.introduction_video_url,
                   (
                       SELECT string_agg(s.name, ',')
                       FROM teacher_subjects ts
                       JOIN subjects s ON ts.subject_id = s.id
                       WHERE ts.teacher_id = tp.id AND ts.is_active = true AND ts.is_deleted = false
                   ) as subjects_csv,
                   COALESCE(ts.average_rating, 0.0) as avg_rating, 
                   COALESCE(ts.bayesian_rating, 0.0) as bayesian_rating, 
                   COALESCE(ts.review_count, 0) as review_count, 
                   ts.global_rank as global_rank
            FROM teacher_profiles tp
            JOIN users u ON tp.user_id = u.id
            LEFT JOIN teacher_stats ts ON ts.teacher_id = tp.id
            WHERE tp.id = ? AND u.status = 'ACTIVE' AND u.is_deleted = false
              AND tp.profile_status = 'APPROVED' AND tp.is_visible = true AND tp.is_deleted = false
        """;

        List<TeacherPublicDetail> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
            java.sql.Array arr = rs.getArray("languages");
            List<String> langs = arr != null ? Arrays.asList((String[]) arr.getArray()) : List.of();
            
            return new TeacherPublicDetail(
                UUID.fromString(rs.getString("id")),
                rs.getString("full_name"),
                rs.getString("avatar_url"),
                rs.getString("bio"),
                rs.getInt("years_of_experience"),
                langs,
                rs.getBoolean("supports_online"),
                rs.getBoolean("supports_offline"),
                rs.getString("location_address"),
                rs.getString("introduction_video_url"),
                rs.getString("subjects_csv") != null ? Arrays.asList(rs.getString("subjects_csv").split(",")) : List.of(),
                rs.getDouble("avg_rating"),
                rs.getDouble("bayesian_rating"),
                rs.getInt("review_count"),
                (Integer) rs.getObject("global_rank")
            );
        }, teacherId);

        if (result.isEmpty()) {
            throw new com.edtech.platform.common.exception.BusinessException(com.edtech.platform.common.exception.ErrorCode.RESOURCE_NOT_FOUND);
        }
        return result.get(0);
    }
}
