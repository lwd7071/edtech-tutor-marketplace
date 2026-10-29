# ADR 0016: Xác thực cho giao diện render phía server

**Status: Accepted — 2026-10-28; triển khai Redis session store cập nhật 2026-10-29.** Quyết định cho UI Thymeleaf; production cutover còn gate staging và external routing/provider.

## Bối cảnh

REST API hiện dùng JWT Bearer; browser Next giữ access/refresh token và đồng bộ danh tính theo ADR 0014. Spring Security hiện có session `IF_REQUIRED` cho OAuth2 redirect nhưng CSRF đang tắt; OAuth success handler đưa one-time exchange code hoặc registration token tới frontend. Thymeleaf chạy cùng origin với Spring nên cần ranh giới xác thực riêng, giữ nguyên contract cho các REST clients đang hoạt động.

## Quyết định

- Các trang HTML dùng Spring Security authentication trong server-side HTTP session. Browser chỉ giữ session cookie opaque với `HttpOnly`, `SameSite=Lax`, `Path=/` và `Secure` trên HTTPS. Session hết hạn sau 30 phút không hoạt động và có absolute lifetime tối đa 7 ngày. Access/refresh JWT không được render vào HTML, cookie JavaScript-readable, URL hay browser storage; nếu cần cho vòng đời phiên thì chỉ giữ phía server.
- `/api/**` tiếp tục nhận Bearer JWT theo contract hiện hành; session cookie không xác thực REST API. Tách security rules để CSRF bật cho mọi mutation của HTML, gồm logout POST, trong khi API Bearer giữ semantics hiện tại.
- Đăng nhập HTML gọi cùng domain service `SessionService`; logout là POST có CSRF, hủy HTTP session và revoke refresh token đã cấp. Regenerate session ID sau đăng nhập, áp dụng role/ownership checks phía server và validate redirect theo allowlist role của ADR 0014.
- OAuth cho Thymeleaf hoàn tất exchange trong server, sau đó tạo session. Không chuyển exchange code hoặc registration token qua query string; token chờ chọn role được giữ tạm trong server session. OAuth flow hiện dành cho Next tiếp tục hoạt động cho tới cutover.
- Session hết hạn hoặc refresh phải dùng cơ chế rotation/revocation sẵn có trong `SessionService`; các request đồng thời trong cùng session không được dùng lại refresh token đã rotate. Multi-tab dùng chung session cookie; logout làm session đó mất hiệu lực ở request kế tiếp. Không thêm multi-account switch.
- Spring Session Redis là session store dùng cho UI; cấu hình bật `spring.session.store-type=redis` với namespace `edtech:session`. `AuthenticatedUser` và `HtmlSessionState` serializable. Đây mới là cấu hình/code evidence: cần xác minh Redis persistence sau restart, chia sẻ session giữa nhiều backend instance, cookie/domain/TTL thực tế trên staging trước production. Không dựa vào in-memory session khi scale ngang.

## Lý do và phương án khác

Session same-origin tránh đưa JWT vào giao diện SSR, giữ CSRF protection tại lớp form và không đổi API clients hiện tại. Dùng JWT trực tiếp trong browser sẽ kéo theo client-side token storage và tái tạo các vấn đề ADR 0014; dùng session cookie để xác thực mọi API request sẽ thay đổi security contract cho các REST clients. Cả hai phương án không được đề xuất.

## Gate trước production cutover

Trước production cutover cần nghiệm thu Redis session qua restart/multi-instance, idle/absolute expiry, session fixation, refresh synchronization, Google OAuth callback/role-selection và cookie qua Nginx. Code cấu hình Redis có nhưng topology/runtime test và provider staging smoke chưa chạy. Cloudflare website origin/callback phải được cập nhật ngoài repo; production cutover chưa được thực hiện.
