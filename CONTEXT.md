# EdTech Tutor Match — Context

## Mục tiêu

Tutor Match là marketplace kết nối học viên với gia sư cho các gói học 1-1. Học viên tìm gia sư, gửi học thử, mua gói, đặt buổi học, nhận bài tập, chat, đánh giá và yêu cầu hoàn tiền hoặc gia hạn. Gia sư quản lý hồ sơ, lịch rảnh, gói học, buổi học, bài tập và thu nhập. Admin duyệt gia sư, xử lý tài chính và vận hành hệ thống.

## Vai trò và quyền sở hữu

- **Student:** chỉ đọc hoặc thay đổi tài nguyên của chính mình; được mua gói đang bán của gia sư đã được duyệt và hiển thị.
- **Teacher:** chỉ quản lý hồ sơ, gói, lịch, buổi học, bài tập và tài chính của mình; phải được `APPROVED` để bán gói.
- **Admin:** duyệt hồ sơ, xử lý refund/extension/payout, quản lý subject, moderation, settings và audit.
- **Guest:** chỉ dùng marketplace và các endpoint công khai; thao tác cần quan hệ hoặc tài khoản phải chuyển sang đăng nhập.

## Module và seam

Backend là modular monolith Spring Boot. Module sở hữu entity, repository và invariant của mình; module khác gọi qua facade, port, snapshot DTO hoặc event công khai. Controller không truy cập repository của module khác.

`auth/user` sở hữu identity và session; `teacher/subject/catalog` sở hữu marketplace; `enrollment/payment` sở hữu gói đã mua và invoice; `booking` sở hữu lịch học/trial/report; `learning` sở hữu assignment/submission/attachment; `communication` sở hữu chat/notification; `finance` sở hữu wallet/ledger/refund/payout; `ranking` sở hữu review và teacher stats.

Frontend tổ chức theo route group và feature. Feature gọi API qua shared transport; không gọi Axios trực tiếp trong UI. Backend vẫn là nơi quyết định authorization và ownership.

## Invariant cần bảo vệ

- Tổng `Wallet` phải khớp `Ledger`; refund không được trừ một phần rồi ghi ledger toàn phần.
- `remaining + reserved + completed + refunded = totalSessions` của gói học.
- Một gia sư hoặc học viên không có hai booking chồng lịch.
- Mọi read/write theo student, teacher, conversation, assignment và file phải kiểm tra ownership.
- Tiền là VND số nguyên; phân bổ dùng phép tính lũy kế và không tạo entry bằng 0.
- Invoice giữ snapshot điều khoản mua; webhook không đọc lại giá hoặc commission hiện tại của package.
- Migration đã áp dụng là bất biến; không tự `repair()` hoặc sửa checksum lịch sử.
- Side effect SMTP, realtime và notification chạy sau khi transaction nghiệp vụ commit.

## Quy ước ổn định

Database lưu UTC, client hiển thị `Asia/Ho_Chi_Minh`; public ID là UUID; REST dùng response envelope năm field; secret chỉ nằm trong biến môi trường hoặc secret store.
