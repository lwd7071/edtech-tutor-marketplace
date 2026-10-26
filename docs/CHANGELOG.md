# Changelog theo đợt hoàn thành

## 2026-10-26 — Dùng container healthcheck cho smoke CI
- Backend container đã healthy trong Compose; probe host `curl localhost:8080` fail vì service không publish cổng ra host. Smoke giờ chờ trạng thái `healthy`, vốn dùng `/actuator/health` bên trong container.
- CI sau cập nhật đang chờ; migration diff trống, không kết nối Supabase.

## 2026-10-26 — Sửa probe smoke test Compose
- Maven/Testcontainers GitHub CI PASS trên `main` và `dev`; bước smoke sau đó fail do workflow inspect tên container cũ. Probe hiện lấy ID từ service `backend` bằng `docker compose ps`, phù hợp với tên container Compose tạo tự động.
- CI sau cập nhật chưa chạy; migration diff trống, không kết nối Supabase.

## 2026-10-26 — Cô lập kiểm thử Flyway lịch sử
- Các test V17–V23 dùng database Testcontainers riêng theo class và dừng ở đúng migration đích; tránh V32 chạy nhầm trên schema lịch sử và tránh extension làm bẩn `public` của integration tests. Không sửa migration đã áp dụng.
- Maven test-compile PASS. Full Testcontainers suite và GitHub CI sau thay đổi đang chờ; không ghi nhận là pass. Migration diff trống; không kết nối Supabase.

## 2026-10-26 — Sửa cấu hình CI đang làm main đỏ
- Backend CI cấp giá trị giả chỉ trong job để Compose xác thực được cấu hình. Frontend CI build bằng public API fixture, khởi fixture cho Playwright, dùng timezone Việt Nam và sửa retry/selector/keyboard assertions theo UI và fixture hiện tại.
- Typecheck PASS; Jest 122 suites/329 tests PASS; ESLint các file E2E/config thay đổi PASS; production build PASS. E2E full 196/197 (một lỗi ghi PNG thoáng qua trên Windows), test đó PASS khi chạy riêng 5/5. Maven local compile PASS nhưng Testcontainers bị chặn do máy không có Docker environment; chờ GitHub CI. Full lint bị ảnh hưởng bởi các thư mục `.next-*` cũ không thuộc repo sạch. Migration diff trống; không kết nối Supabase.

## 2026-10-24 — Cập nhật chi phí dự phóng hồ sơ Tutor Match
- Hạ giả định chi phí vận hành cố định còn 2 triệu đồng/tháng và đồng bộ bảng quý, điểm hòa vốn (~34 gói/tháng), kết quả năm đầu (+22,8 triệu đồng), thiếu hụt tiền mặt cao nhất (2,4 triệu đồng) cùng vốn lưu động tham khảo (8,4 triệu đồng). Tất cả vẫn là giả định, chưa tính lương nhà sáng lập.
- DOCX vẫn 7 trang A4, Times New Roman 14 pt; OfficeCLI validate PASS và ảnh preview các trang đã rà. Migration diff trống; không kết nối Supabase.

## 2026-10-24 — Hoàn thiện định dạng hồ sơ pitching Tutor Match
- Dàn lại bản DOCX thành 7 trang A4 theo nhận diện Tutor Match; giữ Times New Roman 14 pt và toàn bộ nội dung người dùng đã cập nhật, bỏ nhãn trang mẫu và không đánh số trang. Bản hoàn thiện được lưu thành file “Ban_Trinh_Bay” cạnh file nguồn vì tài liệu nguồn đang mở/khóa trong Word.
- OfficeCLI validate PASS; ảnh preview HTML 7/7 trang đã rà trực quan; migration diff trống, không kết nối Supabase.

## 2026-10-24 — Hồ sơ Tutor Match dự thi UTE SV-STARTUP 2026
- Tạo hồ sơ thuyết minh Word 8 trang theo mẫu, bảo toàn mẫu gốc; đã render và kiểm tra trực quan từng trang. Nội dung sản phẩm, mức hoa hồng và tình trạng kiểm chứng được đối chiếu với repo; tài chính ghi rõ là kịch bản giả định, không tuyên bố có traction hoặc đối tác.

## 2026-10-16 — Kiểm toán bằng chứng redesign 70 route

- Đối chiếu route source với inventory/tracker: 70 route sản phẩm và một route sandbox; tracker có đủ 70/70 route PASS, mọi ảnh PNG được dẫn từ tracker đều tồn tại. Sửa liên kết evidence before/after của alias `/admin/dashboard` về UI owner `/admin`.
- Jest toàn frontend 122/122 suites, 329/329 tests PASS; typecheck, lint (bỏ qua output `.next*`), docs checker và production build 64 trang PASS. Build dùng distDir riêng do `.next` mặc định báo EBUSY. Playwright rerun audit logs sau thay đổi copy bị chặn bởi quyền socket localhost; E2E 4/4 trước thay đổi và Jest hiện tại xác nhận deep-link/API.

## 2026-10-16 — Viết lại `/admin/audit-logs` (Admin 70/70)

- Dựng lại audit history thành hàng responsive, target context deep-link và modal JSON chỉ đọc; giữ GET params/pagination và các trường audit hiện có, không thêm filter UI.
- AST layout page **5/6 (83.3%)**, table **12/19 (63.2%)**. Ảnh fixture before/after 375/768/1280 đã xem; Playwright **4/4**, Admin finance components Jest **1 suite / 5 tests**, TypeScript, ESLint mục tiêu và docs checker PASS. Sau đó xác minh toàn frontend: Jest **122/122 suites, 329/329 tests PASS**, typecheck PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/admin/settings`

- Dựng lại trang cấu hình thành heading, hiệu lực/thời điểm cập nhật và biểu mẫu bốn tham số; giữ API GET/PUT, commission conversion và validation/range hiện hành.
- AST layout page **4/5 (80.0%)**, form **16/18 (88.9%)**. Ảnh fixture before/after 375/768/1280 đã xem; Playwright **5/5**, Admin finance components Jest **1 suite / 5 tests**, TypeScript, ESLint mục tiêu và docs checker PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/admin/booking-settlements`

- Dựng lại queue và panel chi tiết để phân biệt buổi học với quyết toán, đọc xác nhận/deadline và dispute trước khi thực hiện hành động sẵn có; giữ `availableSettlementActions`, reason gate, `{version,note}` và xử lý concurrent update.
- AST layout **30/43 (69.8%)**. Ảnh fixture before/after 375/768/1280 đã xem; Playwright **5/5**, page + settlement model Jest **2 suites / 5 tests**, TypeScript, ESLint mục tiêu và docs checker PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/admin/payouts`

- Dựng lại payout queue thành các hàng theo trạng thái, detail context và action cho process/complete/reject; giữ DTO/API contract, version, proof upload và idempotency. Form hoàn tất gửi đúng metadata contract gồm bank reference, transferredAt và version.
- AST layout page **4/5 (80.0%)**, queue **26/42 (61.9%)**. Ảnh fixture before/after 375/768/1280 đã xem; Playwright **6/6**, Admin finance Jest **1 suite / 3 tests**, TypeScript, ESLint mục tiêu và docs checker PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/admin/extensions`

- Dựng lại queue gia hạn thành hàng dữ liệu, chi tiết yêu cầu và form quyết định riêng; giữ GET filters/pagination, approve/reject API payload và idempotency.
- AST layout page **4/5 (80.0%)**, queue **17/24 (70.8%)**. Ảnh fixture before/after 375/768/1280 đã xem; Playwright **5/5**, Admin finance Jest **1 suite / 3 tests**, TypeScript, ESLint mục tiêu và docs checker PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/admin/refunds`

- Dựng lại queue thành danh sách theo hàng, detail context và form quyết định theo trạng thái; giữ API/payload/version/idempotency và thêm trường thời điểm chuyển khoản bắt buộc đã có trong contract.
- AST layout page **4/5 (80.0%)**, queue **24/39 (61.5%)**. Ảnh dev fixture before/after 375/768/1280 đã xem; Playwright **6/6**, Admin finance Jest **1 suite / 3 tests**, TypeScript và ESLint mục tiêu PASS. `scripts/check-docs.ps1` PASS (36 Markdown files). Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/admin/users`

- Dựng lại directory học viên/gia sư dạng hàng; giữ tìm kiếm, role/status filters, sorting, phân trang, audit deep-link và modal khóa/mở khóa theo đúng GET/PATCH contract.
- Ảnh browser fixture trước/sau 375/768/1280 và modal mobile đã xem. Playwright dev **5/5**, Jest **2 suites / 3 tests**, typecheck, targeted ESLint PASS. Chưa chạy production build trong lượt này. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/admin/credentials` và `/admin/subjects`

- Dựng lại hai page Admin thành queue theo trạng thái và danh sách đề xuất môn học; giữ API, role guard, version, payload, validation và luồng quyết định hiện có. Đặc tả môn học ghi rõ UI hiện chỉ tạo môn mới vì chưa có catalog lookup/select trong page.
- Ảnh browser fixture trước/sau 375/768/1280 đã xem. Playwright dev **9/9**, Jest **2 suites / 3 tests**, typecheck, ESLint mục tiêu và docs checker PASS. Chưa chạy production build trong lượt này. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Chụp baseline Admin dashboard

- Lưu source UI và ảnh production `/admin` tại 375/768/1280 bằng `AdminDashboardView` DTO fixture. Baseline Playwright **1/1 PASS**. Xem được mobile bottom navigation phủ nội dung khi full-page và wrap số tiền ở tablet. Reference X chưa duyệt; không sửa UI Admin.

## 2026-10-16 — Hoàn thiện đặc tả Admin theo source

- Mở rộng đặc tả riêng cho 12 route Admin bằng owner/API/DTO, trạng thái, tương tác và responsive từ code hiện tại; sửa đặc tả audit để chỉ bao gồm target deep-link và pagination đang có. Reference X vẫn chờ duyệt nên chưa bắt đầu code Admin.

## 2026-10-16 — Viết lại `/teacher/payouts`

- Tách route rút tiền khỏi màn ví dùng nhầm; dựng hierarchy số dư khả dụng, điều kiện và lịch sử payout riêng. Sửa query invalidation để lịch sử có pagination được tải lại sau khi tạo lệnh; giữ API/payload, wallet version, validation và idempotency.
- AST JSX **10/15 (66.7%)**, full-file comparison **84 xóa / 185 thêm**; screenshots production before/after 375/768/1280 đã chụp và xem. Playwright payout **3/3** + wallet/bank regression **7/7**, finance Jest **6 suites / 24 tests**, ESLint mục tiêu, Next build (64 routes, TypeScript) và docs checker PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/bank-accounts`

- Tách route ngân hàng khỏi UI ví đang được dùng nhầm; tạo trang nhận tiền riêng và giữ nguyên approval guard, mask, validation, create/update/delete, version và idempotency contracts.
- JSX layout AST **11/15 (73.3%)**, full-file comparison **47 xóa / 210 thêm**; production screenshots before/after 375/768/1280 đã chụp và xem. Playwright production **3/3** + wallet regression **4/4**, finance Jest **6 suites / 23 tests**, typecheck, ESLint mục tiêu, Next build (64 routes) và docs checker PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/trial-requests`

- Dựng lại yêu cầu học thử thành danh sách phẳng, nêu rõ mã học viên/môn vì DTO hiện chỉ có ID; giữ GET/pagination, accept/reject, giờ Việt Nam, validation, version và optimistic conflict.
- JSX layout AST **29/33 (87.9%)**, full-file diff **11 xóa / 247 thêm**; ảnh trước/sau 375/768/1280 đã xem. E2E production **3/3**, production build webpack (64 route), typecheck, ESLint mục tiêu và docs checker PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/documents`

