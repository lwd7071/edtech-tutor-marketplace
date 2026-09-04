# Báo cáo Tiến độ Frontend - Thành viên A (Nhiệm vụ A1.1 -> A1.3)

## Tổng quan
Quá trình khởi tạo nền tảng Frontend cho dự án Edtech Tutor Marketplace đã hoàn thành các bước cơ sở (A1.1, A1.2, A1.3) bằng công nghệ Next.js (App Router), Ant Design và Zustand, áp dụng nghiêm ngặt theo **Test-Driven Development (TDD)** và hệ thống Design Tokens.

## A1.1: Cấu trúc thư mục & Kiến trúc Feature-Sliced
- **Cấu trúc lõi**: Khởi tạo cấu trúc theo mô hình Feature-Sliced Design. Đã tạo ra các thư mục `src/features/` cho `auth`, `marketplace`, `teacher-profile`, `catalog`, `learning`, `chat`, `notifications`, `ranking`.
- **Shared Modules**: Cấu hình `src/shared/components` và `src/shared/lib` chứa các module dùng chung (axiosClient, theme config).
- **Phân tách trách nhiệm**: Mỗi tính năng hoạt động độc lập với API, store (Zustand), và UI components riêng biệt.

## A1.2: Design Tokens & Theme Configuration (Ant Design)
- **CSS Variables (globals.css)**:
  - Cài đặt toàn bộ bảng màu (brand, accent, semantic, neutral).
  - Hệ thống Typography (display → overline) và spacing scale (`space-1` đến `space-20`).
  - Hệ thống shadow, border, radius, z-index và animation duration theo đúng Spec Mục 10.
- **Ant Design Integration**:
  - Ánh xạ thành công CSS variables vào `theme.token` của Ant Design (thông qua `src/shared/lib/theme.ts`).
  - Gói trọn cấu hình trong `AppThemeProvider.tsx` giúp cô lập thư viện UI khỏi logic ứng dụng (tránh rò rỉ cấu hình Ant Design ra mọi nơi).
- **Typography Fonts**: Đã nhúng và cấu hình `Be Vietnam Pro`, `Inter`, `JetBrains Mono` tối ưu bằng `next/font`.

## A1.3: Layout & Structure (Kết nối API thực)
**Chiến lược & TDD**:
- Thiết lập Jest và React Testing Library làm "vòng lặp phản hồi" (Feedback Loop) để thực hành TDD.
- Viết test trước để mô tả hành vi giao diện (VD: ẩn hiện nút đăng nhập, khóa menu của giáo viên chưa duyệt).
- **Không dùng Mock Data**: Khởi tạo `axiosClient` và `AuthStore` (bằng Zustand) phản ánh cấu trúc dữ liệu thực tế từ backend (`API_CONTRACT.md` - object `AuthResult`), bảo đảm luồng dữ liệu thật ngay từ đầu.

**Các Component Layout đã hoàn thành**:
1. **`Navbar` (Public/Auth Zone)**:
   - Sticky header, scroll shadow.
   - Trạng thái Guest: Hiển thị các nút Đăng nhập / Đăng ký.
   - Trạng thái Authenticated: Hiển thị chuông thông báo và Dropdown Avatar.
2. **`Sidebar` (Dashboard)**:
   - Giao diện thu gọn/mở rộng, theo dõi `pathname` để hiển thị menu đang active.
   - **Tích hợp Logic Phân quyền**: Menu động cho `STUDENT` và `TEACHER`. Đặc biệt xử lý trạng thái `status: 'PENDING'` của Teacher bằng cách disable các menu nâng cao và hiển thị Tooltip "Đang chờ duyệt".
3. **`TopHeader` (Dashboard)**:
   - Navbar phía trên cho dashboard, chứa Breadcrumb (desktop), nút toggle menu (mobile) và User Profile/Notification.
4. **`BottomNavigation` (Mobile)**:
   - Navigation cố định dưới đáy màn hình trên các thiết bị mobile, tối ưu vùng an toàn (safe-area).
