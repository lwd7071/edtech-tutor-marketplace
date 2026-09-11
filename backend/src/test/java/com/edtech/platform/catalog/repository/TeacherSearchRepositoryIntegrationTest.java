package com.edtech.platform.catalog.repository;

import com.edtech.platform.catalog.dto.TeacherSearchParams;
import com.edtech.platform.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TeacherSearchRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TeacherSearchRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchTeachersMatchesVietnameseTextWithoutAccents() {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, full_name, role, status, email_verified)
                VALUES (?, ?, 'Nguyễn Văn Toán', 'TEACHER', 'ACTIVE', true)
                """, userId, "search-" + userId + "@example.test");
        jdbcTemplate.update("""
                INSERT INTO teacher_profiles
                    (id, user_id, bio, profile_status, is_visible, supports_online, supports_offline)
                VALUES (?, ?, 'Dạy đại số', 'APPROVED', true, true, false)
                """, teacherId, userId);

        var result = repository.searchTeachers(params("  TOAN  "),
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(card -> card.id())
                .contains(teacherId);
    }

    @Test
    void vietnameseSearchExpressionsUseTrigramIndexes() {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, full_name, role, status, email_verified)
                SELECT gen_random_uuid(), 'search-plan-' || g || '@example.test',
                       CASE WHEN g = 5000 THEN 'Nguyễn Văn Toán' ELSE 'Gia sư thử nghiệm ' || g END,
                       'TEACHER', 'ACTIVE', true
                FROM generate_series(1, 5000) g
                """);
        jdbcTemplate.update("""
                INSERT INTO teacher_profiles
                    (id, user_id, bio, profile_status, is_visible, supports_online, supports_offline)
                SELECT gen_random_uuid(), id,
                       CASE WHEN full_name = 'Nguyễn Văn Toán' THEN 'Chuyên luyện thi Vật Lý' ELSE 'Kinh nghiệm giảng dạy' END,
                       'APPROVED', true, true, false
                FROM users WHERE email LIKE 'search-plan-%@example.test'
                """);
        jdbcTemplate.execute("ANALYZE users");
        jdbcTemplate.execute("ANALYZE teacher_profiles");

        String namePlan = explain("""
                SELECT id FROM users
                WHERE status = 'ACTIVE' AND is_deleted = false
                  AND public.f_unaccent_immutable(full_name)
                      LIKE '%' || public.f_unaccent_immutable('toan') || '%'
                """);
        String bioPlan = explain("""
                SELECT id FROM teacher_profiles
                WHERE profile_status = 'APPROVED' AND is_visible = true AND is_deleted = false
                  AND public.f_unaccent_immutable(bio)
                      LIKE '%' || public.f_unaccent_immutable('vat ly') || '%'
                """);

        assertThat(namePlan).contains("ix_users_search_full_name_trgm");
        assertThat(bioPlan).contains("ix_teacher_profiles_search_bio_trgm");
    }

    @Test
    void activePackageMinimumPriceUsesCoveringPartialIndex() {
        jdbcTemplate.execute("""
                INSERT INTO pricing_packages
                    (teacher_id, subject_id, name, total_sessions, duration_days,
                     price_vnd, session_duration_minutes, status, is_deleted)
                SELECT tp.id, s.id, 'Gói benchmark ' || g, 10, 30,
                       100000 + g, 60, 'ACTIVE', false
                FROM (SELECT id FROM teacher_profiles LIMIT 1) tp
                CROSS JOIN (SELECT id FROM subjects LIMIT 1) s
                CROSS JOIN generate_series(1, 5000) g
                """);
        jdbcTemplate.execute("ANALYZE pricing_packages");

        String packagePlan = explain("""
                SELECT MIN(pp.price_vnd)
                FROM pricing_packages pp
                WHERE pp.teacher_id = (SELECT id FROM teacher_profiles LIMIT 1)
                  AND pp.status = 'ACTIVE'
                  AND pp.is_deleted = false
                """);

        assertThat(packagePlan).contains("ix_pricing_packages_active_teacher_price");
    }

    @Test
    void ratingSortPlacesUnratedTeachersAfterRatedTeachers() {
        UUID ratedTeacher = insertTeacher("Gia sư Xếp Hạng Có Điểm");
        UUID unratedTeacher = insertTeacher("Gia sư Xếp Hạng Chưa Điểm");
        jdbcTemplate.update("""
                INSERT INTO teacher_stats
                    (teacher_id, average_rating, bayesian_rating, review_count,
                     completed_session_count, completion_rate, trial_session_count,
                     trial_conversion_rate, global_rank)
                VALUES (?, 4.5, 4.4, 10, 20, 1, 2, 0.5, 1)
                """, ratedTeacher);

        var result = repository.searchTeachers(
                new TeacherSearchParams(
                        "xep hang", null, null, null, null, null, null, null,
                        null, "rating_desc", 0, 100),
                org.springframework.data.domain.PageRequest.of(0, 100));

        List<UUID> ids = result.getContent().stream().map(card -> card.id()).toList();
        assertThat(ids).contains(ratedTeacher, unratedTeacher);
        assertThat(ids.indexOf(ratedTeacher)).isLessThan(ids.indexOf(unratedTeacher));
    }

    private UUID insertTeacher(String fullName) {
        UUID userId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, full_name, role, status, email_verified)
                VALUES (?, ?, ?, 'TEACHER', 'ACTIVE', true)
                """, userId, "ranking-" + userId + "@example.test", fullName);
        jdbcTemplate.update("""
                INSERT INTO teacher_profiles
                    (id, user_id, bio, profile_status, is_visible, supports_online, supports_offline)
                VALUES (?, ?, 'Xếp hạng gia sư', 'APPROVED', true, true, false)
                """, teacherId, userId);
        return teacherId;
    }

    private String explain(String query) {
        List<String> plan = jdbcTemplate.query(
                "EXPLAIN (ANALYZE, BUFFERS) " + query,
                (rs, rowNum) -> rs.getString(1));
        return String.join("\n", plan);
    }

    private TeacherSearchParams params(String keyword) {
        return new TeacherSearchParams(
                keyword, null, null, null, null, null, null, null,
                null, null, 0, 20);
    }
}