- Tái cấu trúc trang xác minh thành hướng dẫn riêng tư, danh sách trạng thái dạng hàng và khu vực tải tài liệu. Giữ GET/POST/DELETE, multipart contract, callback upload, xác nhận xóa và URL lưu trữ riêng tư; thêm thao tác retry danh sách khi GET lỗi.
- AST layout lần lượt **7/9 (77.8%)**, **15/16 (93.8%)**, **5/5 (100%)**; production screenshots before/after 375/768/1280; Playwright **5/5**, Jest **3/3**, TypeScript, targeted ESLint và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/profile`

- Tái cấu trúc trang hồ sơ thành status xét duyệt, form thông tin giảng dạy và form nơi ở độc lập. Giữ nguyên field, validation, API/payload, quyền lưu hồ sơ và yêu cầu gửi duyệt; sửa nơi ở vẫn qua endpoint riêng kể cả khi profile đã APPROVED.
- AST layout **21/34 (61.8%)**, production screenshots before/after 375/768/1280; Playwright **10/10**, Jest **1/1**, TypeScript, targeted ESLint và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Duyệt reference Teacher và viết lại `/teacher`

- Người dùng duyệt reference T; dashboard Teacher được dựng lại theo thứ bậc hồ sơ, việc tiếp theo, lịch/trial, bài nộp và ví. API/auth/role guard, query parameters, dữ liệu và destination giữ nguyên.
- AST layout **42/70 (60.0%)**, ảnh production before/after 375/768/1280; Playwright **8/8**, typecheck, targeted ESLint và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại UI báo cáo buổi học Student

- Dựng lại `/student/session-reports` thành danh sách báo cáo theo thời gian, nội dung và nhận xét tách bậc, việc cần làm tiếp có điều kiện và các link chức năng rõ nhãn. Giữ nguyên endpoint, pagination, ownership và giới hạn URL bản ghi `http(s)`.
- AST layout 12/19 (63.2%); before/after screenshots 375/768/1280; Playwright 1/1, typecheck, ESLint mục tiêu và Next production build (64 static pages) PASS. Ảnh cuối đã chụp và xem từ app production + fixture cục bộ. Không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI hồ sơ Student

- Tái cấu trúc `/student/profile` thành account summary read-only và form liên hệ phụ huynh theo nhóm trường responsive. Giữ nguyên API, handler, validation, trimming/null normalization, email opt-in và auth identity.
- AST layout 29/39 (74.4%); before/after screenshots 375/768/1280; Playwright 1/1, typecheck, ESLint mục tiêu và Next production build (64 static pages) PASS. Không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI theo dõi yêu cầu Student

- Dựng lại trang `/student/requests` thành ba tab Học thử/Hoàn tiền/Gia hạn với flat rows, text status và error→retry; bổ sung đặc tả cụ thể theo data contract hiện có. Đây là trang lịch sử chỉ đọc, không chuyển form tạo refund/extension khỏi trang chi tiết gói.
- AST layout 22/27 (81.5%); production screenshot before/after 375/768/1280; Playwright 1/1, TypeScript, ESLint mục tiêu và Next build (64 static pages) PASS. Không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI thông báo Student

- Dựng lại list notification thành các hàng phẳng, thêm nhãn unread/read, thời gian, empty/error states rõ và dải category tab cuộn ngang trên mobile. Giữ filters/API, mutation handlers, role-aware internal reference routing và dashboard invalidation.
- AST layout R/N 21/30 (70%); ảnh app before/after 375/768/1280. Playwright 2/2 (Student + Teacher shared route), no route-specific Jest suite; TypeScript, ESLint mục tiêu và production build 64 static pages PASS. Không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI tin nhắn Student

- Dựng lại inbox và thread theo layout hai pane desktop / một pane mobile; thêm initial avatar fallback, unread label, trạng thái kết nối, history scroll riêng và composer chữ rõ. Giữ route query, REST pagination, STOMP, optimistic/retry, MESSAGE attachment và role/ownership contract.
- AST layout R/N: ChatLayout 17/22 (77.3%), ChatThread 24/37 (64.9%); ChatPage thin wrapper không tính. Playwright 2/2, Jest 2 suites / 5 tests, typecheck, targeted ESLint, production build 64 static pages PASS. Ảnh before/after 375/768/1280 trong `docs/redesign/evidence/`; Teacher shared route smoke PASS. Không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI chi tiết bài tập Student

- Dựng lại `/student/assignments/[id]` thành hierarchy đề, tệp, bài đã lưu/nộp/đã chấm và editor. Giữ query/API, upload SUBMISSION, DRAFT/SUBMITTED payload, version, attachments, conflict refetch, dashboard invalidation và eligibility hiện có.
- AST layout R/N 17/28 (60.7%); ảnh Edge before/after cùng fixture 375/768/1280. Playwright 1/1, Jest 1 suite / 2 tests, typecheck, ESLint mục tiêu và production build (64 static pages) PASS. Tổng 28/70; không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI danh sách bài tập Student

- Dựng lại `/student/assignments` thành bộ lọc tiến độ, tìm tiêu đề và danh sách bài theo hàng responsive. Giữ nguyên API progress/page/size, pagination, search handler và link detail; chỉ render trường list DTO hiện có, không bịa tên môn/gia sư.
- AST layout R/N 14/15 (93.3%); ảnh Edge before/after cùng fixture 375/768/1280. Playwright 1/1; không có Jest suite riêng; typecheck, ESLint mục tiêu và production build (64 static pages) PASS. Tổng 27/70; không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI chi tiết booking Student

- Dựng lại `/student/bookings/[id]` theo reference S thành vùng thông tin lịch, trạng thái, quyết toán, hành động và báo cáo. Giữ API queries, version payload, role/eligibility, handlers và các modal hiện có; tách presentation Teacher để giữ riêng UI và chức năng Teacher.
- Ảnh browser before/after cùng fixture 375/768/1280. Playwright 2/2, Jest 3 suites / 6 tests, typecheck, ESLint mục tiêu và production build 64 static pages PASS; fixture không gọi backend thật. Tổng 26/70; không có migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI Student bookings

- Dựng lại `/student/bookings` thành heading/filter và danh sách buổi học dạng hàng có thể quét; giữ data/API/query, filter status/date, pagination, Vietnam timezone, status/settlement và detail links. Tách Teacher markup ra component riêng để giữ nguyên UI Teacher trong lúc reference T chưa duyệt.
- AST layout 15/22 (68.2%); full-file diff so baseline 89 deleted / 32 added. Browser screenshots before/after 375/768/1280; Playwright 1/1, Jest 2 suites / 6 tests, typecheck, targeted ESLint và production build (64 static pages) PASS. E2E xác nhận filter/reset/page, retry/empty/loading, focus, bottom-nav clearance và Teacher regression. Tổng 25/70, không migration mới, không Supabase.

## 2026-10-16 — Viết lại UI Student payment callback

- Dựng lại `/student/payments/callback` thành một luồng trạng thái server, nội dung giải thích, retry và link tới gói học. Giữ `invoiceId`, GET invoice, polling 3 giây và cách kết thúc poll theo status; query string không xác nhận thanh toán và page không tự redirect.
- AST JSX layout subtree 7/9 (77.8%); ảnh Edge before/after 375/768/1280 cùng fixture. Playwright 1/1, typecheck, targeted ESLint và Next build 64 static pages PASS; route không có Jest suite riêng. Tổng 24/70; không có migration, không kết nối Supabase.

## 2026-10-16 — Viết lại UI Student payment result

- Dựng lại `/student/payment-result/[invoiceId]` thành status heading, giải thích, details dạng definition list và CTA theo invoice response. PAID/EXPIRED/PENDING/CANCELLED giữ đúng nhánh nguồn; query string không thể giả paid. Giữ GET/detail DTO, không polling, Student guard và href hiện hành; error request có retry qua 
efetch`.
- AST layout subtree `PaymentResultPage` 6/8 (75%), `PaymentResultView` 18/20 (90%); ảnh before/after Edge 375/768/1280 cùng fixture. Playwright 1/1; Jest 3 suites / 6 tests; typecheck, targeted ESLint và Next build 64 static pages PASS. Tổng 23/70 route. Không migration mới, không kết nối Supabase.

## 2026-10-16 — Viết lại UI Student checkout

- Dựng lại `/student/checkout/[invoiceId]` thành hierarchy invoice snapshot, giá trị VND, hướng dẫn trạng thái, hạn, vùng PayOS/QR và link về gói. Terminal state không render QR/action thanh toán. Giữ nguyên query/API contract, Student guard, interval polling 3 giây và quy tắc chỉ tin status PAID từ server.
- AST JSX layout subtree: `CheckoutPage` 6/8 (75%), `CheckoutQRView` 8/12 (66.7%). Ảnh Edge before/after với cùng fixture ở 375/768/1280; Playwright 1/1, Jest 2 suites / 4 tests, typecheck, targeted ESLint và Next production build (64 static pages) PASS. Không có migration và không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI chi tiết gói Student

- Dựng lại `/student/packages/[id]`: status/action theo nghiệp vụ, tiến độ, phân bổ lượt và điều khoản snapshot thành các section phẳng; giữ API, Student ownership guard, mutation handlers, status eligibility, money calculation, redirect và modal validation. SessionCounter bỏ các sub-card màu thành lưới số liệu; detail có loading/error retry an toàn.
- Browser test phát hiện form refund đã nhận `packageVersion` nhưng không đăng ký field trong Ant Form; thêm field ẩn InputNumber để gửi optimistic version đúng contract API. Không đổi endpoint/request shape đã định nghĩa/backend.
- AST subtree page 6/10 (60%), view 49/58 (84.5%), SessionCounter 4/5 (80%); ảnh Edge before/after 375/768/1280 trong `docs/redesign/evidence/`. Playwright 1/1; Jest 6 suites / 14 tests; typecheck, ESLint mục tiêu, production build 64 static pages PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI danh sách gói Student

- Dựng lại `/student/packages` thành danh sách hàng có thể so sánh thay cho lưới card. Giữ `GET /api/student/packages`, filter/tab hiện có, page/size/sort, DTO, href chi tiết và Student guard; thêm retry query khi lỗi, empty state theo filter và vùng số buổi semantic. Reference S đã duyệt.
- AST JSX layout subtree R/N: `StudentPackagesPage` 4/5 (80%), `StudentPackageList` 7/10 (70%), `StudentPackageCard` 14/16 (87.5%). Ảnh Edge before/after cùng DTO fixture 375/768/1280 đã chụp và xem. Playwright 1/1; Jest 5 suites / 12 tests; TypeScript, targeted ESLint và production build 64 static pages PASS. Không có migration; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI tổng quan Student

- Dựng lại `/student` thành task-first dashboard: lịch/hành động tiếp theo, nhóm bốn chỉ số dạng hàng và shortcut báo cáo/tin nhắn. Giữ nguyên dashboard API/DTO, timezone, handler retry, route và role guard. Sửa shared workspace wordmark thành `tutormatch` chữ thường, không trang trí.
- JSX layout subtree R/N 9/15 (60%); baseline và Edge screenshot before/after 375/768/1280 dùng cùng DTO fixture. Playwright Student 1/1, Jest 2 suites / 2 tests, Teacher/Admin shell smoke 3/3, TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Tổng 19/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Làm rõ đặc tả Student dashboard từ source

- Đối chiếu `/student` với page, hook, DTO, API contract, role guard và workspace shell; bổ sung hierarchy, link thật, năm field read model, null/zero/error/loading, responsive và tiêu chí test vào `PAGE_DESIGN_PLAN.md`. Xác nhận inventory/spec/tracker khớp 70 route sản phẩm; `/sandbox/components` nằm ngoài phạm vi. Reference S vẫn chờ duyệt, chưa sửa UI và chưa chạy UI test.

## 2026-10-16 — Hoàn tất UI OAuth callback

- Dựng lại `/oauth2/callback` thành transition surface với trạng thái xử lý và lỗi có recovery rõ; giữ nguyên provider/query branches, exchange API contract, session establishment, cache clear và redirect theo role server trả về.
- JSX layout subtree R/N 4/6 (66.7%); baseline và ảnh Edge before/after 375/768/1280 trong `docs/redesign/evidence/`. Playwright 1/1, Jest 1 suite / 1 test, TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Tổng 18/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI chọn role OAuth

- Dựng lại `/auth/oauth/role` theo reference A; giữ cảnh báo role bất biến, Student/Teacher, registration token, API payload, establish và redirect từ role server trả về. Không thêm Admin hoặc login action.
- JSX layout subtree R/N 11/13 (84.6%); baseline và ảnh app before/after 375/768/1280 trong `docs/redesign/evidence/`. Jest 1 suite / 3 tests, Playwright 1/1 (responsive, warning, pending/success, role route, missing token), TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Tổng 17/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI đặt lại mật khẩu

- Dựng lại `/auth/reset-password` với hierarchy rõ cho policy mật khẩu, xác nhận, submit và token recovery; giữ nguyên schema, endpoint, token query, payload, status mapping và success redirect.
- JSX layout subtree R/N 17/24 (70.8%); baseline và ảnh app before/after 375/768/1280 trong `docs/redesign/evidence/`. Jest 1 suite / 4 tests, Playwright 1/1 (validation, pending, success, missing/expired token, API error recovery), TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Tổng 16/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI quên mật khẩu

- Dựng lại `/auth/forgot-password` theo reference A thành form gọn, phân biệt validation/API error/loading/success; giữ nguyên endpoint, email payload, handler và login recovery. Success copy vẫn không cho biết email có tồn tại.
- JSX layout subtree R/N 16/20 (80%); baseline và ảnh app before/after 375/768/1280 trong `docs/redesign/evidence/`. Jest 1 suite / 3 tests, Playwright 1/1 (validation, pending, error/recovery, privacy-safe success), TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Tổng 15/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI xác minh email

- Dựng lại `/auth/verify-email` theo reference A với trạng thái chờ/resend rõ ràng, hiển thị email query hiện có, xác minh token pending/success/error. Giữ nguyên API/handlers, missing-email alert và điều hướng đăng nhập; resend API failure vẫn không có feedback theo source.
- JSX layout subtree R/N 17/23 (73.9%); baseline và ảnh app before/after 375/768/1280 trong `docs/redesign/evidence/`. Jest 1 suite / 3 tests, Playwright 1/1 cho responsive, resend pending/success, missing email và token success/error; TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Tổng 14/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI đăng ký

- Dựng lại `/auth/register` theo reference A; role options xếp hai cột ở desktop, một cột ở tablet/mobile. Giữ nguyên bốn field API, role query, Google OAuth, links pháp lý và redirect verify-email. Thêm `agreeTerms: false` làm trạng thái ban đầu để hiển thị thông báo validation có sẵn; điều kiện đồng ý không đổi, không thêm parent contact fields.
- JSX layout subtree R/N 30/43 (69.8%); baseline và ảnh app before/after 375/768/1280 trong `docs/redesign/evidence/`. Jest 1 suite / 4 tests, Playwright 1/1 với role query, validation, pending/error fixture, responsive; TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Tổng 13/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI đăng nhập

- Dựng lại `/auth/login` theo reference A với hierarchy form mới; giữ nguyên API, payload, remember, session, redirect theo role, Google OAuth, locked account và support handler. Chuẩn hóa wordmark shared auth shell thành `tutormatch`.
- JSX layout subtree R/N 20/31 (64.5%); baseline và ảnh app before/after 375/768/1280 trong `docs/redesign/evidence/`. Cập nhật page spec/tracker. Jest 1 suite / 3 tests, Playwright responsive + validation/focus và 5 auth shell routes, TypeScript, targeted ESLint và production Next build (64 static pages) PASS. Build hoàn tất ngoài sandbox sau lỗi Windows EPERM khi đóng gói trong sandbox. Tổng 12/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI truy cập bị từ chối

- Người dùng duyệt reference A cho Auth; đã cập nhật trạng thái duyệt trong 
eferences/README.md` và tracker. Dựng lại `/forbidden` thành trạng thái 403 một cột, giữ nguyên ErrorState flow, 
outer.back()` và role guard; Student bị chuyển khỏi `/teacher` về `/forbidden`.
- `forbidden/page.tsx` AST R/N 2/2 (100%); shared `ErrorState.tsx` 5/8 (62.5%). Ảnh app before/after 375/768/1280 trong `docs/redesign/evidence/`. Playwright 4/4 (route + auth/role scenarios), Jest 4 suites / 20 tests, TypeScript, ESLint mục tiêu, production Webpack build (64 static pages) và docs check PASS. Tổng 11/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI chính sách riêng tư

- Tái cấu trúc `/privacy` thành ba phần đọc theo dữ liệu tài khoản, liên hệ phụ huynh và dữ liệu nhạy cảm; giữ nguyên từng câu chính sách, thêm liên kết liên quan tới `/terms`. AST subtree R/N 7/8 (87.5%); ảnh app before/after 375/768/1280 ở `docs/redesign/evidence/`.
- Playwright 1/1, TypeScript, targeted ESLint, production Webpack build (64 static pages) và `scripts/check-docs.ps1` sau cập nhật: PASS. Tổng 10/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI điều khoản sử dụng

- Dựng lại `/terms` thành cột đọc có giới hạn chiều rộng, giữ nguyên ba đoạn pháp lý và thêm liên kết liên quan tới `/privacy` theo đặc tả. AST subtree R/N 5/8 (62.5%); ảnh app before/after 375/768/1280 ở `docs/redesign/evidence/`.
- Playwright 1/1, TypeScript, targeted ESLint, production Webpack build (64 static pages) và `scripts/check-docs.ps1` sau cập nhật: PASS. Tổng 9/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI trang hỗ trợ

- Viết lại `/support` thành phần giới thiệu và danh sách tác vụ hỗ trợ dạng hàng; giữ nguyên ba đường dẫn tài khoản hiện có và hướng dẫn dùng mã giao dịch/yêu cầu. AST subtree R/N 12/14 (85.7%); ảnh app before/after 375/768/1280 ở `docs/redesign/evidence/`.
- Playwright 1/1, TypeScript, targeted ESLint, production Webpack build (64 static pages) và `scripts/check-docs.ps1` sau cập nhật: PASS. Tổng 8/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI trang dành cho gia sư

- Viết lại `/become-a-tutor` thành quy trình onboarding theo hàng bước, giữ nguyên copy, đăng ký với role `TEACHER` và đường tiếp tục hồ sơ. AST subtree R/N 8/13 (61.5%); ảnh app before/after 375/768/1280 ở `docs/redesign/evidence/`.
- Playwright 1/1, TypeScript, targeted ESLint, production Webpack build (64 static pages) và `scripts/check-docs.ps1` sau cập nhật: PASS. Tổng 7/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI hướng dẫn bắt đầu