5. **`Footer` (Public)**:
   - Footer thông tin chung, hiển thị trên các trang public.

**Next.js Route Groups**:
- Tạo `app/(public)/layout.tsx` cho khách.
- Tạo `app/(app)/layout.tsx` cho Dashboard (bọc Sidebar, TopHeader, BottomNav).

## A1.4: Shared Primitive Components (6 States)
**Chiến lược**: Tận dụng tối đa sức mạnh của hệ thống **Ant Design Theme** (cấu hình trong `theme.ts`) để override các thuộc tính cơ bản thay vì viết lại từng component (giảm thiểu độ phức tạp và nguy cơ lỗi).
- **Form Controls & Trạng thái (States)**:
  - Bổ sung cấu hình CSS Override cho `Button`, `Input`, `Select`, `DatePicker`, `Checkbox`, `Radio`, `Switch`, `Tabs`, `Dropdown`.
  - Cập nhật file `globals.css` để thêm hành vi tương tác động như `transform: translateY(1px)` khi nút bấm ở trạng thái `:active`.
  - Thiết lập thuộc tính `min-height: 44px` trên màn hình nhỏ (<768px) để đảm bảo chuẩn **Mobile Hit-area** cho mọi form controls.
- **Custom Wrapper Components (TDD)**:
  - Khởi tạo `DebouncedSearch.tsx` (bọc `Input.Search`) tích hợp sẵn hook debounce để chống gọi API liên tục.
  - Khởi tạo `RadioCard.tsx` để biến các lựa chọn (như gói học/phương thức) thành dạng thẻ (card) với border màu tương tác.
  - Khởi tạo `FormItem.tsx` bọc lấy `Form.Item` chuẩn hóa style.
  - Khởi tạo `ResponsiveModal.tsx` để tự động biến thành Bottom Sheet trên Mobile.
  - Khởi tạo `useConfirmDialog.ts` bọc `Modal.confirm` để xử lý variant `stale` (lỗi 409).
  - Khởi tạo `ResponsiveTable.tsx` để tự động chuyển bảng thành Card List trên Mobile.
  - Khởi tạo `FileUpload.tsx` hỗ trợ Dropzone.
- Cấu hình `<AppThemeProvider>` bọc thêm `<App>` của Ant Design để cung cấp context cho Message (Toast) và Notification.
- Tất cả các Primitive components này đều được gom vào trang Sandbox (`app/sandbox/components/page.tsx`) để dễ dàng review và kiểm thử thủ công giao diện.

## Kết quả chẩn đoán (Diagnosis / Code Review)
- **Tính nhất quán**: Các component layout đều sử dụng chính xác hệ thống CSS Variables (Design tokens), không sử dụng màu "hard-code". Cấu hình Ant Design hoàn toàn override các token nội bộ.
- **Khả năng mở rộng**: Phân tầng layout bằng Route Groups giúp sau này thêm các trang public hay dashboard rất dễ dàng mà không ảnh hưởng lẫn nhau.
- **Tích hợp Backend**: Việc sử dụng Zustand dựa trên `API_CONTRACT.md` giúp frontend sẵn sàng gọi API auth thực tế mà không cần đập đi xây lại cấu trúc Layout. Trạng thái `PENDING` của Teacher được bảo vệ ngay từ Layout.
- **Kiểm thử Hồi quy (Regression & Conflict Check)**: Lệnh chẩn đoán toàn diện `npm run lint && npm run build && npm test` đã được chạy quét qua toàn bộ tiến trình từ A1.1 đến A1.4. Sau khi dọn dẹp các cảnh báo TypeScript nhỏ (liên quan đến `any` type và token dư thừa), toàn bộ hệ thống hiện tại **sạch sẽ 100%**:
  - Không có bất kỳ cảnh báo Linting nào (`0 errors, 0 warnings`).
  - Next.js Build thành công hoàn mỹ (Turbopack).
  - Tương thích tốt không có xung đột giữa các wrapper components và thư viện gốc Ant Design.
  - Các tests kiểm tra hiển thị đúng dữ liệu trên Table và Modal đều pass.
