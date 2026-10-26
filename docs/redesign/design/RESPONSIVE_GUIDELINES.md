# Quy tắc responsive

Kiểm tra tối thiểu tại **375px, 768px, 1280px** với nội dung thật hoặc fixture khớp DTO. Breakpoint là quyết định theo độ rộng hữu dụng của nội dung; không dựa vào loại thiết bị để ẩn chức năng.

| Nhóm | 1280px | 768px | 375px |
|---|---|---|---|
| Public | Hero chữ và visual hai vùng; chọn môn nhiều cột; tìm gia sư có filter, list và preview. | Hero/subject chia ít cột hơn; search có filter gọn, preview không ép list quá hẹp. | Một trục đọc; CTA ở đầu; subject một cột; filter drawer, preview dưới hàng được chọn. |
| Auth | Form tập trung với vùng diễn giải phụ; phần thao tác trong chiều cao thuận tiện. | Form là vùng chính, vùng phụ thu nhỏ. | Một cột; label/error/submit cùng trục; không phải cuộn qua minh họa mới thấy submit. |
| Student | Việc tiếp theo ưu tiên; dữ liệu phụ theo cụm; detail chia phần đọc và hành động. | Hai vùng khi đủ chỗ, action vẫn gần context. | Một cột theo thứ tự nhiệm vụ; bottom nav không che action, bảng/list thành hàng xếp dọc. |
| Teacher | Lưới quản lý tác vụ; bảng hoặc lịch đủ không gian; sidebar theo workflow. | Sidebar thu gọn, form/list giữ độ rộng đọc được. | Header và bottom nav; nội dung một cột; lịch có cuộn ngang có nhãn hoặc dạng ngày, không cắt ô. |
| Admin | Filter + bảng + drawer context; mật độ cao nhưng đọc được. | Bảng giảm cột phụ, drawer rộng hợp lý. | Filter thu gọn; mỗi record là hàng dọc có status và action; drawer toàn chiều rộng, context trước decision. |

## Quy tắc cố định

- Không có horizontal overflow ngoài vùng bảng/lịch được ghi nhãn cuộn. Body giữ width viewport; ảnh và text dài co/đổi dòng an toàn.
- Touch target tối thiểu khoảng 44px cho action chính; khoảng cách đủ tránh chạm nhầm. Sticky CTA chỉ khi không che nội dung, toast, bàn phím hoặc bottom nav.
- Modal/drawer tôn trọng safe-area; bàn phím ảo không che submit. Ở zoom chữ 200%, nội dung và hành động vẫn tới được.
- Thứ tự DOM theo thứ tự đọc mobile; desktop chỉ đổi bố cục bằng CSS, không tạo bản sao nội dung gây focus trùng. `prefers-reduced-motion` tắt dịch chuyển/reveal nhưng giữ nội dung nhìn thấy.
- Ảnh chụp kiểm tra gồm trạng thái populated và ít nhất một loading/empty/error phù hợp route; ghi nguồn fixture hoặc API thật, không gọi fixture là dữ liệu production.
