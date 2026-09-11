# Changelog theo đợt hoàn thành

Các entry dưới đây ghi hành vi và bằng chứng quan trọng. Danh sách file đầy đủ nằm trong Git history.

## 2026-09-11 — Hardening cloud smoke và CI runtime

- Thêm `APP_SCHEDULING_ENABLED`, mặc định `true`, để chạy cloud servlet smoke mà không kích hoạt scheduled jobs; focused config test `3/3` pass.
- Thêm `scripts/smoke-teacher-search.ps1` kiểm tra health, accent/case/whitespace normalization và unrated-last ordering.
- Nâng GitHub Actions lên các major dùng Node.js 24 và thêm Dependabot cho GitHub Actions.
- Cloud smoke thật và cold/warm load test chưa xác minh; một lần full verify sau thay đổi kết thúc exit code `1`, lần test Testcontainers tiếp theo bị chặn do Docker Desktop mất socket.

## 2026-09-11 — Tối ưu teacher search/catalog

- Thêm V28 với immutable `unaccent` wrapper, partial GIN trigram indexes và package price index; ghi ADR về dictionary/reindex invariant.
- Refactor teacher search để tính min price một lần, dùng count query tối giản, giữ batch subject query và unrated-last semantics.
- Thêm cache search TTL 5 phút có normalization/page cap, giảm profile TTL còn 30 phút, Redis lỗi fallback PostgreSQL và cấu hình Hikari qua environment.
- Focused validation/cache/serialization và architecture guardrails `15/15` pass local. PostgreSQL 16 Testcontainers repository/EXPLAIN/index assertions `4/4`; Flyway clean schema và V27 → V28 upgrade `9/9` pass. Full backend suite `338/338` pass. Supabase Flyway connection validated 28 migrations and applied V28 successfully; cloud HTTP smoke stopped on the existing missing `ClientRegistrationRepository` OAuth boot configuration. Load test 100 concurrent chưa chạy.

## 2026-09-11 — Sửa cấu hình WebSocket local cho chat

- Thêm `NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws` vào overlay local frontend để STOMP kết nối trực tiếp backend thay vì fallback sang Next.js tại cổng `3000`.
- Bổ sung `frontend/.env.example` và cập nhật hướng dẫn frontend cho trường hợp Next.js và backend chạy khác origin/port.
- Smoke test thủ công: gửi/nhận tin nhắn thành công giữa Student và Teacher sau khi restart Next.js. Cảnh báo do browser extension không thuộc ứng dụng.

## 2026-09-11 — Sửa regression CI Student Journey

- Loại mapping trùng `GET /api/student/assignments/{id}` để Spring context khởi động được.
- Chuyển endpoint đọc StudentPackage về controller của module enrollment, loại dependency payment → enrollment domain.
- Sửa controller test dùng Spring Security context và assertion đúng response envelope.
- Focused suite: `9/9` pass. GitHub Actions full Maven/Testcontainers: `325/325` pass; Docker image build validation pass.

## 2026-09-11 — Đồng bộ context với working tree

- Cập nhật `STATUS.md` để liệt kê các endpoint, route và invariant đã có trong Student Journey.
- Xác nhận rõ phần đã triển khai với phần chưa nghiệm thu: full Testcontainers, Jest/Playwright, DB sạch/nâng cấp và provider thật.
- API contract tiếp tục là nguồn wire format; architecture docs là nguồn module/seam; archive không dùng để suy ra trạng thái hiện tại.

## 2026-09-11 — Chuẩn bị Student Journey và context docs

- Bổ sung luồng xác minh email, invoice purchase snapshot, trial requests, assignment detail/attachment, conversation, notification, parent contact, session reports và review state.
- Thêm migration V26/V27 và script kiểm thử Student Journey; các thay đổi code vẫn đang ở working tree.
- Đã xác nhận backend focused tests `36/36`, frontend typecheck/lint/build pass.
- Chưa xác minh full Testcontainers, Jest Student Journey và Playwright vì Docker chưa chạy.

## 2026-09-10 — Bảo vệ tài chính và vòng đời package

- Tập trung phép tính phân bổ tiền theo cumulative allocation.
- Refund giữ invariant Wallet/Ledger khi pending không đủ.
- Chặn bán package của teacher chưa approved hoặc đang hidden.
- Focused financial và package lifecycle tests đã được chạy trong đợt tương ứng.

## 2026-09-09 — Frontend architecture refactor

- Chuẩn hóa feature query boundaries, typed critical boundaries, UI component usage và public finance contracts.
- Cập nhật tài liệu frontend architecture và test lifecycle.

## Quy tắc ghi entry mới

Mỗi đợt thêm một entry gồm ngày, hành vi thay đổi, contract/schema/config liên quan, lệnh kiểm thử và phần chưa xác minh. Không ghi secret hoặc token.
