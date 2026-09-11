# ADR-0004: Accent-insensitive teacher search

- Trạng thái: Accepted
- Ngày: 2026-09-11
- Phạm vi: `catalog` / public teacher marketplace

## Bối cảnh

Người dùng Việt Nam thường tìm cùng một tên hoặc môn học bằng chữ có dấu, không dấu và khác kiểu hoa/thường. Search `%keyword%` hiện tại không đáp ứng semantics đó và không có index phù hợp. PostgreSQL `unaccent()` mặc định không phải hàm `IMMUTABLE`, nên không thể dùng trực tiếp trong functional index.

## Quyết định

Migration V28 tạo `public.f_unaccent_immutable(text)`, cố định dictionary `public.unaccent`, chuyển chữ thường và đánh dấu hàm `IMMUTABLE`. Query và hai GIN trigram indexes trên `users.full_name` và `teacher_profiles.bio` phải tiếp tục resolve về đúng function OID và normalization rule này.

Đây là deliberate planner contract, không phải đặc tính mặc định của `unaccent()`. Việc đánh dấu immutable chỉ hợp lệ khi dictionary `public.unaccent` không thay đổi. Search cache dùng cùng semantics normalize và có version trong key.

## Invariant vận hành

Không sửa dictionary `public.unaccent` tùy ý. Khi dictionary hoặc normalization rule thay đổi:

1. Deploy wrapper mới hoặc version mới của wrapper.
2. `REINDEX` `ix_users_search_full_name_trgm` và `ix_teacher_profiles_search_bio_trgm`.
3. Tăng version cache key và xóa các entry teacher-search cũ.
4. Chạy lại regression tests tìm kiếm tiếng Việt và execution-plan assertions.

V28 chỉ được deploy khi `unaccent` và `pg_trgm` cùng tồn tại trong schema `public`. Preflight phải chạy bằng đúng user/connection mà Flyway sử dụng.

V28 không có down migration. Rollback production dùng bản backup/restore đã được kiểm chứng hoặc forward-fix; không sửa checksum hay dùng `flyway repair`. Test migration phải bao phủ cả database sạch và đường nâng cấp V27 → V28 trên database PostgreSQL tách biệt.

## Hệ quả

- `Toan`, `toán` và ` TOÁN ` có cùng semantics tìm kiếm.
- GIN trigram index hỗ trợ substring search sau normalization.
- Thay đổi dictionary mà không reindex có thể làm index trả kết quả sai mà PostgreSQL không tự cảnh báo.
- Test planner phải chấp nhận `Bitmap Index Scan` hoặc `Index Scan` và kiểm tra tên index, không chỉ kiểm tra kết quả nghiệp vụ.

## Lựa chọn không dùng

- Gọi trực tiếp `unaccent()` trong index: PostgreSQL từ chối vì volatility không phù hợp.
- Chỉ dùng `ILIKE`: không xử lý tìm kiếm không dấu và vẫn dễ seq scan với `%keyword%`.
- Thêm normalized shadow column ngay: tăng write-path và đồng bộ dữ liệu không cần thiết ở quy mô pilot.
