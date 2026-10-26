# Checklist nghiệm thu từng route UI

Một route chỉ được đánh dấu `PASS` trong `IMPLEMENTATION_PLAN.md` khi **mọi** mục dưới đây có bằng chứng. `PENDING`, `BLOCKED` và `FAIL` không được tính hoàn thành.

1. **Đúng đặc tả:** route, role, mục đích, thứ tự vùng, hierarchy, component và reference đã được duyệt khớp `PAGE_DESIGN_PLAN.md`. Cây JSX/layout chính được xây lại; không giữ page cũ rồi chỉ đổi spacing/màu.
2. **Ngưỡng rewrite:** ít nhất 60% JSX/layout render ban đầu của mỗi UI owner được thay/xóa vì cấu trúc hoặc hierarchy, xác nhận bằng diff và review. Với source đọc được, đo `R/N` trên các dòng JSX/layout render gốc; không tính props, API, state/handler/business logic, imports, dòng trống, format, rename, di chuyển logic hoặc wrapper mỏng. Với source minified khiến đếm dòng không có ý nghĩa, dùng `node scripts/measure-jsx-layout-rewrite.mjs <baseline.tsx> <current.tsx>`: parser TypeScript so chữ ký cây JSX gồm tag, className/role/aria-label và cấu trúc con; so sánh theo số lần xuất hiện, không tính text/handler/whitespace. Báo kết quả là `subtree R/N` để phân biệt với `line R/N`; reviewer vẫn xác nhận thay đổi làm rõ cấu trúc/hierarchy thật. Nếu JSX chính nằm ở nhiều file, ghi từng UI owner và R/N riêng; mọi owner chính phải đạt ngưỡng. Báo thêm diff toàn file để tham khảo nhưng không dùng phần logic được yêu cầu giữ nguyên làm mẫu số.
3. **Contract:** props, API function, payload, handler, state transition, auth/role guard, query/return URL và navigation giữ hành vi. Backend vẫn quyết định ownership. Không thêm endpoint, dữ liệu giả hoặc business rule mới.
4. **Trạng thái:** populated, loading, empty, error, success và trạng thái nghiệp vụ có trong page đều có UI đúng. Lỗi không hiện là dữ liệu 0/trống. Mutation pending chống gửi lặp; 409/refetch hoặc retry hoạt động ở nơi hiện có.
5. **Ảnh thực tế:** lưu ảnh trước/sau tại 375, 768, 1280px cho route và ghi đường dẫn. Đối chiếu vùng chính, text dài, không tràn ngang, focus, touch, drawer/modal, safe-area và reduced motion với reference. Ảnh chụp phải từ bản app chạy, không từ comp.
6. **Kiểm thử:** chạy test component/E2E liên quan và ghi lệnh, số pass/fail; kiểm tra thao tác chính bằng browser. Sau mỗi nhóm chạy `npm run typecheck`, `npm run lint`, `npm run test:ci`, `npm run build` và E2E khả thi. Compile/build không thay cho kiểm thử hành vi.
7. **Đóng page:** cập nhật hàng route trong `IMPLEMENTATION_PLAN.md` với file, phần trăm, ảnh, test và trạng thái; cập nhật `docs/STATUS.md` theo kết quả thật. Component dùng chung đổi thì kiểm tra các route phụ thuộc trước khi chuyển page.

### Cách tính ngưỡng

Chốt commit/working-tree baseline trước khi bắt đầu page và lưu số dòng JSX/layout render gốc `N` của mỗi UI owner. Với source minified, dùng AST subtree fallback nêu ở mục 2 và ghi rõ đơn vị đo. Yêu cầu `R / N >= 0.60`; reviewer xác nhận thay đổi tạo cấu trúc/hierarchy thật, không phải whitespace, class/CSS đơn lẻ, logic hoặc di chuyển nguyên khối. Ghi thêm số dòng toàn file và diff toàn file để minh bạch phần contract được giữ nguyên. File mới không tự làm một route cũ đạt ngưỡng; phải đối chiếu file UI owner gốc đã bị thay.

### Bằng chứng tối thiểu một hàng route

`Route | Reference approval | UI owner | R/N | 375 before/after | 768 before/after | 1280 before/after | tests | behavior/browser | status`.
Để trống bất kỳ ô bằng chứng nào thì trạng thái giữ `PENDING`.
