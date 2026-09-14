# ADR 0011: RLS default-deny cho bảng Backend-owned

## Status

Accepted and Applied (V32 deployed to Supabase production 2026-09-14).

## Decision

Bật PostgreSQL Row Level Security cho 8 bảng Backend-owned bằng migration append-only V32 (`refresh_tokens`, `teacher_documents`, `subject_proposals`, `subjects`, `pricing_packages`, `teacher_profiles`, `teacher_subjects`, `teacher_availabilities`). Không tạo policy cho `anon` hoặc `authenticated`, không dùng `FORCE ROW LEVEL SECURITY`, và giữ Spring Boot là đường truy cập duy nhất.

`flyway_schema_history` là bảng metadata nội bộ của Flyway và được quản lý riêng ngoài transaction của migration để tránh lock contention. Catalog public chỉ được mở bằng migration/ADR riêng sau này, với predicate loại bỏ dữ liệu inactive, chưa approved và soft-deleted.

## Rollout constraints

- Phải kiểm kê grants và role attributes trước khi apply.
- Phải có backup/snapshot Supabase được xác nhận trước production apply.
- Chỉ Đông được chạy và xác nhận `flyway migrate` trên Supabase production; không chạy song song.
- Flyway rollback không được dùng để đảo migration. Incident chỉ dùng runbook tạm thời và sau đó khôi phục bằng migration forward.

## Consequences

Backend roles có quyền phù hợp tiếp tục hoạt động; PostgREST `anon/authenticated` bị deny mặc định. Realtime/public direct reads chưa được bật. Security Advisor phải được kiểm tra trực tiếp sau apply để xác nhận chín cảnh báo RLS đã biến mất.