- Viết lại `/how-it-works` thành ba hàng bước có thứ tự và CTA theo Public P; giữ nguyên copy và đích `/teachers`, `/become-a-tutor`. AST subtree R/N 8/13 (61.5%); ảnh app before/after 375/768/1280 lưu trong `docs/redesign/evidence/`.
- Playwright 1/1, TypeScript, targeted ESLint, production Webpack build (64 static pages) và `scripts/check-docs.ps1` chạy sau cập nhật tài liệu: PASS. Tổng 6/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI bảng xếp hạng

- Dựng lại `/ranking` thành bộ lọc môn và danh sách hàng thứ hạng, giữ nội dung/số liệu API và link hồ sơ; không dùng podium trang trí hay thêm control phân trang ngoài route hiện có.
- Giữ API endpoint/query/URL. Khắc phục việc dữ liệu toàn cục từ `initialData` chặn request sau khi đổi môn; test xác nhận filter Toán dùng đúng `subjectId`, 7/13 kết quả fixture.
- Ảnh app before/after 375/768/1280 và baseline source/CSS lưu trong `docs/redesign/evidence/`. AST subtree UI owner 10/12 và 12/13. Playwright 1/1, Jest 3 suites / 6 tests, typecheck, ESLint, production Webpack build và `scripts/check-docs.ps1` (36 Markdown files) PASS. Tracker: 5/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Hoàn tất UI hồ sơ gia sư

- Hoàn thiện cấu trúc header, nội dung hồ sơ, gói học, lịch rảnh, đánh giá và CTA theo reference Public P; giữ nguyên API, props, query/pagination, auth, quyền Student mua gói, payment flow và nghiệp vụ hiện hành.
- Lưu baseline source/CSS và ảnh app before/after tại 375/768/1280 trong `docs/redesign/evidence/`; các UI owner đạt ngưỡng AST 60%+.
- Playwright Chromium 1/1 PASS trên fixture cục bộ: responsive/no overflow, điều hướng mục, focus bàn phím và Guest return URL. Jest 3 suites / 9 tests, typecheck, ESLint mục tiêu, production Webpack build và `scripts/check-docs.ps1` PASS. `/teachers/[id]` được nghiệm thu; tổng 4/70 route PASS. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Bắt đầu viết lại hồ sơ gia sư

- Đã thay cấu trúc profile header, phần thông tin/gói học/lịch/đánh giá và vùng CTA cho `/teachers/[id]`; cập nhật package list sang các hàng điều khoản dễ so sánh. Giữ API calls, props, query pagination, Guest login return URL, quyền Student mua gói, payment callback và các handler hiện có.
- AST layout subtree R/N: `TeacherDetailClient` 28/44, `TeacherProfileHeader` 14/15, `TeacherPackagesTab` 11/17; Jest 4 suites / 10 tests, typecheck và targeted ESLint pass.
- Chưa nghiệm thu: ảnh before/after và browser E2E bị chặn do môi trường không cho kết nối socket local; hai lần production build dừng ở Windows `EPERM` dưới `.next`. Route giữ IN_PROGRESS, chưa chuyển sang `/ranking`. Không có migration mới; không kết nối Supabase.

## 2026-10-16 — Nghiệm thu UI tìm gia sư

- Tái cấu trúc JSX của `TeacherSearchClient`, `TeacherSearchResults`, `TeacherSortBar` và filter thành search/filter riêng, danh sách directory và preview theo ngữ cảnh; dùng hàng phân cách thay card lặp. Giữ API/page wrapper, props, query filters, sort, pagination, retry và liên kết hồ sơ.
- Phục hồi/lưu baseline và ảnh before/after ở 375/768/1280. AST subtree R/N lần lượt 12/20, 44/66, 4/5; cả ba UI owner đạt ngưỡng 60%. Jest 3 suites / 7 tests và ESLint mục tiêu pass; Playwright production 1/1 pass trước đó xác nhận search/filter/sort/preview/drawer và responsive. Bổ sung test pagination giữ query.
- Build fixture-local lần kiểm tra mới nhất bị Windows `EPERM` khi Next tạo `.next`; E2E thử lại không truy cập được localhost từ Chrome (`ERR_NETWORK_ACCESS_DENIED`). Ghi rõ đây là lần thử bị chặn; không thay thế kết quả production build/E2E đã pass trước đó. Không có migration mới; không kết nối Supabase.

## 2026-10-15 — Hoàn tất viết lại giao diện `/subjects`

- Đổi layout chọn môn sang heading riêng, thanh tìm kiếm/cấp học, danh sách radio theo lưới desktop và một cột mobile, cùng vùng tóm tắt/tiếp tục nằm sau danh sách. Giữ nguyên wrapper/API, props, query parameters, handler và URL tới `/teachers?subjectId=...`; bỏ icon sách lặp và mũi tên trang trí.
- Thêm route loading skeleton trung tính và trạng thái đang lọc để không nhầm placeholder với dữ liệu môn. Lưu baseline và ảnh before/after tại 375/768/1280 trong `docs/redesign/evidence/`. Jest 2 suites / 9 tests, ESLint mục tiêu, TypeScript, production build và Playwright production 1/1 đã pass với fixture DTO cục bộ; E2E xác nhận search/filter, keyboard radio, CTA và không tràn ngang.
- Full-file diff UI là 86/165 dòng (52%); trong 94 dòng JSX/layout ban đầu, 81 dòng thay/xóa (86%). Đã chốt checklist đo 60% trên phần JSX/layout render của UI owner, loại trừ phần props/API/handler cần giữ; route `/subjects` đạt PASS theo layout R/N. Không có migration mới; không kết nối Supabase.

## 2026-10-15 — Bắt đầu viết lại UI trang chủ theo reference P

- Dựng lại cây JSX/layout route `/` theo reference P đã duyệt; giữ API calls/arguments/data, copy, điều hướng và recovery. Bản baseline JSX 100 dòng được lưu để đối chiếu; 60 dòng gốc bị thay/xóa (60/100) theo `git diff --no-index --numstat`. Chuẩn hóa wordmark thành `tutormatch`; bỏ mũi tên trang trí khỏi link dùng chung và card gia sư.
- Jest liên quan 8 suites / 19 tests, production build (gồm TypeScript) và ESLint mục tiêu PASS. Playwright production 2/2 PASS, gồm CTA, focus bàn phím, không tràn ngang tại 375/768/1280 và shell trên 8 route Public. Chụp before/after populated bằng cùng fixture DTO cục bộ; browser error state cũng đã được xem. Route `/` đạt PASS; các route tiếp theo chưa triển khai.

## 2026-10-15 — Gom tài liệu redesign

- Chuyển `design/`, `implementation/` và 
eferences/` vào `docs/redesign/`; cập nhật đường dẫn tuyệt đối trong tài liệu và giữ nguyên các reference.
- Dọn một concept proposal cũ cùng bảy ảnh concept/đối chiếu khỏi `docs/redesign/design/`; giữ lại năm tài liệu đặc tả hiện hành và reference mới trong `docs/redesign/references/`.
- `scripts/check-docs.ps1` pass (36 Markdown files); `git diff --check` sạch. Không có thay đổi code hay migration.

## 2026-10-15 — Đặc tả lại UI theo từng route

- Tạo hệ thiết kế, kiểm kê 70 route sản phẩm, đặc tả layout/trạng thái từng route, component và responsive rules, implementation tracker 70 dòng, checklist nghiệm thu và reference index. Tách `/sandbox/components` là route nội bộ.
- Tạo năm visual proposal Public/Auth/Student/Teacher/Admin để duyệt; ảnh nằm trong `docs/redesign/references/`, không phải screenshot app. Giữ wordmark `tutormatch`, palette/font ADR 0015; cập nhật tracker tất cả route `PENDING_APPROVAL`, chưa sửa UI.
- Kiểm tra consistency: inventory, page spec và tracker mỗi phần 70 route; `scripts/check-docs.ps1` pass (37 Markdown files), `git diff --check` docs pass, diff migration trống. Chưa chạy frontend/browser tests do không thay code. Impeccable launcher không khả dụng do cần engine/cache ngoài workspace; đọc project artifacts theo fallback.

## 2026-10-15 — Căn lại concept và khôi phục luồng chọn môn trên dữ liệu hiện có

- Sắp xếp trang chủ theo concept đã duyệt: hero, 9 môn học, “Cách hoạt động” và “Trở thành gia sư” nằm cùng màn hình desktop; thêm minh họa vector hero mới. Trên mobile, trang chọn môn ưu tiên danh sách, còn tìm kiếm/cấp học mở khi cần.
- Thay request danh mục 100 môn bằng 11 môn ở trang gia sư, hồ sơ và xếp hạng để tránh lỗi trang đầu API. Xác minh API chạy trả 200 với size 11, 500 khi truy vấn chạm môn thứ 12; luồng chọn Toán từ `/subjects` sang `/teachers?subjectId=...` tải 2 gia sư.
- TypeScript và ESLint pass; 6 bộ Jest liên quan pass (10/10); Docker production build pass và frontend container đã cập nhật trên `localhost:3000`. Playwright xác nhận 11 môn hiển thị, thao tác chọn môn bằng chuột/bàn phím tới đúng danh sách 2 gia sư, mobile 375px không tràn ngang. `scripts/check-docs.ps1` pass; không sửa backend hoặc migration. Endpoint vẫn lỗi 500 khi chạm môn thứ 12; chi tiết ở `docs/STATUS.md`.
- Xem lại dashboard gia sư trên mobile và thêm khoảng đệm cuối trang để thanh điều hướng cố định không che phần lịch; production Docker build lại pass, frontend healthy và trang chủ trả HTTP 200.

## 2026-10-15 — Triển khai concept Bàn học mở trên website thật

- Áp dụng bố cục ngà/vàng và hệ component chung cho Public, Auth, Student, Teacher, Admin; hero và thẻ môn gọn theo hai ảnh concept đã duyệt. Thêm icon theo môn, form đăng nhập hai vùng, điều hướng workspace ưu tiên tác vụ và bottom navigation mobile; giữ tất cả route trong “Thêm”.
- Trang chọn môn có đường xem mọi gia sư khi API lỗi. Bộ lọc gia sư chia nhóm cơ bản/nâng cao, mobile có nút xem kết quả cố định; preview mobile trả focus về thẻ khi đóng. Dashboard học viên/gia sư nhấn vào việc tiếp theo từ dữ liệu hiện có.
- Không sửa backend/API hoặc migration trong đợt này. TypeScript, ESLint, production build, Jest 301/301 và Playwright public 5/5 pass; ảnh desktop/mobile đã lưu trong `docs/redesign/design/`. Kết quả và giới hạn kiểm tra chi tiết ghi tại `docs/STATUS.md`.

## 2026-10-15 — Đề xuất concept Bàn học mở trước khi code

- Ghi brief và hai bản comp cho Public, Auth, Student, Teacher, Admin trong `docs/redesign/design/`. Concept giữ vàng sáng và font hiện có, mở rộng sang bố cục, hệ component, trạng thái, chuyển động và desktop/mobile; chưa sửa UI hoặc nghiệp vụ.
- Impeccable engine chưa chạy được; đã theo fallback của skill. Docs check pass (29 Markdown files), migration diff trống, chưa chạy test code.

## 2026-10-15 — Khởi tạo hồ sơ sản phẩm Impeccable

- Thêm `PRODUCT.md` ghi lại hai nhóm người dùng chính ngang nhau, mục đích kết nối học 1-1 theo mục tiêu, toàn bộ nhóm luồng chức năng hiện có và các giới hạn dữ liệu/ủy quyền cần bảo toàn. Người dùng xác nhận giữ mọi luồng hiện có.
- Người dùng chọn comp-first cho landing/showcase; đã lưu lựa chọn trong `.impeccable/config.json`. `impeccable init` không chạy do engine cần quyền mạng và ghi cache ngoài workspace; hồ sơ được lập theo fallback được skill hướng dẫn.
- Chưa chạy test code; docs check pass trên 28 Markdown files, migration diff trống.

## 2026-10-15 — Sửa icon che tên môn trong thẻ homepage

- CSS cũ còn đặt icon môn học ở `position:absolute` và giữ khoảng đệm trái của bố cục cũ, khiến icon chồng lên tên. Reset vị trí/khoảng cách theo flex và ẩn mô tả dài trong card.
- SubjectGrid Jest 5/5, typecheck, ESLint component và production build pass. Kiểm tra trình duyệt trực quan chưa chạy vì thiếu Chromium Playwright; migration diff trống.

## 2026-10-15 — Đồng bộ thẻ môn học trang chủ với trang tìm gia sư

- Chuyển danh sách môn trên trang chủ thành thẻ ngang hai cột, chỉ giữ icon, tên môn, cấp học và mũi tên; bỏ mô tả để giữ mỗi thẻ gọn như ảnh tham khảo. Mobile hiển thị một cột.
- SubjectGrid Jest 5/5, typecheck, ESLint component và Next production build pass. Chưa chạy Playwright trực quan; migration diff trống.

## 2026-10-15 — Bổ sung minh họa flat vector cho phần giới thiệu

- Thay hình khối CSS ở khối “Một gia sư phù hợp…” bằng minh họa trong suốt học viên trao đổi bài với gia sư qua laptop; bổ sung alt text và hiển thị responsive.
- LandingPage Jest 2/2, ESLint, typecheck và Next production build pass. Impeccable engine không khả dụng do cần quyền ghi/tải ngoài workspace; fallback theo hướng dẫn skill. Migration diff trống.

## 2026-10-15 — Thu gọn card môn học trang chủ

- Thiết kế lại card theo bố cục gọn: icon nhỏ, tên môn/cấp học rõ, mô tả tối đa hai dòng, icon điều hướng đồng bộ; bỏ dòng kêu gọi chọn môn bị lặp và tránh chồng viền với bóng.
- Responsive 4/3/2/1 cột theo kích thước màn hình. SubjectGrid Jest 5/5, typecheck, ESLint component, Next production build và docs check pass. Chưa chạy Playwright trực quan; migration diff trống.

## 2026-10-15 — Nâng cấp thanh điều hướng (Navbar) theo phong cách hiện đại

- Chuyển bố cục thanh top sang phong cách hiện đại (Preply-inspired): Gom nhóm logo thương hiệu cùng các liên kết điều hướng chính ở bên trái, phân bổ tiện ích ngôn ngữ/trợ giúp và khối hành động đăng nhập/bắt đầu học ở bên phải.
- Nút "Đăng nhập" dùng viền nét mảnh kèm icon; nút "Bắt đầu học" dùng nền vàng thương hiệu `#FFD43B`; chiều cao thanh gọn gàng 64px với hiệu ứng mờ nền `backdrop-filter: blur(12px)`.
- Bổ sung `PRIMARY_SCHOOL` vào enum `EducationLevel` backend để tương thích an toàn với dữ liệu seed và chuẩn hoá về `ELEMENTARY` trong `SubjectService`.
- Backend unit test 4/4 pass; Jest `Navbar.test` và `page.test` pass; TypeScript và Next production build pass. Migration diff trống; không mutate Supabase.

## 2026-10-15 — Tinh gọn CTA trang chủ

