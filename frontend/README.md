# Tutor Match Frontend

Frontend của nền tảng kết nối học viên với gia sư 1-1. Cấu trúc giao diện, danh sách trang và phân quyền nằm tại [`docs/planning/SPEC-FE.md`](../docs/planning/SPEC-FE.md).

## Công nghệ

- Next.js 16 App Router, React 19 và TypeScript
- Ant Design 6
- TanStack Query, Axios và Zustand
- STOMP.js cho chat realtime
- Jest và Testing Library

## Chạy dự án

```bash
npm install
npm run dev
```

Mở `http://localhost:3000`. Backend mặc định chạy tại `http://localhost:8080`.

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

Hai biến trên là tùy chọn. WebSocket mặc định dùng `/ws` trên host hiện tại.

## Kiểm tra trước khi bàn giao

```bash
npm run lint
npx tsc --noEmit
npm test
npm run build
```

Baseline hiện tại: 96 test suites, 245 tests và 54 routes build thành công.

## Cấu trúc

```text
src/
├── app/          Route, layout và guard theo vai trò
├── features/     Nghiệp vụ theo domain
└── shared/       API client, component, store và tiện ích dùng chung
```

Các domain chính gồm `auth`, `marketplace`, `teacher-dashboard`, `student-packages`, `bookings`, `learning`, `chat`, `notifications`, `payments`, `finance`, `ranking` và `admin`.

## Quy ước

- Page trong `app` chỉ ghép layout, guard và feature component; nghiệp vụ đặt trong `features`.
- API dùng `shared/api/axiosClient.ts`; không tạo Axios client riêng trong component.
- DTO và payload phải khớp Backend. Không dùng dữ liệu giả để che response thiếu trường.
- Trang lấy dữ liệu phải có loading, error/retry và empty state.
- UI ẩn hành động sai quyền; route vẫn phải có guard khi truy cập URL trực tiếp.
- Chỉ điều hướng `referenceUrl` từ Backend khi là đường dẫn nội bộ bắt đầu bằng `/`.
- Nội dung dùng thống nhất: **gia sư**, **học viên**, **quản trị viên**.
- Public/student ưu tiên mobile; teacher/admin ưu tiên desktop nhưng vẫn phải dùng được trên màn hình nhỏ.

## Tài liệu nguồn

- [Đặc tả Frontend](../docs/planning/SPEC-FE.md)
- [API contract](../docs/architecture/API_CONTRACT.md)
- [Mã lỗi](../docs/architecture/ERROR_CODES.md)
- [Thiết lập toàn dự án](../docs/guidelines/SETUP.md)
- [Coding convention](../docs/guidelines/CODING_CONVENTION.md)
