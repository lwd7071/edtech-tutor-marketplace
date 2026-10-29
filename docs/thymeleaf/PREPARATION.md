# Chuẩn bị trước khi triển khai Thymeleaf

Ngày đối chiếu: 2026-10-29. Đây là ghi chú kiến trúc/cấu hình; không thay thế browser acceptance hoặc production smoke.

## Auth và protected routes

Phương án nằm trong [ADR 0016](../adr/0016-server-rendered-authentication.md), đã `Accepted` cho local/staging. Nó giữ REST Bearer JWT hiện tại, dùng session cookie riêng cho HTML, CSRF cho form mutation và server-side OAuth exchange. Protected route đã có nền tảng SSR; production vẫn cần các gate session topology/OAuth ở ADR.

## Booking, học thử và quyền sở hữu

- REST `POST /api/teacher/bookings` vẫn là command hiện có cho Teacher. `TeacherBookingController` yêu cầu role TEACHER; `BookingService.create` xác minh package/subject/overlap và reserve session khi tạo booking.
- Workflow Student request → Teacher accept/reject đã được quyết định tại [ADR 0017](../adr/0017-paid-booking-request-workflow.md). Request không reserve lượt; accept chốt giờ/mode/logistics, tạo booking và reserve đúng một session theo duration snapshot. Thymeleaf route/action được liệt kê tại [API_INTEGRATION.md](API_INTEGRATION.md). REST API không có Student create-booking endpoint.
- Học thử có luồng riêng đã hiện thực: Student tạo `TrialRequest` với gia sư, môn và giờ mong muốn; Teacher accept với thời gian cuối cùng, delivery mode và logistics hoặc reject. Không suy rộng workflow này thành quy tắc booking trả phí.
- Migration V42 là additive và đã pass local clean-schema cùng upgrade V41→V42 tests; chưa apply production. Browser acceptance dùng profile `test`, database tạm và `TestProfilePaymentGateway` trả link ở domain dành riêng `.test`; fake adapter không xác nhận webhook hay giao dịch đã trả. Không dùng `.env.cloud`/Supabase hoặc PayOS thật. Do ràng buộc đó, production preflight chưa chạy; AGENTS.md yêu cầu read-only preflight trước rollout, nên schema cutover vẫn bị chặn riêng.

## Callback thanh toán và cấu hình URL

- Frontend gửi PayOS return/cancel về `/student/payments/callback?invoiceId=...`. Trang callback đọc trạng thái qua API invoice có ownership check; trạng thái hiển thị đến từ backend, không tin query status từ provider. Webhook `/api/webhooks/payos` mới là luồng xử lý/ghi nhận thanh toán.
- Local Compose phục vụ UI ở `http://localhost:8081`; cấu hình mặc định trước đây của backend là `http://localhost:3000/payment/success` và `/payment/cancel`. `InvoiceServiceImpl` chỉ cho phép return/cancel URL cùng origin cấu hình, nên origin Nginx local có thể bị từ chối.
- Đã sửa `application.yml`: mặc định return/cancel dùng `APP_FRONTEND_URL` và callback route thật; có thể override riêng bằng `APP_PAYMENT_DEFAULT_RETURN_URL` và `APP_PAYMENT_DEFAULT_CANCEL_URL`. Compose hiện đặt `APP_FRONTEND_URL` theo origin Nginx local. Chưa chạy test hoặc smoke thanh toán sau đổi cấu hình; PayOS thật vẫn chưa xác minh.

## Cutover local/staging

- Local Compose và tunnel Compose hiện không chạy service Next.js; [`nginx/default.conf`](../../nginx/default.conf) proxy `/` cùng `/api/`, `/ws` và `/health` tới backend. Nginx chuyển `Host`, `X-Forwarded-Host`, `X-Forwarded-For` và `X-Forwarded-Proto`. Rollback cần khôi phục cấu hình Nginx/tunnel trước đó; chưa thực hiện deploy.
- Runtime route registry và parity test đã có; registry là inventory, không thay thế authorization hoặc browser acceptance. Một số route vẫn đang được nghiệm thu từng lát cắt; chưa có kết quả Guest/Student/Teacher/Admin E2E đầy đủ qua Nginx.
- Spring Session Redis được bật với namespace `edtech:session`; `AuthenticatedUser` và HTML session state hỗ trợ Java serialization. Unit serialization test pass, nhưng chưa kiểm tra Redis persistence qua restart hoặc nhiều backend instance. Compose/Docker E2E chưa chạy do Docker Engine không khả dụng.
- Callback OAuth cloud mặc định dùng website canonical `https://tutormatch.id.vn/login/oauth2/code/google`; staging phải đặt `GOOGLE_OAUTH_REDIRECT_URI` theo canonical staging host và cấu hình Google test client tương ứng. Google Console, PayOS sandbox và Cloudflare tunnel/dashboard không được cấu hình từ repository này và chưa được xác minh.
- Compose local đặt `APP_FRONTEND_URL=http://localhost:8081`; tunnel Compose đặt URL website canonical. CORS local chỉ giữ origin Nginx 8081; API host/origin staging-production cần giữ allowlist riêng phù hợp môi trường.
- `frontend/` và workflow CI Next.js được giữ để rollback; bỏ chúng khỏi CI bắt buộc/deploy chỉ sau khi staging acceptance pass. `APP_CACHE_REVALIDATION_ENABLED` mặc định tắt vì revalidation phục vụ Next Data Cache.

## Trạng thái

- Impeccable engine/launcher đã được xác minh khả dụng trong phiên chuẩn bị; `impeccable context` chạy thành công. Chạy `shape` khi bắt đầu thiết kế bề mặt theo plan, không phải trong bước audit này.
- V42 đã được thêm và kiểm tra cục bộ; không kết nối hoặc mutate Supabase trong QA.
- ADR 0016 và 0017 đã Accepted. Còn gate production session topology/OAuth, read-only migration preflight theo AGENTS.md và xác minh PayOS/staging; các gate đó không làm mất kết quả nghiệm thu riêng của route.