- Bỏ khung trắng, bộ chọn môn và dòng hướng dẫn khỏi hero; giữ CTA vàng “Tìm gia sư” dẫn đến `/subjects`, với trạng thái hover, focus và reduced motion.
- Cập nhật test component, hành trình public và đặc tả trang chủ. Kết quả kiểm tra được ghi trong `docs/STATUS.md`.

## 2026-10-15 — Tinh gọn khối quy trình bắt đầu học trên trang chủ

- Chuyển 3 bước bắt đầu học (`.tm-how-section`) từ dạng card hộp đóng kín sang luồng quy trình mở (Connected Steps) với đường line tiến trình, badge số bước `01`, `02`, `03` và icon 36px gọn gàng.
- Bỏ nhãn thẻ nhỏ bị lặp từ ("Chọn", "Trao đổi", "Bắt đầu"), giảm chiều cao khối giao diện và tối ưu hiển thị responsive trên mobile.
- `page.test` 10/10 suites, 13/13 tests pass; TypeScript `tsc --noEmit` và Next production build pass. Migration diff trống; không kết nối hoặc mutate Supabase.

## 2026-10-15 — Làm sáng trang chủ Tutor Match

- Đổi vàng thương hiệu sang `#FFD43B`, chuyển CTA chính sang nền vàng/chữ mực và căn form tìm gia sư thành hai hàng rõ ràng.
- Bổ sung minh họa vector cho học viên/gia sư, giới thiệu nền tảng, ba bước bắt đầu học, lời mời gia sư và reveal theo cuộn có hỗ trợ giảm chuyển động.
- Jest 118/118 suites, 302/302 tests pass; TypeScript, lint và production build pass. Playwright chưa xác minh được vì môi trường chặn kết nối loopback. Migration diff trống; không kết nối Supabase.

## 2026-10-15 — Nâng cấp giao diện Tutor Match

- Đồng bộ CSS và Ant Design theo vàng `#F4CA28`, mực `#171714`, Be Vietnam Pro/Inter; giữ palette trạng thái semantic và áp dụng nền/tokens chung cho các workspace, Auth và public pages.
- Nối luồng tìm gia sư qua `/subjects`, lọc danh mục theo cấp học, tải môn được chọn theo ID và giữ query tìm kiếm hợp lệ qua login. Danh sách gia sư dùng DTO thật và preview hover/focus/tap; bỏ dữ liệu mẫu khỏi các thành phần mới.
- Thêm `GET /api/public/subjects/{id}` với hành vi active-only/404. Xác minh backend TDD 4/4, Jest 118 suites/302 tests pass (Jest báo open handle sau khi chạy), Playwright 10/10 và Next production build. Migration diff trống; không kết nối Supabase.

## 2026-10-15 — Demo HTML danh sách gia sư

- Thêm trang prototype HTML độc lập với nhận diện vàng, danh sách hồ sơ mẫu và bảng xem nhanh tương tác; mọi nội dung mẫu được gắn nhãn và các CTA không gửi yêu cầu thật.
- Kiểm tra cú pháp JavaScript nhúng và cấu trúc HTML cơ bản pass. Kiểm tra trực quan hover/mobile chưa xác minh được vì chính sách trình duyệt chặn quan sát URL `file://`. Không có migration mới; không kết nối Supabase.

## 2026-10-15 — Sửa cấu hình Google OAuth production

- Cập nhật Client ID cục bộ để khớp OAuth client “TutorMatch Production”, rồi tạo lại backend container; không thay đổi hoặc ghi secret vào repository.
- Backend/frontend/Redis healthy, connector Up; health API cục bộ và công khai trả 200. OAuth initiation cục bộ và công khai trả 302 với Client ID và callback API đúng. Đăng nhập Google hoàn chỉnh trên trình duyệt chưa được xác minh. Migration diff trống; không chạy thao tác schema Supabase.

## 2026-10-15 — Đổi host port backend Docker cho máy Windows

- Chuyển cổng truy cập cục bộ của backend từ 8080 sang 18080 vì Tomcat11 trên máy đang lắng nghe cổng 8080; cổng nội bộ Docker vẫn là 8080 để giữ nguyên route Cloudflare Tunnel.
- Xác minh từ phiên người dùng: lần khởi động sau build thành công, backend/frontend/Redis healthy và container cloudflared started. Chưa xác minh connector active hoặc hostname HTTPS/API, OAuth, WebSocket. Không có migration mới; không kết nối Supabase.
- Kiểm tra runtime tiếp theo từ PowerShell người dùng: frontend cục bộ trả 200, backend health cục bộ trả 200/UP; container cloudflared vẫn `Up`. Log xác nhận route cũ tới cổng 8081 bị từ chối và connector đã nhận cấu hình route mới. Truy cập hostname công khai vẫn chưa xác minh thành công.
- PowerShell trên máy người dùng hiện không phân giải được cả hostname web và API; ảnh DNS Cloudflare xác nhận chỉ có tám bản ghi mail, thiếu A/CNAME cho web/API. Bổ sung bước kiểm tra hai DNS record của Tunnel vào hướng dẫn; chưa thêm bản ghi trên Cloudflare hoặc xác minh HTTPS/OAuth/WebSocket.
- Người dùng báo đã thêm hai CNAME web/API tới tunnel `edtech`; đang chờ kiểm tra DNS và HTTPS sau thay đổi, chưa xác nhận public runtime.
- Người dùng xác nhận web công khai mở được qua HTTPS sau khi thêm DNS. Google OAuth còn `400 redirect_uri_mismatch`; bổ sung hướng dẫn Save callback trên đúng OAuth client ID và xác minh đăng nhập riêng. API public health/WebSocket chưa xác minh.
- Kiểm tra mới nhất: frontend `/auth/register` và API `/health` đều trả 200 qua HTTPS và cục bộ; container backend/frontend healthy, connector Up. Backend OAuth trả 302 tới Google với 
edirect_uri` API đúng. Chrome từng báo lỗi kết nối tunnel sau khi mở web; không tái hiện qua HTTP tại thời điểm kiểm tra. Cần đối chiếu OAuth Client ID và thử lại Chrome/WebSocket.
- Làm rõ hướng dẫn OAuth: Spring backend kết nối trực tiếp Google và lưu user trong PostgreSQL của Supabase; không dùng Supabase Auth và không cần Supabase redirect URL. Google 
edirect_uri_mismatch` vẫn chờ xác minh callback đã Save trên đúng OAuth client.
- Chẩn đoán OAuth xác nhận callback backend và Google error details trùng nhau, nhưng Client ID đang chạy/`.env.cloud` thuộc client cũ, khác OAuth client “TutorMatch Production” đã thêm callback API. Chờ sao chép đúng Client ID, tạo lại backend container và xác minh đăng nhập; không đụng Supabase.

## 2026-10-15 — Docker production qua Cloudflare Tunnel

- Thêm frontend Docker standalone và Compose production chạy backend cloud, Redis có volume, cùng Cloudflare connector qua token file môi trường git-ignore. Hướng dẫn hai route web/API và OAuth.
- Giới hạn Actuator công khai ở health không chi tiết; Prometheus yêu cầu role Admin. Cập nhật integration assertion cho request chưa xác thực.
- Compose config và build cả hai image pass; Next production build/TypeScript và Maven package compile pass. `ActuatorMetricsIntegrationTest` pass 2/2 trên Testcontainers cục bộ; docs check 27 Markdown files pass. Chưa khởi chạy stack hoặc kiểm tra tunnel/runtime; không có migration mới, không kết nối Supabase.

## 2026-10-05 — Local Docker deployment sau Nginx

- Thêm Next.js frontend vào Compose, tách DB admin khỏi role ứng dụng, bind mount PostgreSQL vào `data/postgres` trên D:, và giữ Redis trong network riêng. Nginx phục vụ UI/API trên localhost:8081, chuyển tiếp `/ws` cho STOMP; không publish trực tiếp frontend/backend.
- Thêm local `.env.example`, Dockerfile frontend và pg_dump backup script; không thêm migration. Kiểm tra Compose/build pass, các container healthy, UI/health/public subjects trả 200, WebSocket handshake mở; PostgreSQL giữ Flyway V41/39 bảng sau restart và dump có thể đọc bằng `pg_restore --list` (370 entries).
- Maven image build dùng `-DskipTests`; chưa chạy lại bộ backend tests trong đợt triển khai. OAuth/payment/email thật chưa xác minh; không kết nối Supabase.

## 2026-09-23 — Hoàn thiện ba lỗi logic booking, settlement và admin user (working tree)

- Hiển thị lý do hủy booking an toàn, bảo toàn định dạng và xác nhận giới hạn 1.000 UTF-16 units ở UI/backend.
- Đổi admin settlement sang typed read model với batch lookup; thêm drawer chi tiết trước action và chặn xử lý dispute khi thiếu reason.
- Thêm user directory API/trang admin, moderation action và audit deep-link lọc theo user; sửa facade boundary để không phụ thuộc trực tiếp auth domain từ admin module.
- Frontend typecheck/lint/build và Jest `116/116` suites (`298/298` tests) pass; Playwright Chromium `9/9` pass với một worker. Backend focused `21/21` pass gồm ArchitectureTest. `mvn clean verify` bị chặn bởi Docker không khả dụng cho Testcontainers (102 lỗi khởi tạo integration DB trong 501 tests); integration persistence chưa xác minh. Không có migration mới, không kết nối Supabase.

## 2026-09-23 — Frontend hardening sau khôi phục (working tree)

- Chuẩn hóa giờ Việt Nam cho booking/learning/finance, sửa điều kiện action booking và cảnh báo lịch rảnh; thêm chat attachment, retry/echo, accessible label và scroll ổn định.
- Cập nhật settlement admin, idempotency retry cho các mutation tài chính, teacher overview/navigation, notification semantics và source token màu chung cho CSS/Ant.
- Nối intent lưu nháp/nộp bài vào submission status backend; thêm filter trạng thái cho admin credential queue và chỉ cho phép action ở item `PENDING`.
- Bổ sung regression tests cho timezone, settlement, chat, navigation, token, idempotency, submission draft và credential filter; typecheck/lint/build, Jest 289/289 và Playwright 8/8 ở chế độ một worker pass. Visual review Teacher overview desktop/mobile dùng API fixture; các journey backend thật/visual regression toàn hệ thống chưa xác minh. Chưa triển khai production. Migration diff trống, không kết nối Supabase.
- Diagnose follow-up xác nhận ba khoảng trống chưa xử lý: admin không thấy lý do/ngữ cảnh trước quyết định settlement, booking đã hủy không hiện lý do, và khóa/mở khóa user thiếu danh sách người dùng để mở modal hiện có.

## 2026-09-22 — Diagnose logic API/backend và UI/frontend

- Đối chiếu endpoint với nút/route, ghi nhận sáu khoảng trống hoặc sai điều kiện hiển thị trong `docs/STATUS.md`; không sửa hành vi sản phẩm. Frontend typecheck và 25 focused backend tests pass; E2E/HTTP role chưa chạy. Migration diff trống, không kết nối Supabase.

## 2026-09-22 — Audit frontend sau khi khôi phục

- Đối chiếu các mục audit với source frontend/backend: xác nhận nhóm lỗi timezone, chat, accessibility, navigation và token duplication; đánh dấu các mục cần runtime audit và mục settlement cũ đã lỗi thời. Không sửa code/backend trong đợt audit.

## 2026-09-22 — Cài lại frontend dependencies và xác minh dev server

- Cài dependency đúng theo `frontend/package-lock.json` bằng `npm ci` sau khi khôi phục source từ GitHub; xác nhận Next.js dev server khởi động và route `/` trả HTTP 200.

## 2026-09-22 — Khôi phục frontend từ GitHub

- Khôi phục toàn bộ thư mục `frontend` nguyên trạng từ `origin/main`; không reset toàn bộ branch và không thay đổi backend.

## 2026-09-22 — Đóng frontend rebuild theo yêu cầu

- Xóa toàn bộ thư mục `frontend` hiện tại và các tài liệu riêng của đợt rebuild (`FRONTEND_REBUILD_PLAN.md`, component registry). Backend và tài liệu domain/architecture dùng chung được giữ nguyên.

## 2026-09-22 — Cập nhật palette Warm Ivory + Rosewood

- Cập nhật toàn bộ light/dark design tokens, Ant Design mapping và state tương tác sang Warm Ivory + Rosewood; giữ nguyên geometry/layout và loại bỏ tên palette/implementation khỏi product UI.
- Không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Slice 2 hero visual direction bắt đầu

- Thêm hero collage learner/tutor nguyên bản với nền marketing pink, typography display lớn, CTA rõ và proof strip; mobile chuyển thành bố cục ảnh trên/copy dưới sau screenshot correction loop.
- Asset: `frontend/public/images/tutor-match-hero-pink.png`. Slice 2 vẫn đang triển khai, chưa mở Slice 3.
- Nối landing vào public API subjects/teachers với loading, empty/error và partial-failure state; không tạo số liệu giả khi backend không khả dụng.

## 2026-09-22 — Slice 1 Design System/AppShell hoàn tất visual gate

- Dựng lại frontend từ reset: token source palette cũ, light/dark Ant Design theme, CSS geometry tokens, Global Top Navigation, Student Workspace Left Rail, AppShell, PageHeader và feedback states.
- Thêm component registry entry và test nền tảng: token tests, AppShell navigation test; typecheck, lint, Jest `3/3`, Next build static routes pass.
- Render thật và review screenshot ở CSS viewport `1440×900` và `390×844`; đã kiểm tra mobile drawer, dark theme, CTA contrast, rail collapse và feedback geometry. Sau correction loop, Slice 1 PASS; Slice 2 chưa bắt đầu.
- Không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Bổ sung enforcement rules cho visual implementation

- Cấm unspecified magic numbers ngoài token/contract và yêu cầu chọn scale gần nhất.
- Bắt buộc screenshot correction loop: lệch design brief thì sửa code, render lại và review lại trước PASS.
- Bắt buộc component registry: component visual mới phải kiểm tra `shared/design-system` và khai báo anatomy, variants, states, mobile behavior trước khi reuse nhiều page.
- Chỉ cập nhật docs; frontend chưa được tạo lại, không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Khóa UI Component Geometry và Hybrid Navigation contract

- Thêm contract normative cho kích thước, spacing, radius, state, responsive behavior và anatomy của component; contract đã có thì không được đổi vì lý do thẩm mỹ.
- Khóa mô hình **Global Top Navigation + Workspace Left Rail**, gồm trách nhiệm navigation, kích thước rail/header, grouping, focus behavior và mobile drawer.
- Quy định no-guess rule: component chưa đặc tả dùng Ant Design + token hiện có và phải ghi vào `UNSPECIFIED UI DECISIONS`.
- Chỉ cập nhật docs; frontend chưa được tạo lại, không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Chuẩn hóa tên palette và khóa gate giữa các slice

- Thay mô tả palette cũ trong Slice 1 bằng tên duy nhất theo design contract.
- Thêm completion gate sau Slice 1–10: screenshot desktop/mobile phải được review và PASS hierarchy, spacing, proportion, visual balance trước khi tiếp tục hoặc nghiệm thu.
- Chỉ cập nhật docs; frontend chưa được tạo lại, không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Khóa Visual Composition Contract cho rebuild

