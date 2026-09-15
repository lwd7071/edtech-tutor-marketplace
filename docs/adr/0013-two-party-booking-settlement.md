# ADR 0013: Xác nhận hai bên và escrow cho buổi học

## Trạng thái

Accepted

## Quyết định

Booking trả phí giữ trạng thái lịch học độc lập với `booking_settlements`. Gia sư xác nhận cùng `SessionReport`; học viên xác nhận riêng sau khi buổi kết thúc. Đủ hai xác nhận trước `endTime + 24h` thì tiêu thụ lượt và chuyển tiền ròng sang số dư khả dụng của gia sư. Thiếu xác nhận khi hết hạn vẫn tiêu thụ lượt nhưng giữ tiền ở ESCROW của nền tảng. Tiền ròng được phân bổ từ snapshot gói ngay trước khi tiêu thụ lượt; settlement mới chưa có số tiền cho tới thời điểm đó.

Gia sư có thể khiếu nại khi tiền đang `HELD`. Admin được mở lại đúng một lần trong 24 giờ kể từ lúc mở; bên còn thiếu xác nhận trong cửa sổ này sẽ tự release tiền. Admin cũng có thể từ chối mở lại bằng quyết định retain có lý do. Nếu lần mở lại hết hạn mà vẫn thiếu xác nhận, ESCROW tiếp tục là nghĩa vụ giữ hộ cho đến khi Admin quyết định release hoặc retain. Chỉ retain mới chuyển ESCROW sang REVENUE. Không hoàn lại lượt đã tiêu thụ trong workflow này.

## Hệ quả

`BookingStatus.COMPLETED` không chứng minh đủ hai bên xác nhận hoặc tiền đã giải ngân. `completed_sessions` tiếp tục là số lượt đã tiêu thụ để giữ invariant package. Buổi timeout có thể chưa có report; report chỉ bắt buộc khi gia sư xác nhận. `settlement_processed` chỉ bảo vệ lần tiêu thụ đầu tiên, còn quyết định tiền sau khi giữ dựa vào settlement và hai ledger. Mọi chuyển tiền phải idempotent, append-only ledger và audit.
