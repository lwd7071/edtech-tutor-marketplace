# Refactor Frontend Tutor Match theo Clean Code và SOLID

## 1. Mục tiêu và baseline

Refactor Frontend Tutor Match theo hướng module sâu, interface nhỏ, dễ kiểm thử và dễ mở rộng; giữ nguyên URL, REST contract, STOMP destination và hành vi nghiệp vụ hiện tại.

Hiện trạng đã khảo sát:

- Next.js 16 App Router, React 19, TypeScript, Ant Design 6, TanStack Query, Axios, Zustand và STOMP.js.
- 63 route pages, 293 file TSX và 98 test files.
- `npx tsc --noEmit` hiện pass.
- Jest chưa chạy được trong sandbox Codex do `spawn EPERM`; phải chạy lại trên máy phát triển.
- Có ba định nghĩa `ApiResponse` khác nhau.
- Có hai đường import Axios và hai đường import auth store; một số chỉ là re-export nhưng làm interface bị phân tán.
- Có page và module giao diện gọi Axios trực tiếp.
- Session trải trên Zustand, cookie, localStorage, `AuthProvider`, Axios interceptor và STOMP.
- Root `AuthProvider` chặn cả public site trong lúc hydrate.
- Chat tự quản REST state, STOMP state, dedupe và ordering.
- Admin import formatter và status renderer từ implementation của Finance.
- Có hai implementation `NotificationBell`.
- Một số table/modal đạt 250–300 dòng vì trộn render, form state, mutation và formatter.
- ESLint đang tắt `no-unused-vars`, `no-explicit-any` và `no-img-element`.
- Jest dùng `--forceExit`, có thể che timer hoặc socket chưa được cleanup.

Nguyên tắc thực hiện:

- Characterization test trước khi di chuyển code.
- Không tạo interface cho mọi file; chỉ tạo seam khi có từ hai adapter hoặc có dependency ngoài thật.
- Page trong `app` chỉ compose route và feature page.
- Không đổi giao diện chỉ để phục vụ refactor.
- Không đổi REST endpoint, response contract Backend hoặc STOMP destination.
- Không đưa `.env.cloud`, test credential hoặc secret vào Git.
- Mỗi chặng phải có commit độc lập và test liên quan chạy xanh trước khi sang chặng sau.

## 2. Chặng 0 — Characterization và baseline

Chạy từ `D:\EdTech\frontend`:

```powershell
npm ci
npm run lint
npx tsc --noEmit
npm test -- --runInBand --detectOpenHandles
npm run build
```

Ghi lại:

- Tổng suite/test.
- Test fail và root cause đầu tiên.
- Số route build thành công.
- Open handle khiến Jest cần `--forceExit`.
- Bundle client lớn nhất.
- Request/response thực tế của Auth, Booking, Finance và Chat.

Bổ sung characterization test cho:

- Refresh đồng thời chỉ gửi một request.
- Refresh thất bại xóa toàn bộ session.
- OAuth existing user và role selection.
- Guard anonymous, sai role và teacher chưa duyệt.
- Response envelope có và không có pagination.
- Chat message trùng `clientMessageId`.
- Notification chỉ mở `referenceUrl` nội bộ.

Commit:

```text
test(frontend): characterize session transport and route behavior
```

## 3. Chặng 1 — Một Backend transport module

Tạo cấu trúc:

```text
src/shared/backend/
├── contracts.ts
├── BackendClient.ts
├── AxiosBackendClient.ts
├── errors.ts
└── pagination.ts
```

Cách sửa:

- Chỉ giữ một `ApiResponse<T>`, `PageMeta` và `ParsedApiError`.
- Axios adapter chịu trách nhiệm unwrap response envelope.
- Feature nhận trực tiếp `T` hoặc `PageResult<T>`, không nhận `response.data.data`.
- Gom refresh queue và lỗi mạng vào Axios adapter.
- Không gọi Axios trực tiếp trong page hoặc module hiển thị.
- Xóa `shared/lib/axiosClient.ts` sau khi chuyển hết import.
- Thay `any` trong auth payload bằng DTO cụ thể.
- Chỉ dùng Zod cho dữ liệu không đáng tin như OAuth query và STOMP payload; không parse lặp toàn bộ response đã có TypeScript contract.

