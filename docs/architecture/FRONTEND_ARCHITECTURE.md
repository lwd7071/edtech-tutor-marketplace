# Frontend Architecture

## Ranh giới chính

- `app` định nghĩa route groups và compose màn hình. `(public)`, `(auth)` và `(workspace)` không làm thay đổi URL.
- `features` sở hữu nghiệp vụ, wire DTO, API, query keys, mutation, model và UI của từng domain.
- `shared/backend` sở hữu response envelope, error parser, API URL và transport dùng chung.
- `shared/components/layout` sở hữu public shell và workspace shell.

Luồng phụ thuộc chuẩn:

```text
app → feature component → feature data/API → shared backend transport
                     ↘ shared presentation/component
```

UI không gọi Axios trực tiếp. Admin chỉ dùng contract Finance qua `@/features/finance`. ESLint kiểm tra hai quy tắc này.

## Session và phân quyền

Session browser có ba trạng thái: `booting`, `anonymous`, `authenticated`. Public shell không chờ hydrate. Workspace guard chờ trạng thái rõ ràng, chuyển người chưa đăng nhập về login với `redirect`, và chuyển sai vai trò tới `/forbidden`.

API session công khai chỉ gồm:

```text
hydrate()
establish(authResult, remember)
rotate(accessToken, refreshToken)
clear()
```

Backend tiếp tục là nơi quyết định quyền truy cập dữ liệu.

## Data và realtime

Query key nằm trong `features/<domain>/data` và chứa mọi tham số làm thay đổi response. Mutation invalidates bằng prefix công khai của domain liên quan.

Chat tải lịch sử qua REST. STOMP chỉ quản kết nối, subscription và publish. Payload realtime được kiểm tra bằng Zod trước khi vào state; reducer thay optimistic message theo `clientMessageId`, chống trùng theo id và sắp xếp theo thời gian.

## Kiểm thử

```powershell
cd frontend
npm run typecheck
npm run lint
npm run test:ci
npm run build
```

Từ thư mục gốc:

```powershell
.\scripts\test-frontend-docker.ps1
.\scripts\test-frontend-docker.ps1 -IncludeE2E
```

Lệnh đầu chạy kiểm tra frontend trong Node container. `-IncludeE2E` mới tải image Playwright và chạy Chromium smoke tests. Script chỉ dừng container frontend do nó tạo, không xóa volume hoặc database.
