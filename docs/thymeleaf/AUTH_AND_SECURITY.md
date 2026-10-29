# Đăng nhập và bảo mật

## Hiện trạng và mâu thuẫn kiến trúc

Backend REST xác thực JWT bearer; Spring Security dùng session `IF_REQUIRED` cho OAuth2 redirect flow, CSRF hiện đang disable, CORS cấu hình riêng. Tham chiếu [SecurityConfig.java](../../backend/src/main/java/com/edtech/platform/common/config/SecurityConfig.java), [API_CONTRACT.md](../architecture/API_CONTRACT.md).

[ADR 0014](../adr/0014-browser-session-identity-and-refresh-race.md) chốt JWT/session ở browser, token refresh rotation, localStorage revision và race handling nhiều tab. Nó không quy định server-rendered session cookie làm phương thức đăng nhập UI. [ADR 0016](../adr/0016-server-rendered-authentication.md) đã được chấp nhận: REST giữ JWT Bearer; HTML dùng server-side session cookie opaque, CSRF cho mutation, idle 30 phút và absolute lifetime tối đa 7 ngày. ADR 0016 bổ sung auth transport cho HTML, không thay thế hay sửa ADR 0014 cho Next/API hiện tại.

ADR 0007 yêu cầu zero-trust secrets; [ADR 0012](../adr/0012-residence-and-credential-privacy.md) và [ADR 0009](../adr/0009-server-owned-finance-proof-and-audit.md) quy định quyền riêng tư tài liệu và chứng từ.

## Yêu cầu an toàn không phụ thuộc giải pháp auth

- Backend vẫn quyết định role/ownership; kiểm tra ở server với mọi request và form.
- Return URL phải được kiểm tra theo role/allowlist như nguyên tắc ADR 0014.
- Không render access/refresh token, secret hoặc URL proof riêng tư vào HTML.
- Admin proof tải qua backend proxy no-store; dùng bytes/stream an toàn, không expose storage URL.
- Mọi mutation qua form phải chống CSRF nếu dùng cookie; xử lý replay/version/idempotency theo API contract.
- Escape output Thymeleaf mặc định; không dùng `th:utext` cho dữ liệu người dùng trừ khi có sanitizer được xác định.
- Lỗi 401/403 phải thành redirect/trang phù hợp theo web UX nhưng không làm suy yếu API security.

## Production gate còn lại

Đã triển khai lát cắt local cho `/auth/login`, `/auth/register`, `/auth/verify-email`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/oauth/role`, callback compatibility `/oauth2/callback` và POST `/auth/logout`. Login gọi `SessionService`, đổi session ID, lưu JWT phía server và giới hạn return path theo role; logout revoke refresh token theo khả năng rồi hủy session. Đăng ký gửi yêu cầu xác minh; email/reset one-time token được stage trong session server, sau đó redirect khỏi URL có token. Xác minh và reset là POST có CSRF để GET link preview không gây mutation. Resend/forgot response không tiết lộ email có tài khoản hay không. OAuth HTML flow exchange code phía server và stage registration token trong session; flow cũ cho Next vẫn hoạt động khi không có marker HTML. `HtmlSessionAuthenticationFilter` refresh JWT hết hạn và áp dụng idle/absolute lifetime. MockMvc coverage có; browser/staging và Google/email provider smoke chưa chạy.

ADR 0016 chốt cookie/session lifetime, server-side JWT custody, logout/revocation, OAuth server exchange, CSRF và multi-tab cùng session. Còn phải xác minh refresh synchronization qua nhiều backend instance, OAuth provider callback/cookie flow trên staging, idle/absolute expiry thực tế và shared session store/topology trước production. Không bật `remember-me`.

## Gate triển khai protected route

Protected routes có thể bắt đầu theo ADR 0016. Mỗi lát cắt phải giữ `/api/**` Bearer-only; bridge HTML principal tới service authorization không được tạo HTTP loopback. Trước production vẫn phải có smoke test OAuth/local staging, refresh/revoke, session fixation, CSRF, role/ownership và triển khai topology.
