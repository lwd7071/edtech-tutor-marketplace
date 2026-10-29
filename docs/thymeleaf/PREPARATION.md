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

- Đã có seam local cụ thể: `docker-compose.yml` chạy Nginx; [`nginx/default.conf`](../../nginx/default.conf) chuyển `/api/`, `/ws` và `/health` tới backend, còn `/` tới Next. Khi cutover local, đổi upstream `/` sang Spring và giữ các location API/WebSocket/health; route UI chưa sẵn sàng phải được Spring bắt bằng trang tạm an toàn. Không đổi routing trước khi có shell/route handler để tránh UI trả 404.
- `docs/thymeleaf/ROUTES_AND_PAGES.md` là inventory tài liệu, chưa phải runtime route registry hoặc cơ chế kiểm tra parity. Cần thêm registry/test trong Spring để xác nhận route nào render thật, route tạm và role guard trước cutover.
- Repo không có staging Nginx/Compose override. `render.yaml` chỉ định nghĩa backend web service; `docs/DEPLOYMENT_LOCAL.md` ghi tunnel chưa cấu hình. Do đó local cutover có đường triển khai, còn staging cần xác định host/topology và nơi đặt Nginx override trước khi thực hiện phase 4.

## Trạng thái

- Impeccable engine/launcher đã được xác minh khả dụng trong phiên chuẩn bị; `impeccable context` chạy thành công. Chạy `shape` khi bắt đầu thiết kế bề mặt theo plan, không phải trong bước audit này.
- V42 đã được thêm và kiểm tra cục bộ; không kết nối hoặc mutate Supabase trong QA.
- ADR 0016 và 0017 đã Accepted. Còn gate production session topology/OAuth, read-only migration preflight theo AGENTS.md và xác minh PayOS/staging; các gate đó không làm mất kết quả nghiệm thu riêng của route.
