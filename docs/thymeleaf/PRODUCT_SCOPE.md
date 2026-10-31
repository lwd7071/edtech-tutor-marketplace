# Phạm vi sản phẩm

## Mục tiêu

Chuyển toàn bộ giao diện sản phẩm hiện có sang trang HTML render phía server bằng Thymeleaf trong ứng dụng Spring Boot. Không bỏ chức năng theo role. Hành vi nghiệp vụ tiếp tục do backend sở hữu.

## Vai trò và phạm vi

- Guest: marketplace, nội dung công khai, đăng ký/đăng nhập và khôi phục tài khoản.
- Student: mua gói, thanh toán, đặt buổi từ gói đã mua, học thử, học tập, chat và yêu cầu hỗ trợ tài chính.
- Teacher: hồ sơ, giấy tờ, credentials, lịch rảnh, gói, booking, học viên, bài tập và tài chính.
- Admin: duyệt gia sư/credentials, quản lý subject, người dùng, finance, settlement, cấu hình và audit.

Chi tiết quyền và invariant: [CONTEXT.md](../../CONTEXT.md), [API_CONTRACT.md](../architecture/API_CONTRACT.md). Trang và ma trận quyền legacy: [FRONTEND_SPEC.md](../architecture/FRONTEND_SPEC.md).

## Luồng sản phẩm đã xác định

Mua và học: **tìm gia sư → xem hồ sơ/gói → Student tạo invoice → thanh toán payOS → xác minh trạng thái invoice/package → Student gửi yêu cầu giờ mong muốn → Teacher xác nhận giờ cuối cùng/hình thức học hoặc từ chối → booking được tạo khi Teacher xác nhận**. Yêu cầu không reserve lượt; accept mới tạo booking và giữ lượt. REST API vẫn không có Student create-booking endpoint; quy trình HTML tham chiếu [ADR 0017](../adr/0017-paid-booking-request-workflow.md).

Học thử: có request từ Student và accept/reject từ Teacher trong controller/service, nhưng chính sách slot, lịch đầy đủ và trải nghiệm xuyên suốt chưa được xác minh trong tài liệu này. Không suy diễn thêm.

## Nguyên tắc giao diện

- HTML ban đầu chứa nội dung có thể đọc và thao tác; JavaScript chỉ là tăng cường.
- Mọi chức năng có đường dùng không JavaScript bằng link, query GET, form submit, POST/redirect/GET hoặc tải lại trang.
- Thông tin, trạng thái, quyền và lỗi lấy từ backend; không đặt dữ liệu mẫu giả vào UI.
- Hiển thị thời gian nghiệp vụ theo `Asia/Ho_Chi_Minh`; tiền VND số nguyên.
- Không làm lộ tài liệu riêng tư, token, secret, UUID thô trong giao diện người dùng.

Nguồn thuật ngữ và bất biến: [CONTEXT.md](../../CONTEXT.md); không lặp lại nội dung đó tại đây.

## Phạm vi chuyển đổi

- Bao phủ 70 route sản phẩm hiện có; `/sandbox/components` là route nội bộ, giữ hay bỏ cần quyết định riêng. Route inventory chính là [ROUTES_AND_PAGES.md](ROUTES_AND_PAGES.md).
- Phạm vi gồm public/auth, Student, Teacher, Admin, upload/download riêng tư, thanh toán, chat và WebSocket enhancement nếu giữ feature parity.
- Không suy diễn API từ route UI. Capability nghiệp vụ mới phải được ghi nhận trong ADR/API contract thích hợp và có test trước khi UI gọi; server form không gọi REST loopback.
- ADR 0016 đã chấp nhận server session cho HTML; auth foundation đang triển khai để mở protected routes. Production vẫn cần xác minh session store/topology, OAuth callback và smoke theo [AUTH_AND_SECURITY.md](AUTH_AND_SECURITY.md).
