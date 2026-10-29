# ADR 0016: Xác thực cho giao diện render phía server

**Status: Accepted — 2026-10-28.** Quyết định cho UI Thymeleaf và các lần triển khai local/staging; production cutover vẫn có gate topology/session-store riêng.

## Bối cảnh

REST API hiện dùng JWT Bearer; browser Next giữ access/refresh token và đồng bộ danh tính theo ADR 0014. Spring Security hiện có session `IF_REQUIRED` cho OAuth2 redirect nhưng CSRF đang tắt; OAuth success handler đưa one-time exchange code hoặc registration token tới frontend. Thymeleaf chạy cùng origin với Spring nên cần ranh giới xác thực riêng, giữ nguyên contract cho các REST clients đang hoạt động.

## Quyết định

- Các trang HTML dùng Spring Security authentication trong server-side HTTP session. Browser chỉ giữ session cookie opaque với `HttpOnly`, `SameSite=Lax`, `Path=/` và `Secure` trên HTTPS. Session hết hạn sau 30 phút không hoạt động và có absolute lifetime tối đa 7 ngày. Access/refresh JWT không được render vào HTML, cookie JavaScript-readable, URL hay browser storage; nếu cần cho vòng đời phiên thì chỉ giữ phía server.
- `/api/**` tiếp tục nhận Bearer JWT theo contract hiện hành; session cookie không xác thực REST API. Tách security rules để CSRF bật cho mọi mutation của HTML, gồm logout POST, trong khi API Bearer giữ semantics hiện tại.
- Đăng nhập HTML gọi cùng domain service `SessionService`; logout là POST có CSRF, hủy HTTP session và revoke refresh token đã cấp. Regenerate session ID sau đăng nhập, áp dụng role/ownership checks phía server và validate redirect theo allowlist role của ADR 0014.
- OAuth cho Thymeleaf hoàn tất exchange trong server, sau đó tạo session. Không chuyển exchange code hoặc registration token qua query string; token chờ chọn role được giữ tạm trong server session. OAuth flow hiện dành cho Next tiếp tục hoạt động cho tới cutover.
- Session hết hạn hoặc refresh phải dùng cơ chế rotation/revocation sẵn có trong `SessionService`; các request đồng thời trong cùng session không được dùng lại refresh token đã rotate. Multi-tab dùng chung session cookie; logout làm session đó mất hiệu lực ở request kế tiếp. Không thêm multi-account switch.
- Local/staging có thể bắt đầu với một backend instance và session store tương ứng môi trường riêng. Trước production/multi-instance phải chọn shared session store hoặc chứng minh topology giữ session ổn định; không dựa vào in-memory session khi scale ngang.

## Lý do và phương án khác

Session same-origin tránh đưa JWT vào giao diện SSR, giữ CSRF protection tại lớp form và không đổi API clients hiện tại. Dùng JWT trực tiếp trong browser sẽ kéo theo client-side token storage và tái tạo các vấn đề ADR 0014; dùng session cookie để xác thực mọi API request sẽ thay đổi security contract cho các REST clients. Cả hai phương án không được đề xuất.

## Gate trước production cutover

Trước production cutover cần test cấu hình thời hạn idle/absolute của HTTP session, session fixation protection, refresh synchronization và OAuth callback/role-selection; phải chọn shared session store hoặc topology tương đương khi chạy nhiều instance. Các chi tiết đó chưa được triển khai/xác minh trong code hiện tại. Điều này không chặn bắt đầu local/staging protected routes theo quyết định đã chốt.
