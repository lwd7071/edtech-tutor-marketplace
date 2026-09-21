# Frontend Architecture

## Ranh giới chính

- `app` định nghĩa route groups và compose màn hình. `(public)`, `(auth)` và `(workspace)` không làm thay đổi URL.
- `features` sở hữu nghiệp vụ, wire DTO, API, query keys, mutation, model và UI của từng domain.
- `shared/backend` sở hữu response envelope, error parser, API URL và transport dùng chung.
- `parseApiError` đọc `X-Request-Id` từ response và chỉ nối mã tra cứu vào thông báo lỗi `5xx`; luồng nghiệp vụ luôn dựa vào `errors[].code`.
- `shared/components/layout` sở hữu public shell và workspace shell.

Luồng phụ thuộc chuẩn:

```text
app → feature component → feature data/API → shared backend transport
                     ↘ shared presentation/component
```

UI không gọi Axios trực tiếp. Admin chỉ dùng contract Finance qua `@/features/finance`. ESLint kiểm tra hai quy tắc này.

## Session và phân quyền

Session browser có ba trạng thái: `booting`, `anonymous`, `authenticated`. Public shell không chờ hydrate. Workspace guard chờ trạng thái rõ ràng, chuyển người chưa đăng nhập về login với `redirect`, và chuyển sai vai trò tới `/forbidden`.

Sau khi login, return URL được kiểm tra cùng lúc với `AuthResult.user.role`: Student chỉ nhận workspace `/student`, Teacher chỉ nhận `/teacher`, Admin chỉ nhận `/admin`; URL public được phép theo allowlist. Query/hash chỉ được giữ theo allowlist của từng flow, và các tham số lồng như `redirect`, `next`, `returnTo`, `callbackUrl` bị loại bỏ. Login và logout của tab nguồn tự điều hướng bằng `router.replace`, không chờ sự kiện từ tab khác.

Session persisted có `sessionId` sinh bằng `crypto.randomUUID()`; local HTTP test/dev dùng UUID-compatible `crypto.getRandomValues()` fallback nếu browser không expose `randomUUID()` ngoài secure context. Mỗi establish/logout/rotate ghi một revision không chứa token vào `localStorage`; tab khác nghe `storage`, còn tab nguồn xử lý ngay tại call site. `visibilitychange` đồng bộ lại session khi tab được mở lại. Khi danh tính đổi hoặc logout, React Query dùng `queryClient.clear()` toàn bộ.

Axios phải gắn snapshot `sessionId` vào request và snapshot refresh token khi bắt đầu refresh. Chỉ phiên có cùng `sessionId` và refresh token hiện tại mới được rotate hoặc clear; response refresh cũ phải bị bỏ qua để không ghi đè phiên đăng nhập mới. Giả định hiện tại là một origin dùng một danh tính; multi-account switch không qua logout chưa phải capability được hỗ trợ.

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

Public Server Components dùng `shared/api/public.server.ts` với native `fetch` và Next Data Cache. Subjects, teacher profile/list và packages dùng TTL 300 giây; availability, reviews và ranking dùng TTL 60 giây. Mỗi server-side fetch có timeout 10 giây để backend cold start hoặc outage không làm Vercel prerender treo quá giới hạn build; route public dùng `Promise.allSettled` để render fallback rỗng khi request lỗi. Browser Axios adapter không được import vào server adapter. Dữ liệu cá nhân của workspace dùng React Query; Student dashboard gọi một read-model endpoint và cache client tối đa 30 giây.

Next Data Cache hỗ trợ on-demand revalidation qua Route Handler nội bộ `POST /api/internal/revalidate-public`, nhận callback từ backend sau khi transaction commit:
- Xác thực bằng shared secret qua header `x-internal-secret` (lưu tại biến môi trường `APP_INTERNAL_REVALIDATE_SECRET`, không phơi bày qua `NEXT_PUBLIC_*`).
- Tag mapping:
  - Credential được duyệt hoặc credential đã duyệt bị sửa/xóa: invalidate `public-teacher:{id}` qua `revalidateTag(tag, { expire: 0 })`.
  - Nơi ở gia sư thay đổi: invalidate `public-teacher:{id}` và `public-teachers` để trang tìm kiếm cập nhật ngay.
  - Tạo credential mới ở trạng thái `PENDING` không đổi dữ liệu công khai nên không invalidate tag.
- TTL hiện tại (60–300s) đóng vai trò fallback bảo đảm dữ liệu luôn tươi mới ngay cả khi callback nội bộ timeout hoặc gặp sự cố mạng tạm thời.

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
