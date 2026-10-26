# Quy tắc component UI

Đọc `DESIGN_SYSTEM.md` trước khi sửa component dùng chung. Một component chỉ chứa hành vi chung; layout theo route nằm ở page/feature sở hữu nghiệp vụ. Giữ props, callback và data flow đang hoạt động.

| Thành phần | Cấu trúc và sử dụng | Trạng thái và kiểm tra |
|---|---|---|
| Public navbar/footer | Wordmark `tutormatch`; mục chính là nhãn chữ; CTA một cấp rõ; mobile drawer chứa đầy đủ link thật. Footer là điều hướng/hỗ trợ, không phải bản sao hero. | `aria-current`, menu keyboard/touch, focus trả về nút mở, guest/authenticated đúng link. |
| Workspace sidebar/bottom nav | Shared `WorkspaceLayout` hiển thị wordmark chính xác `tutormatch`; link và nhóm tác vụ lấy role từ `shared/lib/navigation.ts`. Desktop có sidebar; mobile có bốn lối tắt cùng `Thêm` để tới mọi route. | Active route đúng cả trang detail; mọi role giữ wordmark lowercase; menu đóng trả focus; safe-area không che content. Sửa shell thì smoke-test Student, Teacher, Admin. |
| Button/link | Primary vàng viền mực; secondary viền hoặc nền trung tính; tertiary là liên kết chữ. Nhãn mô tả action, không gắn `→` chỉ để trang trí. | Disabled/loading chống gửi lặp; focus rõ; external/internal link đúng semantics. |
| Form/filter | Label ngoài control; hint và lỗi sát field; filter có trạng thái đã áp dụng và cách xóa; submit/clear theo logic route hiện có. | Validation không làm mất input; loading/error có chỗ cố định; mobile filter drawer có apply/reset rõ. |
| Subject/tutor row | Subject chọn được bằng toàn vùng chạm và tên/cấp học thật. Tutor row ưu tiên ảnh, tên, môn, rating/giá khi DTO có; preview không nhân đôi hồ sơ. | Keyboard và touch mở được; preview đóng trả focus; thiếu trường thì bỏ trường, không bịa fallback. |
| Data list/table | Dùng hàng ngắn, cột số căn phải, status chữ + màu, filter/sort/pagination sát danh sách. Mobile đổi sang hàng xếp dọc cùng thứ tự thông tin. | Empty, loading, error tách biệt; pagination/filter vẫn dùng API/query key hiện có. |
| Modal/drawer | Dùng cho quyết định hoặc thông tin thứ cấp; title nêu resource/action. Admin xem đủ bằng chứng và context trước action. | Focus trap, escape/close, trả focus, pending/409/refetch, xác nhận khi action có hậu quả. |
| Feedback | Skeleton tương ứng layout; alert lỗi có retry; empty có CTA hợp quyền; toast chỉ xác nhận kết quả server. | Không biến network failure thành `0` hoặc danh sách trống; 5xx chỉ hiện request ID theo transport hiện có. |
| Chat/notification | Message là luồng đọc; trạng thái optimistic rõ. Notification item có action semantic và trạng thái đã đọc. | Giữ retry `clientMessageId`, attachment purpose và scroll khi tải lịch sử; mark read sau API success. |

### Thông báo dùng chung theo vai trò

- `NotificationList` nhận audience rõ (`STUDENT` hoặc `TEACHER`); nhãn category và mô tả dùng từ vựng đúng vai trò (`Lịch học`/`Lịch dạy`, `Tài chính`/`Thu nhập`).
- Chỉ render notification thành link khi `referenceUrl` là URL cùng origin dưới prefix route của audience. URL external, protocol-relative và đường dẫn của vai trò khác chỉ hiển thị nội dung.
- Giữ nguyên category query, pagination, `PATCH` từng mục, `POST` đọc tất cả và chỉ đổi trạng thái sau khi API thành công. Workspace shell sở hữu landmark `main`; component này dùng `section` để tránh landmark lồng nhau.

### Quy tắc tái sử dụng

Sửa `Navbar`, `WorkspaceLayout`, button/feedback chung một lần rồi kiểm tra mọi route dùng chúng. Không đưa logic role hoặc API của một page vào shared presentation. Với page wrapper mỏng, viết lại feature component đang render UI; giữ wrapper nếu nó chỉ truyền props hoặc áp guard.