- Bổ sung contract hình học và responsive vào `FRONTEND_REBUILD_PLAN.md`: page width/grid, section rhythm, typography, hero, card anatomy, teacher result/profile, controls/filter, image ratio, accent distribution, border/shadow, motion và alignment.
- Thêm visual balance checklist và required screenshots ở bốn viewport trước khi nghiệm thu; yêu cầu Preply reference evidence cụ thể cho từng public/marketplace screen.
- Frontend vẫn chưa được tạo lại; chỉ cập nhật docs, không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Bổ sung reference-driven design review cho frontend rebuild

- Đọc lại Preply production pages và ghi nguyên tắc chuyển hóa cho Tutor Match: tutor-first evidence, subject discovery, CTA hierarchy, section composition đa dạng và responsive re-composition; không sao chép thương hiệu, copy hoặc layout.
- Thêm gate bắt buộc 
eference → Taste → hierarchy → composition → cards decision → desktop/mobile → states/a11y` trước khi viết code.
- Mở rộng roadmap Slice 1-10 bằng mục tiêu hierarchy, dominant composition, primary CTA, nơi dùng/không dùng card và desktop/mobile transformation; giữ dials đã chốt và không khôi phục frontend cũ.
- Chỉ cập nhật tài liệu; frontend vẫn chưa tồn tại, không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Chốt lại toàn bộ Design System color palette

- Chốt palette trung tính có accent đơn cho hướng Premium education platform; semantic colors chỉ dành cho trạng thái.
- Bổ sung contract đầy đủ cho light/dark background, surface, text, border, primary, semantic và interaction tokens; ghi rõ primary chỉ dùng cho CTA/active/selected/link/focus có chủ đích.
- Kiểm tra contrast token bằng WCAG relative luminance: text, primary button, semantic states, focus ring, disabled state và border-strong đạt yêu cầu đã định. Source token thực tế sẽ được tạo trong `shared/design-system` ở Slice 1; hiện frontend vẫn chưa tồn tại.
- Tham khảo Preply và các visual reference education premium để đánh giá direction, không sao chép palette hoặc layout. Không sửa backend/migration và không kết nối Supabase.

## 2026-09-22 — Phân mức Taste và phạm vi skill theo slice

- Cập nhật `FRONTEND_REBUILD_PLAN.md` với ba dial riêng cho Slice 1, 2, 3-4 và 5-8; Slice 9 giữ dial theo surface đã xây, Slice 10 không dùng dial/Taste.
- Taste dẫn dắt visual landing/portfolio và audit consistency; product UI dày đặc theo contract/component patterns, `web-design-guidelines` chủ đạo cho accessibility. Motion trên landing phải có mục đích và reduced-motion fallback.
- Chỉ sửa tài liệu. Frontend chưa tồn tại để chạy test UI; không sửa backend/migration hoặc kết nối Supabase.

## 2026-09-22 — Chọn định hướng Premium education platform cho rebuild

- Đổi lựa chọn trong `FRONTEND_REBUILD_PLAN.md` từ hướng A sang B, đồng bộ token đề xuất, landing, hồ sơ gia sư, workspace, visual references và tiêu chí nghiệm thu.
- Đặt ranh giới cho cảm giác “premium”: không tự tạo bằng chứng, rating, ảnh gia sư hoặc lời hứa kết quả; bố cục vẫn phải trung thực khi dữ liệu thiếu. Chưa triển khai frontend hay chạy test UI.
- Không sửa backend/migration và không kết nối hoặc mutate Supabase.

## 2026-09-22 — Đặt lại frontend về điểm xuất phát

- Ghi nhận toàn bộ file frontend đã bị xóa khỏi working tree theo quyết định xây lại từ đầu; chưa có ứng dụng frontend hoặc slice nào được nghiệm thu trong trạng thái hiện tại.
- Gỡ các ghi chép hoàn thành hai slice đầu và contract theme cũ khỏi tài liệu hiện hành để tránh dùng chúng làm mẫu triển khai. Kế hoạch rebuild chỉ còn là phạm vi dự kiến; mọi thiết kế và kiểm thử phải được xác nhận lại trên code mới.
- Không sửa backend/migration, không kết nối hoặc mutate Supabase. Kiểm thử frontend chưa chạy vì ứng dụng đã bị xóa.

## 2026-09-22 — Cập nhật bộ dữ liệu seed Supabase đồng bộ Schema V41

- Làm mới `supabase_seed_data.sql` đồng bộ với schema V41: thêm khối `TRUNCATE TABLE ... CASCADE` dọn dẹp sạch dữ liệu cũ các bảng nghiệp vụ, bảo tồn lịch sử `flyway_schema_history` và danh mục `provinces`/`wards`.
- Bổ sung các cột bắt buộc: 6 trường snapshot cho `invoices` (V26), địa chỉ tỉnh/xã cho `teacher_profiles` (V39), chứng chỉ đã duyệt cho `teacher_credentials` (V40), bản ghi quyết toán hai bên cho `booking_settlements` (V41).
- Bổ sung dữ liệu thực tế cho bài tập `assignments`/`submissions` (V12), trao đổi `conversations`/`messages` (V13) và thông báo `notifications` (V13/V20).
- Không có migration Flyway mới; không mutate Supabase từ repository.

## 2026-09-21 — Nullable audit snapshots và booking teacher-name fallback

- Sửa AuditLog snapshot để giữ JSONB null, từ chối null key trước khi copy và chỉ shallow-copy map ngoài; audit vẫn bắt buộc cùng transaction nghiệp vụ.
- Thêm regression test residence/persistence và booking list/detail cho teacher name thiếu, null, blank và hợp lệ. Hai integration suite đang blocked cục bộ vì Docker Desktop không lắng nghe.
- Cập nhật contract, architecture và ADR; không thay đổi migration.
- Tinh chỉnh assertion integration: dùng jsonb_exists thay cho toán tử ? trong JdbcTemplate và so sánh JSON qua tree để không phụ thuộc whitespace.
- Focused integration sau sửa pass 14/14 (0 failure/error/skipped), xác nhận Docker/Testcontainers và toàn bộ residence/audit persistence scenarios.
- Full backend clean verify sau đó pass 500/500 tests, 0 failure/error/skipped; package và Spring Boot repackage pass.
- Docker cleanup sau full suite hoàn tất: container, volume và system prune đều thành công, thu hồi 0B.

## 2026-09-21 — Chặn workspace skeleton vô hạn

- Thêm timeout 15 giây cho mọi Axios request từ browser, bao gồm refresh token; request không còn có thể giữ loading vô hạn khi Render cold-start hoặc redeploy.
- React Query chỉ retry lỗi timeout một lần rồi chuyển sang error state có thao tác thử lại; thêm regression assertions cho cấu hình Axios và retry policy.
- Frontend CI run `35604116297` pass toàn bộ unit tests, build và Playwright E2E; Vercel deploy thành công và bundle production đã nhúng timeout 15 giây. Production bundle dùng đúng Render API URL, CORS preflight từ Vercel trả `200`; các log `GET/HEAD / -> 401` là request vào backend root, không phải API workspace. Không có migration mới.

## 2026-09-21 — Tự phục hồi landing page sau Render cold start

- Giảm TTL output landing fallback từ 300 xuống 30 giây nhưng giữ cache dữ liệu public 300 giây khi fetch thành công.
- Khi subjects hoặc teachers fail trong server render, client tự refresh sau 5 giây và mỗi 15 giây cho tới khi backend phục hồi; thêm regression tests cho bật/tắt vòng retry.
- Frontend CI run `35600022117` pass unit tests, build và Playwright E2E; Vercel production smoke xác nhận landing có dữ liệu, health/subjects/teachers đều trả `200`. Không có migration mới và không mutate Supabase.

## 2026-09-21 — Làm nhẹ health check Render

- Đổi health check của Render từ `/actuator/health` sang `/health`, endpoint public nhẹ chỉ xác nhận process HTTP đã sẵn sàng, tránh phụ thuộc Redis/DB health contributor trong lúc deploy.
- Đồng bộ Docker với Render bằng `PORT=10000`, `EXPOSE 10000` và bind `0.0.0.0`; log deploy trước đó xác nhận ứng dụng đã start port 10000 nhưng port scanner vẫn timeout, cần xác minh lại bằng redeploy production.

## 2026-09-21 — Chặn cấu hình Brevo placeholder trên Render

- Preflight cloud mặc định kiểm tra Brevo khi `APP_EMAIL_PROVIDER` bỏ trống, từ chối placeholder literal và validate định dạng `BREVO_SENDER_EMAIL`.
- Blueprint Render khai báo `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` và `BREVO_SENDER_NAME` dạng `sync: false`, không đưa secret vào source.

## 2026-09-21 — Hardening Vercel prerender public pages

- Thêm timeout 10 giây cho server-side public fetch; route landing/ranking không còn treo build khi backend cold start hoặc tạm thời không phản hồi.
- Thêm regression assertion cho timeout signal; local Next production build pass đủ 63 route.

## 2026-09-21 — Chuyển email production từ SMTP sang Brevo

- Thay SMTP transport bằng Brevo Transactional Email API qua HTTPS; giữ transactional outbox và dùng ID outbox làm idempotency key.
- Phân loại lỗi tạm thời để retry và lỗi request vĩnh viễn để dừng retry; local/test tiếp tục dùng logging transport.
- Bỏ Spring Mail và cấu hình SMTP; cloud chỉ cần API key, sender email và sender name của Brevo, còn endpoint/timeout dùng default nội bộ; preflight không in secret. Không có migration mới và không kết nối Supabase.

## 2026-09-21 — Tách xác minh danh tính khỏi minh chứng năng lực

- Đổi khu vực tài liệu teacher thành “Xác minh danh tính”, chỉ nhận `IDENTITY`; bằng cấp/chứng chỉ dùng khu vực Minh chứng năng lực.
- Ẩn `secureUrl` khỏi DTO teacher và cập nhật copy UI/admin để giấy tờ tùy thân được coi là riêng tư.
- Không có migration mới; dữ liệu cũ được giữ nguyên.

## 2026-09-21 — Làm rõ giao diện Minh chứng năng lực

- Thiết kế lại form chứng chỉ với field dọc, dropzone, preview, CTA màu chủ đạo và danh sách trạng thái/chỉnh sửa/xóa; sửa màn hình admin xem proof qua API có xác thực.
- Đồng bộ giới hạn proof JPG/PNG/PDF xuống 5MB tại frontend, backend và API contract; giữ schema hiện có.
- File proof chỉ được mở qua API owner có xác thực; public badge chỉ hiển thị nhãn đã duyệt. Không có migration mới, không kết nối Supabase.

## 2026-09-21 — Sửa đổi role auth và đồng bộ session nhiều tab

- Validate return URL cùng role sau login; loại query/hash không nằm trong allowlist và redirect lồng.
- Logout luôn dọn local session/cache và điều hướng tab nguồn về login; tab khác đồng bộ qua storage revision và visibility change.
- Thêm session identity bằng `crypto.randomUUID()`, chống refresh response cũ ghi đè phiên mới và clear toàn bộ React Query cache khi đổi danh tính.
- Thêm regression tests cùng tab, hai page cùng browser context và frontend CI workflow; chưa gọi production smoke khi Render backend còn 502.
- Bổ sung UUID-compatible CSPRNG fallback cho local HTTP test và chặn RoleGuard gắn lại `?redirect=` sau logout.
- Docker verification hoàn tất: Jest `103/103` suites, `268/268` tests, lint, typecheck, Next build và Playwright Chromium E2E `6/6` đều pass.
- GitHub Frontend CI run `35581295953` pass toàn bộ; Render health probe vẫn trả `503 Service Unavailable`, nên chưa gọi production smoke/deploy là hoàn tất.
- Không có migration mới và không kết nối hoặc mutate Supabase.

## 2026-09-21 — Sửa Google OAuth complete-registration

- Đặt `notifyParent=false` khi tạo user mới qua Google OAuth, khớp invariant `users.notify_parent NOT NULL DEFAULT false`.
- Thêm regression assertion cho OAuth registration; OAuth focused suite `13/13` pass.
- Không có migration mới và chưa redeploy production.

## 2026-09-20 — Sửa Spring MVC binding cho teacher search

- Tái hiện lỗi production bằng request MVC thật tới `GET /api/public/teachers?sort=rating_desc&page=0&size=6`: Spring ném `No primary or single unique constructor found` vì record `TeacherSearchParams` có hai constructor.
- Xóa constructor phụ 12 tham số, dùng duy nhất canonical constructor 14 tham số và cập nhật các test fixture liên quan.
- Thêm regression test đi qua `MockMvc` để khóa đúng seam HTTP. Focused teacher-search `12/12` pass; full backend PostgreSQL 16/Redis Testcontainers `478/478` pass (`0` failure, `0` error, `0` skipped).
- Không có migration mới; không kết nối hoặc mutate Supabase. Cần smoke test endpoint production sau khi Render redeploy.

## 2026-09-17 — Hardening deploy Render 512 MiB

- Chẩn đoán log deploy xác nhận backend khởi động xong nhưng bị Render dừng vì vượt 512 MiB; probe mặc định `HEAD /` đồng thời nhận 401.
- Giới hạn JVM container ở heap 128–256 MiB, metaspace 128 MiB, code cache 64 MiB, Serial GC và stack 512 KiB; giảm pool DB container mặc định còn 5 và hỗ trợ biến `PORT` của Render.
- Thêm Blueprint 
ender.yaml` cho service Docker trên nhánh `main`, root `backend` và health check `/actuator/health`; không đưa secret vào source.
- Contract tests cấu hình `7/7` pass, Maven package pass. Health integration bị chặn do Docker daemon local không khả dụng; cần xác minh deploy mới và RAM thực trên Render. Không có migration mới và không kết nối Supabase.

## 2026-09-17 — Sửa kiểm tra metadata Flyway trên CI

- Giới hạn ba truy vấn `pg_constraint`/`pg_trigger` trong `FlywayMigrationTest` vào đúng bảng thuộc schema `public`, tránh đếm đối tượng từ các schema thử nghiệm cùng database. Run `35169842151` trước sửa có 472/474 test pass; run `35170433438` sau sửa pass 474/474 test, Compose startup smoke và Docker image build. Không thay đổi migration hoặc schema production.

## 2026-09-15 — Hardening settlement V41 (working tree)

- Giữ `ledger_entries.entry_type` ở `varchar(30)` bằng tên `SESSION_ESCROW_HELD`/`SESSION_ESCROW_RELEASED`, thêm backfill settlement cho booking trả phí cũ, bật RLS cho `provinces`/`wards`, đồng bộ `netAmountVnd` và sửa admin queue dùng `bookingId`.
- Booking unit tests pass; migration/Testcontainers chưa xác minh do Docker engine không có socket hợp lệ. Supabase V41 vẫn Pending; chưa apply production.

## 2026-09-14 — Tiếp tục hoàn thiện xác nhận hai bên và escrow V41 (working tree)

- Tách trạng thái tiêu thụ lượt khỏi quyền nhận tiền; bổ sung xác nhận hai bên, timeout giữ hộ, khiếu nại/mở lại một lần và quyết định admin. Ghi debit/credit ở cả ledger ví và platform ledger; hiển thị số tiền đang giữ riêng khỏi số có thể rút.
- Bổ sung hàng đợi admin, trạng thái/hạn xác nhận trên booking và các kiểm tra migration V40→V41. Frontend typecheck, lint, production build và docs check pass; backend focused và PostgreSQL integration đang được kiểm tra lại sau sửa cuối. Supabase read-only xác nhận V40 `Success`, V41 `Pending`; chưa deploy V41.

