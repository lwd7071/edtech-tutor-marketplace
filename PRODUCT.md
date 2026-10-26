# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Học viên:** tìm gia sư theo môn học và mục tiêu, xem hồ sơ/gói học, rồi theo học và quản lý việc học trên nền tảng.
- **Gia sư:** giới thiệu năng lực, kết nối với học viên, tổ chức buổi học và quản lý hoạt động dạy cùng thu nhập.
- Học viên và gia sư là hai nhóm người dùng chính ngang nhau; trải nghiệm marketplace cần phục vụ cả hai phía.
- **Khách chưa đăng nhập:** khám phá môn học, gia sư và thông tin công khai; cần đăng nhập để dùng các thao tác gắn với tài khoản.
- **Quản trị viên:** duyệt và vận hành marketplace, người dùng cùng các quy trình tài chính.

## Product Purpose

Tutor Match kết nối học viên với gia sư cho việc học 1-1 theo mục tiêu. Nền tảng hỗ trợ hai bên đi từ khám phá và kết nối đến tổ chức buổi học, theo dõi việc học và xử lý các tác vụ liên quan. Chưa có chỉ số thành công kinh doanh cụ thể được xác nhận.

## Positioning

Điểm định vị đã được xác nhận: kết nối học viên và gia sư cho việc học 1-1 theo mục tiêu. Không khẳng định đây là cơ chế độc quyền hoặc cam kết kết quả học tập.

## Operating Context

- Học viên có thể duyệt môn, lọc và xem hồ sơ gia sư công khai; sau khi đăng nhập có thể gửi yêu cầu học thử, mua gói và đặt buổi học.
- Gia sư quản lý hồ sơ, năng lực, lịch rảnh và gói học; trạng thái được duyệt là điều kiện để bán gói.
- Học viên và gia sư tiếp tục phối hợp qua lịch học, bài tập, tin nhắn và thông báo.
- Quản trị viên xử lý hàng đợi duyệt, người dùng, thiết lập, nhật ký và yêu cầu tài chính.
- Website có giao diện tiếng Việt; thời gian được lưu UTC và hiển thị theo `Asia/Ho_Chi_Minh`; tiền tệ nghiệp vụ là VND số nguyên.

## Capabilities and Constraints

Các luồng hiện có là phạm vi phải giữ khi nâng cấp giao diện:

- **Marketplace công khai:** duyệt/tìm môn học và gia sư, bộ lọc, hồ sơ gia sư, gói học, lịch rảnh, đánh giá và bảng xếp hạng; trang cách hoạt động, hỗ trợ và đăng ký trở thành gia sư.
- **Tài khoản:** đăng ký, đăng nhập, đăng xuất, xác minh email, khôi phục/đặt lại mật khẩu và luồng OAuth.
- **Học viên:** tổng quan, hồ sơ và thông tin liên hệ phụ huynh, yêu cầu học thử, gói học, hóa đơn/thanh toán, lịch/buổi học/báo cáo, bài tập/bài nộp, hội thoại, thông báo, đánh giá, yêu cầu hoàn tiền và gia hạn.
- **Gia sư:** tổng quan/thống kê, hồ sơ, tài liệu và minh chứng năng lực, đề xuất môn, gói học, lịch rảnh, yêu cầu học thử, học viên, buổi học, bài tập/chấm bài, hội thoại, thông báo, tài khoản ngân hàng, ví và yêu cầu rút tiền.
- **Quản trị:** tổng quan, duyệt gia sư và minh chứng, duyệt đề xuất môn, quản lý trạng thái người dùng, hoàn tiền/gia hạn/rút tiền/quyết toán booking, thiết lập và audit log.
- Quyền sở hữu và phân quyền do backend quyết định; giao diện không được bỏ qua hoặc thay thế các kiểm tra này.
- Giữ các invariant nghiệp vụ hiện hành về số dư/ledger, lượt học của gói, lịch booking không chồng lấn, snapshot hóa đơn, xác nhận booking và quy trình settlement.
- Tương tác giao diện chỉ được dựa trên API/contract hiện có; không hiển thị số liệu hoặc lời chứng thực minh họa như dữ liệu thật.
- Chưa xác nhận KPI, phân khúc tuổi cụ thể, cam kết kết quả, hay chỉ số/thành tích công khai nào; không tự tạo các thông tin này.

## Brand Commitments

Tên sản phẩm trong dự án là Tutor Match. Người dùng xác nhận định vị kết nối học 1-1 theo mục tiêu. Chưa có thêm cam kết giọng thương hiệu hoặc tuyên bố thương mại đã được xác nhận.

## Evidence on Hand

- Mô hình vai trò, mục tiêu và invariant: `CONTEXT.md`.
- Trạng thái triển khai và bằng chứng kiểm thử: `docs/STATUS.md`.
- Route, API và hành vi hiện có: `frontend/src/app/`, `backend/src/main/java/` và `docs/architecture/`.
- Minh họa giao diện có trong `frontend/public/images/`; đây là tài sản hình ảnh, không phải bằng chứng khách hàng, đánh giá hay kết quả học tập.
- Không có bằng chứng được xác nhận cho testimonial, số lượng người dùng/gia sư, thống kê thành tích, giá hoặc cam kết đầu ra; không được tự bịa.

## Product Principles

- Phục vụ cân bằng cả học viên lẫn gia sư trong hành trình kết nối và học/dạy.
- Giúp hai bên hiểu mục tiêu, năng lực, lịch và chi phí trước khi cam kết.
- Giữ nguyên luồng nghiệp vụ và quyền truy cập khi cải thiện giao diện.
- Phân biệt rõ dữ liệu thật, trạng thái xử lý và lỗi; không biến dữ liệu thiếu thành kết quả giả.
