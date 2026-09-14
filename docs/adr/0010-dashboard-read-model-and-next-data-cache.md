# 10. Dashboard Read Model và Public Data Cache

## Status

Accepted

## Decision

- Student dashboard dùng endpoint `GET /api/student/dashboard` với một SQL read projection tổng hợp các counter cần hiển thị. Module dashboard chỉ đọc, không sở hữu mutation và không expose entity.
- Public Server Components gọi backend qua server-only native `fetch`, dùng Next Data Cache với TTL 300 giây cho catalog/profile/package và 60 giây cho availability/review/ranking.
- Browser Axios adapter tiếp tục phục vụ các Client Component và authenticated request. Không dùng chung adapter để tránh kéo session/cookie code vào server.
- Đợt đầu không thêm public revalidation webhook; dữ liệu public chấp nhận bounded staleness theo TTL. Cache tags được gắn để mở rộng invalidation sau này.

## Consequences

- Student dashboard giảm nhiều round-trip và không tải các list lớn chỉ để đếm.
- Public landing/profile/ranking có thể render tại Vercel edge/cache và không gọi backend từ browser lúc initial load.
- Dashboard projection phải được kiểm thử bằng PostgreSQL thật để giữ đúng semantics của từng module.
- Public data có thể trễ tối đa 60–300 giây; các command vẫn được backend kiểm tra trên dữ liệu hiện thời.