## 2026-09-14 — Hoàn tất bốn phần hardening Luna & On-demand Next Data Cache Revalidation

- Supabase production đã áp dụng thành công toàn bộ migration V38–V40 (`State = Success`); kiểm tra read-only xác nhận 0 bản ghi `evidence_format = ''`, giữ nguyên schema V40 và không cần tạo V41.
- Sửa `evidence_format` đồng nhất cho cả create và update bằng MIME do Apache Tika xác nhận (`jpg`, `png`, `pdf`), không phụ thuộc extension hay Content-Type client gửi.
- Bổ sung bộ regression tests credential toàn diện:
  - Unit tests: `TeacherCredentialServiceTest` (15/15 pass) kiểm tra MIME mapping, chặn file rỗng/quá khổ/extension giả trước khi upload, audit trail và concurrency.
  - Controller contract tests: `TeacherCredentialControllerTest` (4/4 pass) và `AdminCredentialControllerTest` (4/4 pass) kiểm tra HTTP status, envelope, phân quyền admin và `Cache-Control: no-store`.
  - Integration tests: `TeacherCredentialIntegrationTest` (5/5 pass) trên PostgreSQL Testcontainers kiểm tra xử lý xung đột đồng thời trả 409 `CONCURRENT_MODIFICATION`, rollback storage cleanup xóa proof mới, after-commit cleanup xóa proof cũ, và public detail chỉ trả badge ID/label đã duyệt (không lộ URL/bytes).
  - Architecture guardrails: `ArchitectureTest` và `SolidGuardrailsArchitectureTest` (10/10 pass).
- Kích hoạt Next Data Cache on-demand tag revalidation:
  - Route Handler nội bộ `POST /api/internal/revalidate-public` với shared secret header `x-internal-secret` (`APP_INTERNAL_REVALIDATE_SECRET`, không dùng `NEXT_PUBLIC_*`) và 
evalidateTag(tag, { expire: 0 })` chuẩn Next 16.
  - Mapping tag: duyệt hoặc sửa/xóa credential đã duyệt invalidate `public-teacher:{id}`; đổi nơi ở invalidate `public-teacher:{id}` và `public-teachers`.
  - Backend gửi callback sau commit (`afterCommit`) qua `PublicCacheRevalidationClient` (3/3 pass), lỗi callback được log an toàn không làm hỏng command, transaction rollback không gửi sự kiện; giữ nguyên TTL 60–300s làm fallback.
  - Next.js production build pass 62 static/SSG pages và dynamic route handler.
- Kết quả kiểm thử nghiệm thu toàn diện:
  - Backend: `mvn test` pass `466/466` tests (`0` failure, `0` error, `0` skipped) trên PostgreSQL 16 và Redis Testcontainers.
  - Frontend: `npm run check` trong Docker pass toàn bộ (typecheck, lint, `101/101` test suites, `264/264` tests và Next.js production build).

## 2026-09-14 — Credential badges V40 (local)

- Thêm credential teacher với proof Cloudinary authenticated, workflow teacher CRUD và admin approve/reject; public chỉ hiển thị label đã duyệt.
- Giới hạn 10 credential/teacher, file JPG/PNG/PDF tối đa 10MB; local Flyway validate/clean tới V40 pass. Chưa preflight/apply Supabase.

## 2026-09-14 — Thêm reference địa giới hành chính V38 (local)

- Thêm `provinces` và `wards` theo cấu trúc hành chính Việt Nam 2025 (34 tỉnh/thành, 3.321 xã/phường), seed từ Open Admin Data.
- Local Flyway clean/upgrade tới V38 và kiểm tra cardinality/FK/code format đã pass; chưa triển khai Supabase production.

## 2026-09-14 — Thêm nơi ở một tỉnh/xã cho teacher profile V39 (local)

- Thêm `province_code`/`ward_code` nullable và FK composite bảo đảm xã thuộc đúng tỉnh.
- Thêm endpoint `PUT /api/teacher/profile/residence`; đổi nơi ở không làm profile `APPROVED` quay về `DRAFT`.
- Local Flyway clean/upgrade tới V39 và focused teacher profile/Flyway tests pass; chưa triển khai Supabase production.

## 2026-09-14 — Public location API và teacher search filter (local)

- Thêm API cascading tỉnh/xã và filter search trực tiếp trên `teacher_profiles.province_code`/`ward_code`.
- Cache key teacher search bao gồm location; card projection trả tên tỉnh/xã; focused search tests pass.

## 2026-09-14 — Frontend cascading location filter (local)

- Thêm dropdown tỉnh → xã cho hồ sơ teacher và trang tìm kiếm public; card hiển thị khu vực tổng quát.
- Typecheck pass; Jest Windows host còn bị `spawn EPERM`, chưa xác nhận test pass trong lượt này.

## 2026-09-14 — Triển khai RLS V37 Cụm System, Outbox & Audit Logs lên Supabase Production

- Thêm migration `V37__enable_rls_system_and_cleanup.sql` kích hoạt Row Level Security default-deny cho các bảng System, Outbox & Audit Logs: `platform_settings`, `audit_logs`, `email_outbox`, `modulebentity`.
- Thu hồi toàn bộ quyền (REVOKE ALL bao gồm TRUNCATE) từ `anon` và `authenticated` trên toàn bộ các bảng này.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (21 tests, clean V1->V37, upgrade V27..V36->V37, assert 33 bảng nghiệp vụ có RLS), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `platform_settings`, `audit_logs`, `email_outbox`), tạo mới `SystemOutboxRlsFlowIntegrationTest` (xác nhận luồng cấu hình singleton, ghi audit log, đưa email vào outbox và worker gửi mail qua PostgreSQL superuser).
- Verification: 29/29 tests local Testcontainers pass; preflight Supabase read-only validate 37 migrations pass; apply V37 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: 100% dữ liệu được bảo toàn (`platform_settings`: 1 row, `flyway_schema_history`: 37 rows); toàn bộ 34 bảng nghiệp vụ trong schema `public` đã được bảo vệ bởi RLS (0 data loss). Hoàn tất trọn vẹn toàn bộ 5 nhóm RLS migration!

## 2026-09-14 — Triển khai RLS V36 Cụm Communication & Notifications lên Supabase Production

- Thêm migration `V36__enable_rls_communication_tables.sql` kích hoạt Row Level Security default-deny cho 4 bảng: `conversations`, `messages`, `attachments`, `notifications`.
- Khắc phục lỗ hổng RLS bypass qua `TRUNCATE` bằng khối `REVOKE TRUNCATE ... FROM anon, authenticated`.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (20 tests, clean V1->V36, upgrade V27..V35->V36, assert 30 bảng có RLS), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `conversations`, `messages`, `notifications`), tạo mới `CommunicationRlsFlowIntegrationTest` (xác nhận luồng hội thoại, tin nhắn văn bản, file attachment, thông báo và trạng thái đã đọc trơn tru qua PostgreSQL superuser).
- Verification: 27/27 tests local Testcontainers pass; preflight Supabase read-only validate 36 migrations pass; apply V36 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: baseline 9 rows được bảo toàn 100% (`conversations`: 3, `messages`: 3, `attachments`: 0, `notifications`: 3) (0 data loss).

## 2026-09-14 — Triển khai RLS V35 Cụm Booking & Learning lên Supabase Production

- Thêm migration `V35__enable_rls_booking_learning_tables.sql` kích hoạt Row Level Security default-deny cho 9 bảng: `student_packages`, `package_extension_requests`, `trial_requests`, `bookings`, `session_reports`, 
eviews`, `teacher_stats`, `assignments`, `submissions`.
- Khắc phục lỗ hổng RLS bypass qua `TRUNCATE` bằng khối `REVOKE TRUNCATE ... FROM anon, authenticated`.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (19 tests, clean V1->V35, upgrade V27..V34->V35, assert 26 bảng có RLS), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `bookings`, `student_packages`, 
eviews`), tạo mới `BookingLearningRlsFlowIntegrationTest` (xác nhận luồng mua gói, đặt lịch, session report, review/stats, assignment/submission chạy trơn tru qua PostgreSQL superuser).
- Verification: 27/27 tests local Testcontainers pass; preflight Supabase read-only validate 35 migrations pass; apply V35 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: baseline 32 rows được bảo toàn 100% (teacher_stats: 20, bookings: 3, student_packages: 3, trial_requests: 2, session_reports: 2, reviews: 2, các bảng còn lại: 0) (0 data loss).

## 2026-09-14 — Triển khai RLS V34 Cụm Finance & Payments lên Supabase Production

- Thêm migration `V34__enable_rls_finance_tables.sql` bật Row Level Security default-deny cho 8 bảng: `wallets`, `ledger_entries`, `invoices`, `payment_transactions`, `payout_requests`, 
efund_requests`, `teacher_bank_accounts`, `finance_command_receipts`.
- Khắc phục lỗ hổng RLS bypass qua `TRUNCATE` bằng khối an toàn `REVOKE TRUNCATE ... FROM anon, authenticated`.
- Bổ sung kiểm thử TDD: mở rộng `FlywayMigrationTest` (18 tests, clean V1->V34, upgrade V27..V33->V34), mở rộng `RlsBehaviorVerificationTest` (chặn TRUNCATE và truy cập unprivileged trên `teacher_bank_accounts`), tạo mới `FinanceRlsFlowIntegrationTest` (xác nhận luồng ví, invoice, ledger và idempotency receipt chạy qua PostgreSQL superuser bypass RLS).
- Verification: 24/24 tests local Testcontainers pass; preflight Supabase read-only validate 34 migrations pass; apply V34 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu trên Supabase: baseline 32 rows được bảo toàn 100% (wallets: 20, ledger_entries: 4, invoices: 4, teacher_bank_accounts: 4; 4 bảng còn lại: 0) (0 data loss).

## 2026-09-14 — Triển khai RLS V33 Bảng users lên Supabase Production

- Thêm migration `V33__enable_rls_users.sql` kích hoạt Row Level Security default-deny cho bảng gốc `users`.
- Bổ sung test tích hợp xuyên bảng `UserRlsCrossTableFlowIntegrationTest` (luồng `users` -> `teacher_profiles` -> `pricing_packages` -> `invoices` -> `student_packages` -> `bookings` trên Testcontainers).
- Verification: clean/upgrade Flyway tests và test luồng pass 100%; apply V33 thành công trên Supabase production (`State = Success`), post-migrate validation pass.
- Đối soát dữ liệu: baseline 55 users được bảo toàn 100% (0 data loss).

## 2026-09-14 — Triển khai RLS V32 Bảng Backend-owned lên Supabase Production

- Thêm preflight read-only kiểm kê grants/role/RLS cho 9 bảng và migration `V32__enable_rls_public_tables.sql` theo mô hình default-deny, không policy public và không `FORCE ROW LEVEL SECURITY`.
- Cập nhật Flyway tests lên V32, thêm kiểm tra 9 bảng bật RLS và test local Backend/unprivileged role với cleanup sau mỗi test.
- Script Supabase yêu cầu xác nhận backup và quyền apply riêng của Đông trước khi chạy production. Chưa kết nối hoặc mutate Supabase production.
- Verification focused: Flyway `14/14`, RLS behavior `2/2` pass; Supabase read-only preflight V31→V32 pass, không apply. Backup/snapshot, production apply và Security Advisor verification còn chờ Đông; full backend sau V32 chưa hoàn tất do Maven test runner treo trên Windows.

## 2026-09-14 — Sửa lỗi 500 khi hoàn tất đăng ký Google OAuth

- Khắc phục lỗi `SerializationException` khi hoàn tất đăng ký OAuth (`POST /api/auth/oauth2/complete-registration`): chuyển payload lưu trong Redis từ `Map` sang `OAuthIdentity` record để `GenericJackson2JsonRedisSerializer` gắn `@class` và deserialize an toàn.
- Thêm unit test và edge case cho `OAuthAccountService` (5 ca kiểm thử).
- Thêm regression test cho Redis serialization của `OAuthIdentity` (2 ca kiểm thử).
- Thêm controller contract test cho `complete-registration` (2 ca kiểm thử).
- Verification: 13/13 OAuth focused tests pass, 10/10 Architecture guardrails pass. Không thay đổi schema DB.

## 2026-09-14 — Tối ưu Student dashboard và public cache

- Thêm `GET /api/student/dashboard` dùng một SQL projection, thay cho bảy request riêng ở Student dashboard; React Query giữ cache 30 giây và invalidate sau mutation liên quan.
- Public landing, catalog, teacher detail và ranking chuyển sang server-only fetch với Next ISR/Data Cache TTL 60–300 giây; ranking initial data không còn tải lại bằng hai `useEffect`.
- Verification: full backend Maven/Testcontainers `385/385` pass, frontend Docker `100/100` Jest suites và `255/255` tests pass, typecheck/lint/Next production build pass. Build xác nhận `/` ISR 300s, `/ranking` ISR 60s và `/teachers/[id]` on-demand ISR; Vercel Preview smoke chưa chạy. Host Windows Jest gặp `spawn EPERM`, không ảnh hưởng Docker verification.

## 2026-09-14 — Ổn định startup contract Backend

- Sửa regression CI của `FlywayMigrationTest`: giới hạn truy vấn constraint V31 vào schema hiện tại để các schema tạm không tạo kết quả trùng trên Linux runner.
- Giữ `cloud` làm profile mặc định và tự nạp `.env.cloud`; tách OAuth, Cloudinary và account-encryption theo local/test/cloud để từng môi trường có contract rõ ràng.
- Cloudinary cloud nhận ba credential rời; thêm preflight báo key thiếu mà không in secret; hợp nhất cấu hình test về một nguồn canonical.
- Đồng bộ Docker image/Compose profile, sửa healthcheck sang `/actuator/health`, sửa tên biến trong script test và thêm Compose startup smoke vào Backend CI.
- Verification: config contract `9/9`, full backend Maven/Testcontainers `374/374`, Compose syntax/image build/startup health đều pass. Cloud smoke chưa chạy vì còn thiếu `EDTECH_ACCOUNT_ENCRYPTION_KEY` trong `.env.cloud`.

## 2026-09-14 — Hoàn tất regression hardening Luna

- Khóa ownership package bằng query `id + student_id`, map unknown/foreign ID thành `404 RESOURCE_NOT_FOUND`; stale version dùng `409 CONCURRENT_MODIFICATION`.
- Complete refund/payout nhận multipart metadata + proof server-owned, kiểm tra MIME/size và fingerprint file cho idempotency; thêm cleanup khi rollback.
- Bổ sung audit atomic cho refund/payout/extension/platform settings và migration append-only V31 yêu cầu chứng từ ở terminal state.
- Verification: full backend Maven/Testcontainers `370/370` pass; Flyway clean V1→V31 và upgrade V30→V31 pass; Supabase preflight sạch, V31 apply/validate/info báo `Success`. Frontend typecheck pass; focused finance API Jest chạy qua `cmd` `7/7` pass; full Jest chưa chạy.

