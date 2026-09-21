# ADR 0014: Browser session identity và chống refresh race

## Status

Accepted — 2026-09-21

## Context

Frontend lưu session trong Zustand, cookie và `localStorage`. Zustand là state trong từng tab, còn cookie/localStorage dùng chung giữa các tab. Vì vậy logout hoặc login role khác ở một tab có thể để tab khác giữ role và React Query cache cũ. Ngoài ra, refresh request có thể hoàn thành sau khi người dùng đã logout hoặc đăng nhập tài khoản khác.

## Decision

- Mỗi lần establish tài khoản tạo `sessionId` bằng `crypto.randomUUID()`; môi trường HTTP local/test dùng UUID-compatible `crypto.getRandomValues()` fallback nếu `randomUUID()` không được browser expose ngoài secure context. Refresh token giữ nguyên `sessionId`; login mới và logout tạo trạng thái phiên mới.
- Sau khi ghi session/token, client ghi revision metadata không chứa token vào `localStorage`. Tab khác nghe `storage` và đồng bộ; tab ghi dữ liệu tự cập nhật store, cache và redirect ngay tại call site. `visibilitychange` là fallback khi tab bị nền trong lúc event xảy ra.
- Return URL được validate cùng lúc với role từ `AuthResult`: workspace chỉ được nhận nếu đúng role (`/student`, `/teacher`, `/admin`), public route và query/hash chỉ qua allowlist. Admin không có impersonation trong contract hiện tại.
- Mỗi request/refresh giữ snapshot `sessionId`; response refresh chỉ được rotate khi `sessionId` và refresh token hiện tại vẫn trùng snapshot. Nếu không trùng, bỏ response và không clear phiên mới. Hàng đợi 401 chỉ phục vụ cùng `sessionId`.
- Khi danh tính đổi hoặc logout, dùng `queryClient.clear()` toàn bộ để không sót cache cá nhân theo query key.

## Consequences

- Đổi tài khoản cùng tab và nhiều tab không còn quay vào workspace role cũ hoặc hiển thị cache người dùng trước.
- Refresh response cũ không thể ghi đè access/refresh token của phiên mới.
- Multi-account switch không qua logout chưa được hỗ trợ; nếu cần, phải thiết kế một capability riêng thay vì nới guard hiện tại.
