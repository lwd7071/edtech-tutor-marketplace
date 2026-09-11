# Changelog theo đợt hoàn thành

Các entry dưới đây ghi hành vi và bằng chứng quan trọng. Danh sách file đầy đủ nằm trong Git history.

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
