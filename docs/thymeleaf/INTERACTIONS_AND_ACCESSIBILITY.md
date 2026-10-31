# Tương tác và accessibility

## Progressive enhancement

- Tìm kiếm/bộ lọc: GET form, query params giữ trạng thái, submit tải lại trang. htmx có thể thay riêng vùng kết quả nhưng phải trả cùng một đường fallback full page.
- Tạo/cập nhật/xóa/action nghiệp vụ: HTML form và POST/redirect/GET; validation hiển thị lại form với dữ liệu đã nhập an toàn.
- Phân trang/sort/tab: link có URL rõ ràng, hỗ trợ mở tab/chia sẻ.
- Chat: form gửi tin nhắn và tải lại lịch sử khi không có JS; STOMP chỉ là tăng cường khi kết nối hoạt động.
- Trạng thái invoice: trang kiểm tra server khi tải; nút refresh/manual refresh dùng link. Polling/skeleton chỉ khi có enhancement async và phải giữ manual fallback.
- Modal/dialog: action nguy hiểm có trang xác nhận hoặc form xác nhận fallback. Không phụ thuộc hover.
- Logout: form `POST` thường có CSRF token, submit sang web handler; không dùng GET/logout link. Revoke token phụ thuộc ADR auth mới.
- Upload: `multipart/form-data` form hoạt động khi JS tắt. Giới hạn service hiện tại: teacher identity document 10 MiB; teacher credential 5 MiB; finance proof 10 MiB. File quá giới hạn phải render lỗi cụ thể và cho chọn file khác; server validation là nguồn quyết định. Chi tiết API tại [API_CONTRACT.md](../architecture/API_CONTRACT.md).

## Sau POST/redirect/GET

Sau thành công, redirect và render status message trong HTML. Sau validation/domain error, render lại form với dữ liệu không nhạy cảm, summary đầu form và lỗi cạnh field; focus summary hoặc field lỗi đầu tiên. Không phụ thuộc toast/JS để báo kết quả. Không render mật khẩu, token hoặc bytes file.

## States

Mỗi trang dữ liệu định nghĩa success, empty, validation/domain error, unauthorized/forbidden, unavailable. Loading state chỉ tồn tại cho request client-side thật; SSR error render thông báo trong trang.

Thông báo lỗi dùng mã từ [ERROR_CODES.md](../architecture/ERROR_CODES.md), không sao chép bảng mã vào đây. Tránh hiện stack trace/raw exception/provider response.

## Accessibility

- Semantic HTML, heading theo thứ tự, landmark, label liên kết input, mô tả lỗi qua `aria-describedby`.
- Bàn phím thao tác được mọi control; focus visible; focus order theo nội dung; không để nội dung hover-only.
- Tương phản WCAG AA; ảnh có alt phù hợp; trạng thái không chỉ phân biệt bằng màu.
- Tôn trọng `prefers-reduced-motion`; animation trang trí nhẹ, không trì hoãn thao tác. Không bật animation mặc định cho SSR.
- Bảng dữ liệu có heading; trên mobile chuyển sang row/card có nhãn trường và giữ thao tác.

## Responsive

Mốc cơ sở 640, 768, 1024 CSS px; reflow không mất chức năng ở 320px. Bộ lọc mobile phải dùng form truy cập được; không bắt buộc drawer JS.