Test:

- Success response, paginated response và no-content response.
- Validation, 401, 403, 404, 409 và network error.
- Refresh concurrent, retry request ban đầu và logout khi refresh token hỏng.
- Upload giữ đúng content type.

Commit:

```text
refactor(frontend-api): centralize backend transport contracts
```

## 4. Chặng 2 — Client session module

Tạo cấu trúc:

```text
src/features/auth/session/
├── sessionStore.ts
├── sessionPersistence.ts
├── sessionLifecycle.ts
├── selectors.ts
└── SessionHydrator.tsx
```

Interface session chỉ gồm:

```text
hydrate()
establish(authResult, remember)
rotate(tokens)
clear()
```

Cách sửa:

- Một module sở hữu Zustand, cookie và localStorage.
- Dùng trạng thái `booting | anonymous | authenticated`.
- Xóa các mutation trùng nhau: `logout/clearAuth`, `setToken/setTokens/setAuth`.
- Axios và STOMP lấy token qua cùng session interface.
- Refresh rotation cập nhật access token và refresh token atomically.
- Public page không hiển thị loading toàn trang khi hydrate.
- Guard chỉ điều khiển điều hướng UX; Backend vẫn quyết định quyền truy cập dữ liệu.
- Giữ REST contract hiện tại.

Giới hạn: token hiện nằm trong cookie đọc được bằng JavaScript. Refactor FE không thể chuyển thành HttpOnly nếu không thay đổi Backend hoặc thêm BFF; không giả định vấn đề này đã được xử lý.

Test:

- Hydrate session hợp lệ.
- Cookie có token nhưng user trong localStorage hỏng.
- Remember session.
- Rotate token.
- Logout xóa toàn bộ storage.
- Không flash protected content khi session đang `booting`.

Commit:

```text
refactor(auth-ui): consolidate client session lifecycle
```

## 5. Chặng 3 — Route groups và workspace shell

Chuyển route về cấu trúc sau nhưng giữ nguyên URL:

```text
src/app/
├── (public)/
├── (auth)/
└── (workspace)/
    ├── student/
    ├── teacher/
    └── admin/
```

Cách sửa:

- Root layout chỉ giữ theme và Query provider cần thiết.
- Session hydrator không khóa public content.
- Protected workspace mới hiển thị loading khi xác minh session.
- Gom `shared/components/layout` và `shared/components/layouts` thành một đường dẫn.
- Xóa `StudentAppLayout` vì chỉ chuyển tiếp sang `WorkspaceLayout`.
- Navigation config giữ role, label, icon và active-match rule tại một module.
- `app/**/page.tsx` chỉ compose feature page, không fetch hoặc chứa nghiệp vụ.

Test:

- URL không thay đổi sau khi chuyển route group.
- Deep link vào Student, Teacher và Admin.
- Sai role tới `/forbidden`.
- Redirect login giữ pathname và search params.
- Navigation active đúng ở detail page.

Commit:

```text
refactor(shell): consolidate workspace routing and navigation
```

## 6. Chặng 4 — Feature data modules

Mỗi feature tổ chức data theo mẫu:

```text
src/features/bookings/data/
├── bookingQueries.ts
├── bookingMutations.ts
└── bookingKeys.ts
```

Áp dụng cho Booking, Student Packages, Payment, Finance, Admin, Learning, Marketplace và Ranking.

Cách sửa:

- Query key được tạo tại một nơi.
- Mutation module sở hữu cache invalidation.
- Không hard-code key như `['wallet']` hoặc `['student-packages']` trong feature khác.
- Page không tự quản `loading/error/data` khi TanStack Query đã quản lý.
- Không tạo hook mỏng cho từng endpoint nếu chỉ chuyển tiếp; ưu tiên export `queryOptions` có thể dùng lại.
- Chuẩn hóa `PageResult<T>` để pagination luôn zero-based theo Backend.

Test:

- Query key ổn định và chứa đủ pagination/filter/sort.
- Mutation invalidates đúng list, detail và dashboard.
- Không invalidate toàn bộ cache không cần thiết.
- Loading, error/retry và empty state của mỗi feature.

