# Kiểm thử và nghiệm thu

## Nghiệm thu theo route

Route chỉ hoàn thành khi có nội dung SSR thật, role đúng, liên kết/form hoạt động, trạng thái rỗng/lỗi/validation, responsive, keyboard/focus/labels, không lộ dữ liệu riêng tư và dùng được khi JavaScript tắt. Danh mục route là [ROUTES_AND_PAGES.md](ROUTES_AND_PAGES.md); PROGRESS chỉ ghi trạng thái route.

## Lớp kiểm thử

- MockMvc: render view với model, form binding/validation, redirect PRG, login/role guard, CSRF và no-store proof.
- Playwright: hành trình marketplace → mua package/invoice → xác minh thanh toán → đặt booking; guest/auth/role redirects; teacher/admin actions.
- Tắt JavaScript trong Playwright cho search/filter/pagination, form mutations, payment refresh, chat submit/reload và upload/download.
- Accessibility tự động bằng axe trên public, Student, Teacher, Admin mẫu; kiểm tra thủ công bàn phím, zoom/reflow và reduced motion.
- Với list/search/dashboard, đo query count theo page size, kiểm tra N+1 khi render Thymeleaf và xác minh lazy associations không tải ngầm qua Open Session in View. Dùng projection/view model; template không truy cập lazy relation.
- Dùng test data/fake provider; không gọi provider production hoặc Supabase trong kiểm thử UI thông thường.

## Acceptance criteria

- Mọi route sản phẩm trong sitemap được map tới trang Thymeleaf hoặc có quyết định loại bỏ được ghi nhận.
- Luồng chính không tạo booking Student trực tiếp nếu API/service không hỗ trợ; endpoint đó phải được xác minh trước khi tuyên bố luồng hoàn chỉnh.
- Học thử được nghiệm thu riêng sau khi xác minh UI flow và state transition thật.
- Mỗi nhóm route có smoke test tối thiểu một list, detail, form mutation và role guard; finance/upload/proof có test riêng multipart, authorization và no-store.
- Payment success chỉ dựa trên trạng thái backend invoice/package, không dựa query redirect.
- Test failure/blocked được ghi rõ trong [docs/STATUS.md](../STATUS.md), không ghi pass chỉ vì compile/static check.

## Phân tầng kiểm thử cho toàn bộ FE

1. **Backend web:** MockMvc theo controller/page family; status, template, role, CSRF, binding, validation, redirect và content safety.
2. **UI component/template:** render fragment với data thật từ fixture; missing/null fields; dài tiếng Việt; formatting tiền/ngày.
3. **E2E theo luồng:** public search→profile→purchase/payment; auth/OAuth; Student learning/requests/chat; Teacher profile/package/booking/finance; Admin approval/finance/audit. Luồng booking Student chỉ đưa vào E2E khi endpoint/service được xác minh.
4. **No-JS:** GET search/filter/page; POST+PRG mọi mutation; chat submit/reload; payment status refresh; multipart upload/download.
5. **Matrix:** Guest/Student/Teacher/Admin, anonymous/wrong-role/owner/non-owner; 320/640/768/1024/1280px; JS enabled/disabled; reduced motion.

Mỗi feature PR ghi test cụ thể theo route được đổi. Full suite chạy trước release; không dùng một smoke test mỗi role làm bằng chứng thay cho route-level coverage.