## 2026-09-13 — Apply V30 lên Supabase và khóa quy trình migration

- Thêm Flyway Maven plugin và `scripts/update-supabase-schema.ps1` với preflight version/checksum, guard migration phá hủy và post-migrate validation; secret chỉ đọc từ `.env.cloud`, không in ra log.
- Bổ sung rule Supabase vào `AGENTS.md`: code/docs-only không mutate database; migration tương thích ngược phải test, preflight, apply và ghi evidence.
- Flyway cloud preflight xác nhận V29; `V30__fix_data_constraints_and_locking` đã apply thành công, Supabase hiện ở V30 và validate 30 migrations pass.
- Flyway Testcontainers focused run trong lượt này bị blocked vì Docker daemon không có `dockerDesktopLinuxEngine`; không tính blocked run là pass.

## 2026-09-13 — Sửa regression contract/version sau review

- Khôi phục request contract cho teacher rejection và cho phép `ApiResponse<Void>` thành công đi qua Axios interceptor.
- Siết version bắt buộc, phân biệt đúng `CONCURRENT_MODIFICATION`, refetch dữ liệu stale, validate chặt `If-Match` và HTTP/HTTPS profile URL.
- Bổ sung regression/config tests và đồng bộ API contract/ERD; full backend `366/366`, frontend `97/97` suites và `252/252` tests cùng Next build đều pass.

## 2026-09-13 — Clean verification

- `mvn clean test` với Docker/Testcontainers thật pass `361/361`, không sửa migration V1–V30; docs check vẫn pass.

## 2026-09-12 — Verification optimistic-lock và frontend contract

- Hoàn tất narrowing `ApiResponse.data` nullable qua 
equireApiData`; frontend gửi version cho các public mutation và xử lý conflict không retry.
- Frontend full check pass (`97/97` suites, `248/248` tests, build); backend focused non-container `29/29` pass.
- Ghi rõ các test integration/Flyway bị chặn do Docker Desktop Linux engine chưa cung cấp socket; không coi compile/static check là integration pass.

## 2026-09-12 — Full verification sau khi Docker hoạt động

- Sửa test profile Cloudinary để integration context dùng credential giả chỉ trong test; giữ base/cloud fail-fast.
- Sửa bank-account delete kiểm tra ownership trước `If-Match`, cập nhật fixture trial/bank contract và xác minh IDOR.
- Thêm test validation biên cho Teacher Profile (`3/3`), kiểm tra giới hạn/nullability/XSS/URL theo contract.
- Siết architecture contract để kiểm tra generic `ApiResponse`, allowlist đúng từng webhook/health method, và loại hai error code token-expired không thể phát sinh.
- Full backend `361/361`, finance idempotency `2/2`, Flyway `12/12`, frontend `248/248` và docs check đều pass.

Các entry dưới đây ghi hành vi và bằng chứng quan trọng. Danh sách file đầy đủ nằm trong Git history.

## 2026-09-12 — Optimistic-lock contract và zero-trust storage

- Thêm version contract cho các public mutation của trial, learning, subject proposal và teacher bank account; stale version trả `409 CONCURRENT_MODIFICATION`.
- Thêm guardrail controller không phụ thuộc JPA entity, validation teacher profile và Cloudinary fail-fast ở base/cloud.

## 2026-09-12 — Fix lỗi bảo mật và phân quyền (Mục 5)

- Sửa lỗi Authorization bằng cách thiết lập lại `RequireRoleAspect.java` sử dụng cả `@within` và `@annotation` pointcut, giúp đảm bảo việc bảo vệ endpoint khi annotation `@RequireRole` được đánh dấu ở cấp độ Class.
- Xóa bỏ toàn bộ các default secrets cứng (hardcode) trong `application.yml` (như JWT, Encryption key, Google Client Secret) để bắt buộc ứng dụng phải crash (fail-fast) nếu DevOps quên cấu hình biến môi trường production.
- Áp dụng Zero-Trust Secrets policy và ghi nhận quyết định này tại ADR `0007-enforce-zero-trust-secrets.md`.
- Thêm cơ chế filter XSS bằng `@Pattern` Regex để ngăn ngừa chèn mã độc HTML tại các text request nhạy cảm (`CreateReviewRequest`, `UpdateTeacherProfileRequest`, `CreateSubjectProposalRequest`).
- Xác minh bằng cách chạy lại toàn bộ integration test, `SecurityIdorIntegrationTest` và Testcontainers: `350/350` tests pass thành công.

## 2026-09-12 — Sửa lỗi dữ liệu/migration

- Thêm `@Version` (Optimistic Locking) cho các entity: `Invoice`, `TrialRequest`, `Assignment`, `Submission`, `SubjectProposal`, `TeacherBankAccount` để chống race condition.
- Cập nhật `@SQLDelete` cho các entity có version để tương thích với soft-delete.
- Tạo file migration `V30__fix_data_constraints_and_locking.sql` chỉ chứa cấu trúc thêm cột `version` cho các bảng cần thiết, tuân thủ không sửa file `V1`-`V29`.
- Toàn bộ suite test 350/350 (bao gồm Testcontainers kiểm thử migration) đều pass.

## 2026-09-12 — Siết chặt architecture boundaries và module isolation

- Sửa lỗi Entity Leakage ở module Payment và Booking bằng cách sử dụng `InvoiceDetail` và `TrialRequestView` thay vì trả về domain entity từ Controller.
- Sửa lỗi Cross-Module Coupling bằng cách chuyển `AdminExtensionController`, `AdminPayoutController`, `AdminRefundController` sang module `finance`, và `TeacherSubjectProposalController` sang module `subject`.
- Đảm bảo Abstraction đúng chuẩn: Đóng gói SDK qua `PaymentGateway` port thay vì gọi thẳng SDK từ Service.
- Thêm ADR `0006-strict-layer-and-module-isolation.md` để ghi nhận quyết định cấm Controller truy cập Entity và cấm gọi chéo module.
- Các test backend liên quan đã pass.

## 2026-09-12 — Đồng bộ API contract Backend/Frontend

- Chuẩn hóa invoice DTO/request, availability `items` (giữ alias cũ), pricing package optimistic locking và error-code mapping.
- Siết architecture envelope/status contract, bổ sung parity check `ErrorCode` ↔ `ERROR_CODES.md`.
- Focused backend contract suite `26/26` pass; full backend Maven/Testcontainers `350/350` pass; frontend typecheck pass; Jest contract runner chưa xác minh do bị treo.

## 2026-09-11 — Finance state, contract và V29 hardening

- 2026-09-14: Residence/credential lifecycle hardening — credential version fields and 409 contract, audit hooks, MIME+extension validation, rollback/after-commit asset cleanup, and UI version propagation added. Full backend `mvn clean verify` 438/438 pass; admin pagination, Next tag revalidation and real Cloudinary upload smoke remain unverified.

- Đơn giản hóa transition Refund/Payout và siết invariant counter của StudentPackage; focused backend finance `20/20` pass.
- Đồng bộ payload/version và endpoint Admin Refund/Payout ở frontend; focused Jest finance/UI `6 suites, 22 tests` và TypeScript typecheck pass.
- Thêm V29 fail-fast preflight, `transferred_at`, active-request partial unique indexes và bảng receipt idempotency; Flyway Testcontainers clean V1→V29 và V27→V29 pass.
- V29 đã được preflight và áp dụng trên Supabase bằng đúng cloud Flyway user; không có dữ liệu xung đột.
- Thêm `FinanceCommandExecutor`, receipt cleanup có ShedLock và wiring `Idempotency-Key` cho Finance POST; focused executor `2/2`, frontend finance/admin `42/42`, full backend `348/348` pass.
- Full frontend Jest `97/97` suites, `248/248` tests và typecheck pass. Concurrency/rollback/reconciliation finance integration và load benchmark chưa xác minh; xem `STATUS.md`.

## 2026-09-11 — Re-check sau khi Docker Engine hoạt động

- Chạy lại vòng diagnose cho scheduler toggle, Teacher Search và CI hardening.
- Full backend suite với PostgreSQL/Redis Testcontainers: `341/341` pass; scheduler config `3/3`; Teacher Search repository `4/4`.
- Xác nhận container Testcontainers được Ryuk tự dọn sau test; cloud smoke và load benchmark vẫn chưa chạy.

## 2026-09-11 — Hardening cloud smoke và CI runtime

- Thêm `APP_SCHEDULING_ENABLED`, mặc định `true`, để chạy cloud servlet smoke mà không kích hoạt scheduled jobs; focused config test `3/3` pass.
- Thêm `scripts/smoke-teacher-search.ps1` kiểm tra health, accent/case/whitespace normalization và unrated-last ordering.
- Nâng GitHub Actions lên các major dùng Node.js 24 và thêm Dependabot cho GitHub Actions.
- Cloud smoke thật và cold/warm load test chưa xác minh; một lần full verify sau thay đổi kết thúc exit code `1`, lần test Testcontainers tiếp theo bị chặn do Docker Desktop mất socket.

## 2026-09-11 — Tối ưu teacher search/catalog

- Thêm V28 với immutable `unaccent` wrapper, partial GIN trigram indexes và package price index; ghi ADR về dictionary/reindex invariant.
- Refactor teacher search để tính min price một lần, dùng count query tối giản, giữ batch subject query và unrated-last semantics.
- Thêm cache search TTL 5 phút có normalization/page cap, giảm profile TTL còn 30 phút, Redis lỗi fallback PostgreSQL và cấu hình Hikari qua environment.
- Focused validation/cache/serialization và architecture guardrails `15/15` pass local. PostgreSQL 16 Testcontainers repository/EXPLAIN/index assertions `4/4`; Flyway clean schema và V27 → V28 upgrade `9/9` pass. Full backend suite `338/338` pass. Supabase Flyway connection validated 28 migrations and applied V28 successfully; cloud HTTP smoke stopped on the existing missing `ClientRegistrationRepository` OAuth boot configuration. Load test 100 concurrent chưa chạy.

## 2026-09-11 — Sửa cấu hình WebSocket local cho chat

- Thêm `NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws` vào overlay local frontend để STOMP kết nối trực tiếp backend thay vì fallback sang Next.js tại cổng `3000`.
- Bổ sung `frontend/.env.example` và cập nhật hướng dẫn frontend cho trường hợp Next.js và backend chạy khác origin/port.
- Smoke test thủ công: gửi/nhận tin nhắn thành công giữa Student và Teacher sau khi restart Next.js. Cảnh báo do browser extension không thuộc ứng dụng.

## 2026-09-11 — Sửa regression CI Student Journey

- Loại mapping trùng `GET /api/student/assignments/{id}` để Spring context khởi động được.
- Chuyển endpoint đọc StudentPackage về controller của module enrollment, loại dependency payment → enrollment domain.
- Sửa controller test dùng Spring Security context và assertion đúng response envelope.
- Focused suite: `9/9` pass. GitHub Actions full Maven/Testcontainers: `325/325` pass; Docker image build validation pass.

## 2026-09-11 — Đồng bộ context với working tree

- Cập nhật `STATUS.md` để liệt kê các endpoint, route và invariant đã có trong Student Journey.
- Xác nhận rõ phần đã triển khai với phần chưa nghiệm thu: full Testcontainers, Jest/Playwright, DB sạch/nâng cấp và provider thật.
- API contract tiếp tục là nguồn wire format; architecture docs là nguồn module/seam; archive không dùng để suy ra trạng thái hiện tại.

## 2026-09-11 — Chuẩn bị Student Journey và context docs

- Bổ sung luồng xác minh email, invoice purchase snapshot, trial requests, assignment detail/attachment, conversation, notification, parent contact, session reports và review state.
- Thêm migration V26/V27 và script kiểm thử Student Journey; các thay đổi code vẫn đang ở working tree.
- Đã xác nhận backend focused tests `36/36`, frontend typecheck/lint/build pass.
- Chưa xác minh full Testcontainers, Jest Student Journey và Playwright vì Docker chưa chạy.

## 2026-09-10 — Bảo vệ tài chính và vòng đời package

- Tập trung phép tính phân bổ tiền theo cumulative allocation.
- Refund giữ invariant Wallet/Ledger khi pending không đủ.
- Chặn bán package của teacher chưa approved hoặc đang hidden.
- Focused financial và package lifecycle tests đã được chạy trong đợt tương ứng.

## 2026-09-09 — Frontend architecture refactor

- Chuẩn hóa feature query boundaries, typed critical boundaries, UI component usage và public finance contracts.
- Cập nhật tài liệu frontend architecture và test lifecycle.

## Quy tắc ghi entry mới

Mỗi đợt thêm một entry gồm ngày, hành vi thay đổi, contract/schema/config liên quan, lệnh kiểm thử và phần chưa xác minh. Không ghi secret hoặc token.
- 2026-09-15: Hoàn tất verification và rollout booking settlement V41, đồng bộ E2E title assertion với metadata `Edtech Tutor Marketplace`. Backend Testcontainers `474/474` pass; frontend Docker check `101/101` suites, `264/264` tests, typecheck/lint/build pass; Playwright `4/4` pass. Supabase preflight V40→V41, apply và post-validate đều pass; schema V41 `Success`.
- 2026-09-16: Đồng bộ lại STATUS sau đối soát commit/push. V38–V41 đều đã nằm trong `origin/dev` (`c2de235`, `8308b62`, `5c4e1a1`); cập nhật các dòng stale còn ghi V41 Pending/V40 hiện tại và ghi nhận GitHub CI run `34943918942` fail ở Maven Testcontainers với log public chỉ có exit code tổng quát.
- 2026-09-14: Bổ sung xác nhận hai bên cho booking trả phí, settlement 24 giờ, escrow ledger V41 và admin reopen/release/retain actions; frontend đã có xác nhận học viên, khiếu nại gia sư và hiển thị trạng thái giữ tiền. Backend compile pass; full workflow, Supabase apply và frontend Jest chưa xác minh.
- 2026-09-14: Cloudinary flow audit — cloud key preflight pass without exposing values; real endpoint smoke remains unverified; teacher-document cleanup calls Cloudinary, generic attachment cleanup is missing.
- 2026-09-14: Local HTTP smoke attempt — backend health `200`, nhưng tài khoản test chưa có trong local DB (`0` user/profile), login `401`; không phát sinh file test trên Cloudinary.
- 2026-09-14: Cloud HTTP smoke attempt — Supabase/Flyway cloud startup pass và teacher login pass; upload bị `500` do `TeacherDocumentController` parse sai `principal.name` thành UUID trước khi gọi Cloudinary. Chưa phát sinh artifact.
- 2026-09-14: Teacher controllers fix — chuẩn hóa `TeacherDocumentController`, `TeacherProfileController`, `TeacherAvailabilityController`, `TeacherSubjectController`, `TeacherSubjectProposalController` dùng `@AuthenticationPrincipal AuthenticatedUser` thay `Principal.getName()`. Bổ sung 6 test classes với 23/23 unit tests pass (`TeacherDocumentControllerTest`, `TeacherProfileControllerTest`, `TeacherAvailabilityControllerTest`, `TeacherSubjectControllerTest`, `TeacherSubjectProposalControllerTest`, `TeacherDocumentServiceTest`) và architecture tests (10/10) pass.
- 2026-09-14: Supabase schema V32 — migration V32 (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY` trên 8 bảng backend-owned: 
efresh_tokens`, `teacher_documents`, `subject_proposals`, `subjects`, `pricing_packages`, `teacher_profiles`, `teacher_subjects`, `teacher_availabilities`) apply thành công lên Supabase production (`State = Success`, schema version 32); clean V1→V32 và upgrade tests V27→V32, V30→V32, V31→V32 pass (`FlywayMigrationTest` 16/16). ADR 0011 accepted and applied.
- 2026-09-14: Supabase schema V33 — migration V33 (`ALTER TABLE users ENABLE ROW LEVEL SECURITY;`) apply thành công lên Supabase production (`State = Success`, schema version 33); đối soát baseline `users` giữ nguyên `55` rows; clean V1→V33 và upgrade tests V27→V33, V30→V33, V31→V33, V32→V33 pass (`FlywayMigrationTest` 17/17); RLS behavior tests pass (`RlsBehaviorVerificationTest` 3/3); cross-table business flow test pass (`UserRlsCrossTableFlowIntegrationTest` 1/1). ADR 0011 updated.
- 2026-09-14: Regression hardening — hợp nhất request ID vào `RequestLoggingFilter`, expose `X-Request-Id` qua CORS, FE hiển thị mã tra cứu cho lỗi `5xx`, và chặn raw provider message trong ba nhánh tạo payment link của `InvoiceServiceImpl`; frontend Jest còn blocked bởi `spawn EPERM` trong môi trường hiện tại.
- 2026-09-14: API error messages — `BusinessException` sử dụng custom user message an toàn; Việt hóa các message còn sót ở auth, booking, teacher, learning, enrollment, attachment, bank account và invoice. Focused backend/AuthRegister tests pass; frontend Jest vẫn blocked bởi `spawn EPERM`.
- 2026-09-14: Hoàn thiện đợt message payment/webhook — loại bỏ các thông báo cấu hình/provider bằng tiếng Anh khỏi invoice và webhook, giữ log kỹ thuật ở server và contract ErrorCode ổn định; payment/webhook regression pass.
- 2026-09-14: Frontend verification — Jest full pass `100` suites / `256` tests; xác nhận lại sau khi chạy đúng từ `D:\EdTech\frontend`. Typecheck và lint pass; lỗi `spawn EPERM` trước đó được xác định là do môi trường/lệnh chạy, không phải test failure.

## 2026-10-16 — Viết lại `/teacher/credentials`

- Dựng lại page thành danh sách chứng chỉ theo trạng thái và vùng thêm/cập nhật độc lập, dùng form fieldset, hàng divider và action Button theo hệ thống. Giữ API, multipart/version, validation file, private proof blob, role/approval và handler nghiệp vụ; GET lỗi có thể tải lại.
- JSX layout AST **35/55 (63.6%)**; ảnh production before/after 375/768/1280. Playwright **5/5**, Jest **1 suite / 2 tests**, TypeScript, targeted ESLint và Next production build (64 static pages) PASS. Xác minh trạng thái loading/empty/error/retry, create/update (bao gồm sửa mục approved mà giữ file), delete, proof viewer, no overflow và mobile bottom-nav clearance. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/subjects`

