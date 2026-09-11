# Tutor Match Frontend

Frontend của nền tảng kết nối học viên với gia sư 1-1. Cấu trúc giao diện, danh sách trang và phân quyền nằm tại [`docs/architecture/FRONTEND_SPEC.md`](../docs/architecture/FRONTEND_SPEC.md).

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

Sao chép `.env.example` thành `.env.local` trước khi chạy local. `NEXT_PUBLIC_API_URL` là tùy chọn khi backend chạy ở `http://localhost:8080`. Khi Next.js và backend chạy khác origin/port (ví dụ local `3000` và `8080`), phải đặt `NEXT_PUBLIC_WS_URL`; nếu không, frontend sẽ thử kết nối `/ws` trên Next.js (`localhost:3000`) thay vì backend.

## Kiểm tra trước khi bàn giao

```bash
npm run lint
npm run typecheck
npm run test:ci
npm run build
```

Hoặc chạy toàn bộ kiểm tra nhẹ trong Docker từ thư mục gốc:

```powershell
.\scripts\test-frontend-docker.ps1
```

Smoke test bằng Chromium được tách riêng vì image Playwright khá lớn:

```powershell
.\scripts\test-frontend-docker.ps1 -IncludeE2E
```

Baseline hiện tại: 97 test suites, 248 tests và 55 routes build thành công.

## Cấu trúc

```text
src/
├── app/
│   ├── (public)/       Marketplace công khai
│   ├── (auth)/         Đăng nhập, đăng ký và OAuth
│   └── (workspace)/    Student, Teacher và Admin; URL không chứa tên group
├── features/           API, data/query, model và UI theo nghiệp vụ
└── shared/
    ├── backend/        Contract, error parser và cấu hình Backend
    └── components/     UI và shell dùng chung
```

Các domain chính gồm `auth`, `marketplace`, `teacher-dashboard`, `student-packages`, `bookings`, `learning`, `chat`, `notifications`, `payments`, `finance`, `ranking` và `admin`.

## Quy ước

- Page trong `app` chỉ ghép layout và feature component; guard vai trò đặt tại workspace layout.
- Component/page không import Axios trực tiếp. Mọi request đi qua API của feature.
- Query key được sở hữu bởi feature và phải chứa đủ filter, pagination và sort.
- Session chỉ dùng `hydrate`, `establish`, `rotate`, `clear` từ `features/auth`.
- Module khác chỉ import Finance qua public entrypoint `@/features/finance`.
- DTO và payload phải khớp Backend. Không dùng dữ liệu giả để che response thiếu trường.
- Trang lấy dữ liệu phải có loading, error/retry và empty state.
- UI ẩn hành động sai quyền; route vẫn phải có guard khi truy cập URL trực tiếp.
- Chỉ điều hướng `referenceUrl` từ Backend khi là đường dẫn nội bộ bắt đầu bằng `/`.
- Nội dung dùng thống nhất: **gia sư**, **học viên**, **quản trị viên**.
- Public/student ưu tiên mobile; teacher/admin ưu tiên desktop nhưng vẫn phải dùng được trên màn hình nhỏ.

## Tài liệu nguồn

- [Đặc tả Frontend](../docs/architecture/FRONTEND_SPEC.md)
- [API contract](../docs/architecture/API_CONTRACT.md)
- [Mã lỗi](../docs/architecture/ERROR_CODES.md)
- [Thiết lập toàn dự án](../docs/guidelines/SETUP.md)
- [Coding convention](../docs/guidelines/CODING_CONVENTION.md)