Commits:

```text
refactor(bookings-ui): centralize queries mutations and cache effects
refactor(finance-ui): centralize queries mutations and cache effects
refactor(learning-ui): centralize assignment data flow
refactor(marketplace-ui): centralize public search data flow
```

## 7. Chặng 5 — Chat và Notification

Tạo communication module:

```text
src/features/communication/
├── data/
├── realtime/
│   ├── RealtimeClient.ts
│   ├── StompRealtimeClient.ts
│   └── FakeRealtimeClient.ts
├── model/
│   ├── messageReducer.ts
│   └── schemas.ts
└── components/
```

Cách sửa:

- REST load lịch sử vào Query cache.
- STOMP adapter chỉ quản connection và subscription.
- Message reducer quản ordering, dedupe và seen state.
- Không expose `lastMessage: any`.
- Validate STOMP payload trước khi ghi cache.
- Reconnect không tạo subscription trùng.
- Token rotation làm STOMP reconnect bằng token mới.
- Chỉ giữ một `NotificationBell`.
- Notification list và bell dùng chung query/mutation.
- Không nuốt lỗi bằng `catch {}`; log có context hoặc hiển thị trạng thái phù hợp.

Test:

- Connect, disconnect và reconnect.
- Cleanup subscription khi unmount.
- Duplicate `clientMessageId`.
- Message đến sai conversation.
- Seen event.
- Invalid JSON hoặc payload.
- Notification mark one/all và safe internal navigation.

Commit:

```text
refactor(communication-ui): unify rest realtime and notification state
```

## 8. Chặng 6 — Tách Admin khỏi implementation Finance

Cách sửa:

- Đặt wire DTO tài chính tại `features/finance/contracts`.
- Admin chỉ import contract công khai qua `features/finance/index.ts`.
- Chuyển formatter và status metadata về:

```text
src/shared/presentation/
├── dateTime.ts
├── money.ts
└── statusCatalog.ts
```

- Status module trả metadata `{ label, tone }`, không trả JSX.
- Admin table tự render bằng shared `StatusTag`.
- Tách modal hành động khỏi các table 250–300 dòng.
- Table nhận command callback và display data; page orchestration giữ mutation/toast.

Mục tiêu:

- `AdminPayoutTable.tsx`: dưới khoảng 170 dòng.
- `AdminRefundTable.tsx`: dưới khoảng 170 dòng.
- `AdminExtensionTable.tsx`: dưới khoảng 160 dòng.
- `StudentRequestsTable.tsx`: tách refund table và extension table riêng.

Không ép đạt số dòng bằng cách tạo các module chuyển tiếp nông; chỉ tách trách nhiệm có interface riêng và test surface riêng.

Commit:

```text
refactor(finance-ui): isolate admin contracts and presentation rules
```

## 9. Chặng 7 — UI primitives và form workflow

Chỉ trích xuất module khi deletion test chứng minh có lặp thật.

Ưu tiên:

- `AsyncActionModal`: loading, validation, submit và close.
- `PagedDataTable`: pagination, loading và empty state; domain vẫn sở hữu columns.
- `StatusTag`: render từ status metadata.
- `ApiFormError`: map `fieldErrors` sang React Hook Form hoặc Ant Form.
- Một `NotificationBell` và một workspace shell.

Không thực hiện:

- Không tạo generic form builder.
- Không bọc lại mọi Ant Design control.
- Không chia file chỉ vì vượt số dòng.
- Không tạo interface khi chỉ có một implementation và không có seam thật.

Commit:

```text
refactor(ui): consolidate repeated async presentation patterns
```

## 10. Chặng 8 — Architecture guardrails

Bật dần ESLint:

- `@typescript-eslint/no-unused-vars`: error.
- `@typescript-eslint/no-explicit-any`: cấm trước trong auth, transport, finance và communication.
- Không tắt toàn cục `@next/next/no-img-element`; chỉ thêm exception đúng file khi có lý do.
- Dùng `no-restricted-imports` để bảo đảm:
  - Page không import Axios.
  - Feature không import implementation nội bộ của feature khác.
  - Feature khác chỉ được import qua public `index.ts`.
  - Shared không import feature.