- Thay giao diện môn học dạng card bằng danh sách chia hàng và panel catalog tìm/chọn riêng, một cột ở tablet/mobile. Giữ nguyên `TeacherSubject`/props, search debounce, query `keyword/size`, GET/POST/DELETE, duplicate warning, confirmation và mutation handlers; không thêm approval gate/status không có trong contract.
- JSX AST `page.tsx` **17/19 (89.5%)**, `SubjectSelector.tsx` **4/5 (80%)**; ảnh production before/after 375/768/1280, đã đối chiếu. Playwright **4/4**, Jest **1 suite / 3 tests**, TypeScript, targeted ESLint và Next production build (64 static pages) PASS. Baseline mobile overflow (538px content at 375px) đã được loại bỏ; mobile bottom-nav clearance pass. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/subject-proposals`

- Dựng lại page thành form đề xuất và history theo trạng thái; desktop hai vùng, mobile hiển thị mỗi đề xuất thành nhóm field có nhãn. Giữ nguyên GET `?size=100`, POST payload, validation, handler, toast và response update; không thêm update/filter/version/duplicate workflow không có trong source/API.
- JSX layout AST **17/19 (89.5%)**, full-file diff **73 xóa / 93 thêm**. Ảnh production trước/sau 375/768/1280 với cùng fixture đã xem. Playwright **3/3**, Jest **1 suite / 2 tests**, TypeScript, ESLint mục tiêu và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/packages`

- Thay card grid bằng danh sách hàng có tên/môn/mô tả, điều khoản, giá, trạng thái và action; mobile xếp thông tin theo thứ tự đọc. Giữ nguyên query/page size, route callbacks, profile approval gate, PATCH status/version, pagination, busy/error/409/refetch logic.
- JSX layout AST **14/22 (63.6%)**, full-file diff **89 xóa / 27 thêm**. Ảnh production trước/sau 375/768/1280 đã xem. Playwright **5/5**, Jest `PackageList.test.tsx` **1 suite / 3 tests**, TypeScript, targeted ESLint và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/packages/create`


- Dựng lại giao diện tạo gói thành form theo nhóm và summary live, responsive; giữ nguyên API subjects/create, giá trị mặc định, validation, payload version 0, callbacks và xử lý lỗi.
- JSX layout AST **17/20 (85.0%)**, diff **105 xóa / 32 thêm**; ảnh production before/after 375/768/1280 đã xem. Playwright **3/3**, Jest **1 suite / 3 tests**, TypeScript, ESLint mục tiêu và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/packages/[id]`


- Tách trạng thái đã lưu khỏi summary live khi chỉnh sửa gói, giữ GET theo ID, PUT với version API, conflict refetch/retry và điều hướng hiện có.
- Shared UI owner JSX layout AST **17/20 (85.0%)**, diff **105 xóa / 32 thêm**; ảnh production before/after 375/768/1280 đã xem. Playwright create/edit **5/5**, Jest **1 suite / 3 tests**, TypeScript, ESLint mục tiêu và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/availability`


- Dựng lại lịch rảnh thành bảng tuần có weekday/time labels trên desktop và nhóm ngày với nút giờ trên mobile; nút native hỗ trợ bàn phím, trạng thái chọn không phụ thuộc màu. Giữ GET/PUT contract, mapping, timezone và handler.
- JSX layout AST **9/12 (75.0%)**, diff **141 xóa / 52 thêm**; ảnh production before/after 375/768/1280 đã xem. Playwright **4/4**, Jest **1 suite / 3 tests**, TypeScript, ESLint mục tiêu và Next production build (64 static pages) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/students`

- Thay card grid thành danh sách quan hệ học viên/gói theo hàng, responsive; giữ GET pagination, URL học viên/bài tập, booking modal, active+remaining rule và logic hiện tại. Không thêm search/filter không có trên UI.
- JSX layout AST **12/19 (63.2%)**, diff **117 xóa / 12 thêm**; ảnh production before/after 375/768/1280 đã xem. Playwright **3/3**, TypeScript, ESLint mục tiêu và Next production build (64 static pages) PASS; Jest chưa chạy. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/students/[id]`

- Thay card packages bằng list rows có tên/môn, lượt còn/hết hạn và actions; giữ studentId filter, pagination, getLearners dùng cho assignment, modal và điều kiện booking hiện tại.
- JSX layout AST **14/19 (73.7%)**, diff **128 xóa / 12 thêm**; ảnh production before/after 375/768/1280 đã xem. E2E route **3/3**, shared directory regression **6/6**, TypeScript, ESLint mục tiêu và Next production build PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/bookings`

- Thay grid card bằng lịch dạy dạng danh sách có filter trạng thái/ngày và CTA chọn học viên; giữ role Teacher, filter/query/timezone/pagination, link detail và settlement labels. Student branch không đổi.
- JSX layout AST **14/22 (63.6%)**, diff **31 xóa / 111 thêm**; ảnh production before/after 375/768/1280 đã xem. Playwright **3/3**, TypeScript, ESLint mục tiêu và Next production build **64 route** PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/bookings/[id]`

- Dựng lại teacher booking detail thành facts/settlement/actions/report theo trạng thái; giữ handler, authorization, endpoints, version payloads, cancel/complete/dispute eligibility và Student view.
- JSX layout AST **10/15 (66.7%)**, view diff **13 xóa / 80 thêm**; shell **2 xóa / 1 thêm**. Ảnh production before/after 375/768/1280 đã xem. Playwright teacher **4/4** + Student regression **2/2**, TypeScript, ESLint mục tiêu và Next production build **64 route** PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/assignments`

- Thay card grid bằng danh sách bài đã giao theo hàng, với người nhận/môn, tiến độ nộp, hạn theo giờ Việt Nam và link chi tiết; giữ API, query, pagination, prop `role` và Student compatibility view.
- JSX layout AST **13/18 (72.2%)**; full-file diff **9 xóa / 157 thêm**. Ảnh before/after 375/768/1280 đã chụp và xem. Playwright production **3/3 PASS**; TypeScript, targeted ESLint, production Webpack build (64 routes) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/assignments/new`

- Tổ chức lại trang giao bài thành ba section người nhận, nội dung, hạn/trạng thái; thêm hướng dẫn empty state và CTA theo lựa chọn draft/publish. Giữ GET learner packages, eligibility filter, validation, POST payload, converter giờ Việt Nam, toast và redirect; không thêm attachment field chưa có trong contract.
- JSX layout AST **15/17 (88.2%)**; full-file diff **12 xóa / 183 thêm**. Ảnh before/after 375/768/1280 đã chụp và xem. Playwright production **4/4 PASS**; typecheck, targeted ESLint, production Webpack build (64 routes) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/assignments/[id]`

- Tách assignment facts/nội dung/submissions và phân biệt read-only vs editable theo status; giữ GET/PUT/publish/close, version, timezone, non-TEXT blocks, optimistic-conflict refetch và submission IDs.
- JSX layout AST **16/26 (61.5%)**; full-file diff **23 xóa / 216 thêm**. Ảnh before/after 375/768/1280 đã chụp và xem. Playwright production **6/6 PASS**; typecheck, ESLint mục tiêu, production Webpack build (64 routes) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/submissions/[id]`

- Dựng lại trang đọc và chấm bài theo status; giữ GET/key, POST score/feedback/version, validation, read-only cho DRAFT/GRADED, toast/refetch và 409. Không tạo tên học viên hay file URL không có trong DTO.
- JSX layout AST **12/16 (75.0%)**; full-file diff **13 deletions / 207 additions**. Ảnh before/after 375/768/1280 đã chụp và xem. Playwright production **6/6 PASS** (kể cả POST lỗi giữ form); TypeScript, ESLint mục tiêu, production Webpack build (64 routes) PASS. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Hoàn thiện `/teacher/messages`

- Dùng lại inbox/thread đã được thiết kế lại cho Student, gắn đúng counterpart “học viên” trong Teacher workspace và làm rõ empty state theo vai trò; sửa các landmark `main` lồng nhau. Giữ nguyên REST, STOMP, optimistic/retry, attachment, query và membership behavior.
- Shared UI AST: `ChatLayout` **17/22 (77.3%)**, `ChatThread` **24/37 (64.9%)**; `ChatPage` controller shell **5/11 (45.5%)** được báo riêng, không tính làm presentation owner. Ảnh Teacher before/after 375/768/1280 đã chụp và xem. Playwright production Teacher + Student regression **4/4 PASS**, gồm loading/empty/error/retry; chat Jest **2 suites / 5 tests**, TypeScript, ESLint mục tiêu và production Webpack build (64 routes) PASS. Fixture local không xác minh live WebSocket CONNECTED/echo. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Hoàn thiện `/teacher/notifications`

- Tách copy và điều hướng theo audience Teacher/Student trong shared `NotificationList`; Teacher thấy `Lịch dạy`/`Thu nhập`, và chỉ route cùng vai trò mới là liên kết. Bỏ landmark `main` lồng trong workspace; giữ nguyên API/filter/read behavior.
- Shared UI AST **21/30 subtree (70.0%)**. Ảnh before/after 375/768/1280 đã chụp và xem. Playwright production Teacher **1/1** + Student regression **2/2**; typecheck, targeted ESLint, production Webpack build (64 routes) và docs check PASS. Fixture cục bộ không xác minh live WebSocket push. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/stats`

- Thay lưới ba card bằng dải chỉ số và hàng dữ liệu từ đúng `TeacherStatsView`; sửa hiển thị tỷ lệ completion/conversion từ số thập phân sang phần trăm và tách rõ trạng thái chưa có đánh giá/xếp hạng.
- JSX layout AST **16/20 (80.0%)**; ảnh before/after 375/768/1280 đã chụp và xem. Playwright production **1/1 PASS** với giá trị, loading, empty, error/retry, focus, responsive và GET-only; typecheck, ESLint mục tiêu và production Webpack build (64 routes) PASS. Fixture cục bộ, không backend thật. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Viết lại `/teacher/wallet`

- Dựng lại số dư, ledger, lịch sử payout và tài khoản nhận theo reference T; trên mobile giữ đủ nhãn tab và đặt các bucket để giao dịch đầu tiên nằm trên thanh điều hướng. Giữ nguyên API, phân quyền, payload/version/idempotency, escrow semantics và số tài khoản đã mask.
- JSX layout AST đạt **66.7–97.8%** trên năm UI owner. Ảnh before/after 375/768/1280 đã chụp và xem. Playwright production **4/4**, finance Jest **4 suites / 15 tests**, typecheck, ESLint mục tiêu, Next Webpack build (64 routes) và docs checker PASS. Build server báo cảnh báo cấu hình standalone nhưng route phục vụ và E2E pass. Migration diff trống; không kết nối Supabase.
## 2026-10-16 — Hoàn thiện `/admin` và `/admin/dashboard`

- Người dùng duyệt reference X cho nhóm Admin. Dựng lại dashboard thành các hàng chỉ số, activity data và queue links thật; giữ GET `/api/admin/dashboard`, Admin guard và DTO. Cả hai URL dùng chung UI owner.
- JSX layout AST page **5/7 (71.4%)**, overview **57/57 (100%)**. Production screenshots before/after tại 375/768/1280; Playwright **2/2 PASS**, Admin finance Jest **1 suite / 5 tests**, typecheck, targeted ESLint và production build (64 routes) PASS. Fixture local, không backend thật. Build dùng distDir tạm vì Windows khóa `.next/standalone`; cấu hình đã khôi phục. Migration diff trống; không kết nối Supabase.

## 2026-10-16 — Hoàn thiện `/admin/teachers`

- Dựng lại queue xét duyệt thành danh sách theo trạng thái; chi tiết mở drawer đọc được thông tin, khu vực, video giới thiệu và tài liệu từ snapshot hiện có. Giữ Admin guard, query pagination/sort, approve note và reject reason payload.
- JSX layout AST page **83.3%**, queue **61.1%**, drawer **72.2%**. Production screenshots before/after 375/768/1280 đã chụp và xem. Playwright **5/5 PASS**, Jest **1 suite / 2 tests**, typecheck qua production build, targeted ESLint PASS. Fixture local. Migration diff trống; không kết nối Supabase.
