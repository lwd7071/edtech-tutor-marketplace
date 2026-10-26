# Hệ thiết kế tutormatch

## Nguồn quyết định

`CONTEXT.md`, ADR 0015 và `frontend/src/shared/design-system/tokens.ts` là nguồn hiện hành. Reference P/A/S/T/X đã được duyệt. Chỉ dùng phần bố cục đã duyệt. Chữ, người, thời gian, giá, số liệu và thao tác do ảnh sinh ra không phải dữ liệu hay contract. Tham khảo Preply về nhịp đọc, tìm kiếm và mật độ thông tin; giao diện vẫn phải phục vụ đúng các route và vai trò của tutormatch.

## Nhận diện

- Wordmark hiển thị chính xác `tutormatch`, toàn chữ thường, liền một từ, không thêm dấu chấm, hình tròn hoặc biểu tượng. Chữ có thể dùng hai sắc mực/vàng nếu vẫn đọc thành một từ. Accessible name cũng là `tutormatch`.
- Vàng `#FFD43B`, mực `#171714`, nền `#F7F6F0`, surface `#FFFEFA`. Lấy các màu qua token semantic, không khai báo palette thứ hai. Success, warning, error và info dùng token ngữ nghĩa riêng, luôn kèm nhãn chữ.
- Be Vietnam Pro cho heading, Inter cho nội dung. Hero desktop 48–56px, mobile 34–40px; heading section desktop 30–36px, mobile 26–30px; body 15–16px; metadata hành động tối thiểu 13–14px. Giữ line-height đủ cho dấu tiếng Việt.
- Nhịp spacing bội số 8px, có thể dùng nửa bước 4px cho icon/label. Nội dung public rộng tối đa khoảng 1200px; workspace ưu tiên độ dài dòng và mật độ tác vụ thay vì lấp đầy màn hình.
- Radius nhỏ đến vừa theo loại control; không dùng capsule cho mọi thành phần. Chỉ dùng border khi cần tách tác vụ hoặc nhóm dữ liệu. Shadow rất nhẹ cho lớp nổi như modal/preview; không áp cho mọi hàng. Không dùng gradient làm nền trang.

## Thứ bậc và hành động

- Mỗi vùng nhiệm vụ có một hành động chính rõ bằng nhãn chữ, ví dụ `Tìm gia sư`, `Đăng ký`, `Lưu`, `Duyệt`. Button và navigation không tự thêm mũi tên trang trí. Icon chỉ dùng khi nó phân biệt chức năng, trạng thái hoặc điều hướng có nghĩa; icon-only cần accessible name.
- Trang public dẫn theo lựa chọn môn → tìm và so sánh gia sư → hồ sơ → học thử hoặc gói. Workspace dẫn theo việc tiếp theo của đúng vai trò. Admin đặt bằng chứng và trạng thái trước quyết định.
- Không biến mọi phần thành card. Dùng section, hàng dữ liệu, divider, bảng hoặc panel tùy nhiệm vụ. Một page có thể có bố cục riêng mà vẫn dùng cùng token, typography và component contract.
- Copy UI dùng thuật ngữ trong `CONTEXT.md`; không đưa lời hứa, testimonial, số lượng người dùng, giá hoặc xếp hạng không có API tương ứng.

## Trạng thái và tương tác

- Loading giữ cấu trúc vùng nội dung; error nêu tác vụ không tải được và có `Thử lại` khi retry hợp lệ; empty mô tả chưa có dữ liệu và bước tiếp theo hợp quyền; success chỉ xuất hiện sau xác nhận từ server.
- Focus nhìn thấy rõ trên nền vàng và nền sáng. Hover không phải cách duy nhất mở thông tin; touch, keyboard và reduced motion có đường tương đương. Overlay đóng xong trả focus về tác nhân mở.
- Hiển thị tiền là VND nguyên và thời gian theo `Asia/Ho_Chi_Minh`; giữ ý nghĩa trạng thái settlement, booking, assignment và approval từ backend.

## Khi dùng reference mới

Reference theo nhóm chỉ khóa phần bố cục sau khi người dùng duyệt. Nếu ảnh và page spec khác nhau, page spec cùng API contract thắng về nội dung và hành vi; ảnh được sửa hoặc chú thích trước khi code. Thay đổi palette/font cần quyết định ADR mới; phiên này giữ chúng cố định.