- Cấm khai báo thêm `ApiResponse` ngoài canonical contract.
- Cấm đọc token ngoài session module.
- Cấm dùng Axios ngoài Backend adapter hoặc feature transport đã duyệt.

Dùng ESLint hoặc script TypeScript nhỏ cho architecture checks; không dùng snapshot test chỉ để pipeline xanh.

Commit:

```text
test(frontend-architecture): enforce feature seams and client boundaries
```

## 11. Chặng 9 — Docker và E2E

Luna chuẩn bị các file:

```text
frontend/Dockerfile.test
frontend/.dockerignore
frontend/playwright.config.ts
frontend/e2e/
compose.frontend-test.yml
scripts/test-frontend-docker.ps1
```

`Dockerfile.test` dùng Node LTS và chạy:

```text
npm ci
npm run lint
npm run typecheck
npm run test:ci
npm run build
```

Thêm scripts vào `frontend/package.json`:

```json
{
  "typecheck": "tsc --noEmit",
  "test:ci": "jest --runInBand --detectOpenHandles",
  "test:e2e": "playwright test"
}
```

Chỉ xóa `--forceExit` sau khi timer, QueryClient và STOMP cleanup đúng.

E2E Docker bao phủ:

- Guest: homepage → môn học → danh sách gia sư → hồ sơ gia sư.
- Auth: login thường, Google redirect initiation và callback lỗi.
- Student: packages, booking, assignment, chat và notification.
- Teacher: profile, availability, package, booking, assignment và wallet.
- Admin: teacher approval, refund, payout và settings.
- Quyền chéo trả 403/404 và UI không hiển thị action sai quyền.
- Mobile viewport cho public/student.
- Desktop viewport cho teacher/admin.

Không tự động dùng Google consent thật, email người dùng thật hoặc PayOS thật. Test thay đổi dữ liệu chỉ dùng tài khoản và fixture test riêng.

Commit:

```text
test(frontend): add dockerized quality and role smoke gates
```

## 12. Lệnh Docker chạy thủ công ở gate cuối

Từ `D:\EdTech`:

```powershell
docker compose -f compose.frontend-test.yml config --quiet
docker compose -f compose.frontend-test.yml build frontend-check
docker compose -f compose.frontend-test.yml run --rm frontend-check
docker compose -f compose.frontend-test.yml up -d frontend
docker compose -f compose.frontend-test.yml run --rm frontend-e2e
docker compose -f compose.frontend-test.yml stop frontend
```

Không chạy:

```text
docker compose down -v
docker volume prune
docker system prune
```

## 13. Điều kiện hoàn thành

Chỉ chốt refactor khi:

- Không còn Axios trong page hoặc UI module.
- Chỉ còn một response contract và một session source.
- Không còn import implementation xuyên feature.
- Public pages không bị chặn bởi auth hydration.
- Refresh concurrency và STOMP reconnect có regression test.
- Jest không cần `--forceExit`.
- Lint, typecheck, unit test và build đều xanh.
- Docker quality gate xanh.
- E2E chạy qua Guest, Student, Teacher và Admin.
- REST route, STOMP destination và giao diện hiện tại không đổi ngoài lỗi được xác nhận.
- `.env.cloud`, test credential và secret vẫn nằm ngoài Git.

## 14. Thứ tự commit dự kiến

```text
1. test(frontend): characterize session transport and route behavior
2. refactor(frontend-api): centralize backend transport contracts
3. refactor(auth-ui): consolidate client session lifecycle
4. refactor(shell): consolidate workspace routing and navigation
5. refactor(bookings-ui): centralize queries mutations and cache effects
6. refactor(finance-ui): centralize queries mutations and cache effects
7. refactor(learning-ui): centralize assignment data flow
8. refactor(marketplace-ui): centralize public search data flow
9. refactor(communication-ui): unify rest realtime and notification state
10. refactor(finance-ui): isolate admin contracts and presentation rules
11. refactor(ui): consolidate repeated async presentation patterns
12. test(frontend-architecture): enforce feature seams and client boundaries
13. test(frontend): add dockerized quality and role smoke gates
14. docs(frontend): document module seams and test workflow
```
