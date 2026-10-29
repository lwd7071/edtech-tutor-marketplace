# Route và trang

Danh sách dưới đây là baseline 70 route sản phẩm được kiểm kê từ `frontend/src/app/**/page.tsx` (route group Next không xuất hiện trong URL); trạng thái triển khai và nghiệm thu theo dõi riêng trong [PROGRESS.md](PROGRESS.md). `ThymeleafRouteRegistry` là inventory dùng cho parity tests, ghi expected role và route state; state `LIVE` chỉ xác nhận có Spring MVC mapping, không có nghĩa route đã nghiệm thu. Registry không thực thi quyền: Spring Security và service ownership checks vẫn là nguồn authorization. Contract nghiệp vụ/API tham chiếu [FRONTEND_SPEC.md](../architecture/FRONTEND_SPEC.md) và [API_CONTRACT.md](../architecture/API_CONTRACT.md).

Registry hiện có 70 route legacy, route workflow bổ sung `/teacher/booking-requests`, và `/sandbox/components` được phân loại dev-only. Test parity so sánh mọi route không-dev với mapping Spring thật. `/oauth2/callback` hiện là compatibility redirect; khi đổi OAuth flow cần giữ/đổi state registry cùng handler.

| Route | Role | Mục đích/ghi chú |
|---|---|---|
| `/` | Guest | Landing và lối vào marketplace |
| `/teachers` | Guest | **Spec chi tiết bên dưới; trang mẫu đầu tiên cần duyệt trước khi nhân rộng** |
| `/teachers/{id}` | Public | Hồ sơ, gói, availability, reviews |
| `/subjects` | Public | Duyệt/tìm môn |
| `/ranking` | Public | Xếp hạng gia sư |
| `/how-it-works` | Public | Giới thiệu quy trình |
| `/become-a-tutor` | Public | Đăng ký làm gia sư |
| `/terms` | Public | Điều khoản |
| `/privacy` | Public | Quyền riêng tư |
| `/support` | Public | Hỗ trợ |
| `/auth/login` | Guest | Đăng nhập |
| `/auth/register` | Guest | Đăng ký |
| `/auth/verify-email` | Guest | Xác minh email |
| `/auth/forgot-password` | Guest | Khởi tạo khôi phục |
| `/auth/reset-password` | Guest | Đặt lại mật khẩu |
| `/auth/oauth/role` | Guest/auth flow | Hoàn tất chọn role sau OAuth |
| `/oauth2/callback` | Auth flow | Callback OAuth hiện thuộc frontend; thay bằng server callback cần xác minh |
| `/forbidden` | Any | Không đủ quyền |
| `/student` | Student | Dashboard |
| `/student/packages` | Student | Các gói đã mua |
| `/student/packages/{id}` | Student | Chi tiết gói |
| `/teacher/booking-requests` | Teacher | Xem, xác nhận thời gian cuối cùng hoặc từ chối yêu cầu booking trả phí |
| `/student/checkout/{invoiceId}` | Student | Thanh toán invoice |
| `/student/payment-result/{invoiceId}` | Student | Kết quả; xác minh invoice từ backend |
| `/student/payments/callback` | Student/payment flow | Callback hiện hành; cần kiểm tra provider return config khi chuyển |
| `/student/bookings` | Student | Lịch buổi học |
| `/student/bookings/{id}` | Student | Chi tiết booking và action được phép |
| `/student/assignments` | Student | Bài tập |
| `/student/assignments/{id}` | Student | Chi tiết, nộp bài |
| `/student/session-reports` | Student | Báo cáo buổi học |
| `/student/messages` | Student | Hội thoại/chat |
| `/student/notifications` | Student | Thông báo |
| `/student/requests` | Student | Refund/extension |
| `/student/profile` | Student | Hồ sơ/liên hệ phụ huynh |
| `/teacher` | Teacher | Dashboard |
| `/teacher/profile` | Teacher | Hồ sơ và submit duyệt |
| `/teacher/documents` | Teacher | Giấy tờ xác minh riêng tư |
| `/teacher/credentials` | Teacher | Minh chứng năng lực |
| `/teacher/subjects` | Teacher | Môn dạy |
| `/teacher/subject-proposals` | Teacher | Đề xuất môn |
| `/teacher/packages` | Teacher | Quản lý gói |
| `/teacher/packages/create` | Teacher | Tạo gói |
| `/teacher/packages/{id}` | Teacher | Cập nhật gói |
| `/teacher/availability` | Teacher | Lịch rảnh |
| `/teacher/students` | Teacher | Học viên |
| `/teacher/students/{studentId}` | Teacher | Chi tiết học viên/gói |
| `/teacher/bookings` | Teacher | Lịch dạy |
| `/teacher/bookings/{id}` | Teacher | Chi tiết, hoàn tất/hủy/khiếu nại theo quyền |
| `/teacher/trial-requests` | Teacher | Danh sách học thử; chấp nhận/từ chối theo service |
| `/teacher/assignments` | Teacher | Quản lý bài tập |
| `/teacher/assignments/new` | Teacher | Giao bài |
| `/teacher/assignments/{id}` | Teacher | Chi tiết/cập nhật bài |
| `/teacher/submissions/{id}` | Teacher | Chấm bài |
| `/teacher/messages` | Teacher | Hội thoại/chat |
| `/teacher/notifications` | Teacher | Thông báo |
| `/teacher/stats` | Teacher | Thống kê |
| `/teacher/wallet` | Teacher | Ví và ledger |
| `/teacher/bank-accounts` | Teacher | Tài khoản ngân hàng |
| `/teacher/payouts` | Teacher | Yêu cầu rút tiền |
| `/admin` | Admin | Trang dashboard mặc định |
| `/admin/dashboard` | Admin | Dashboard |
| `/admin/teachers` | Admin | Duyệt hồ sơ gia sư |
| `/admin/credentials` | Admin | Duyệt credentials |
| `/admin/subjects` | Admin | Subject/proposal |
| `/admin/users` | Admin | User directory/moderation |
| `/admin/refunds` | Admin | Hoàn tiền |
| `/admin/extensions` | Admin | Gia hạn |
| `/admin/payouts` | Admin | Payout |
| `/admin/booking-settlements` | Admin | Settlement |
| `/admin/settings` | Admin | Cấu hình |
| `/admin/audit-logs` | Admin | Audit |
| `/sandbox/components` | Internal/dev | Không tính là route sản phẩm; quyết định giữ hay bỏ khi thay frontend |

## Mẫu đặc tả bắt buộc cho từng trang

Ghi rõ: mục tiêu; role; URL/query; layout desktop/mobile; dữ liệu và nguồn; actions + method/form target; điều kiện hiển thị theo quyền/trạng thái; rỗng/lỗi/validation; no-JS fallback; loading chỉ khi có request bất đồng bộ thật; accessibility; tiêu chí nghiệm thu.

### `/teachers` — Tìm gia sư (trang mẫu đầu tiên)

- **Mục tiêu/role:** giúp Guest tìm và so sánh gia sư đang hiển thị; không yêu cầu đăng nhập.
- **URL/query:** `/teachers`; GET query theo `TeacherSearchParams` trong API contract. Giữ filter trên URL để refresh/chia sẻ/back-forward.
- **Desktop:** header public; tiêu đề; thanh keyword; hai cột filter trái và kết quả phải; sort/count từ response; tutor cards và pagination.
- **Mobile:** keyword đầu trang; filter trong `<details>`; sort trước kết quả; card một cột; pagination dạng link Trước/Sau và số trang có nhãn.
- **Dữ liệu:** `GET /api/public/teachers`; subject/location options từ public subject/location endpoints. Chỉ render field public DTO, không bịa dữ liệu khi thiếu.
- **Card:** avatar/placeholder + alt; tên; verified badge khi đúng; môn; kinh nghiệm; mode; starting price; rating/review count. Link tới `/teachers/{id}`. Không có CTA đặt lịch trực tiếp.
- **Hành động:** GET form tìm/lọc, reset về `/teachers`, pagination/sort bằng URL.
- **States:** SSR kết quả; empty hướng dẫn nới filter; API error có link thử lại giữ query; lỗi filter inline; không render fixture giả. Loading/skeleton chỉ khi htmx enhancement đang request.
- **No-JS:** filter submit GET và reload; link card/pagination hoạt động bình thường.
- **A11y/acceptance:** label, fieldset/legend; focus rõ; keyboard dùng filter; query hợp lệ được giữ sau submit; test 320/640/768/1024/1280px và JavaScript tắt.

### `/teachers/{id}` — Hồ sơ gia sư công khai

- **Mục tiêu/role:** giúp Guest so sánh hồ sơ, gói đang mở, lịch rảnh và phản hồi đã công khai; người dùng đã đăng nhập vẫn xem cùng dữ liệu public.
- **Dữ liệu:** profile, gói, lịch rảnh và reviews lấy từ các capability public trong [API_CONTRACT.md](../architecture/API_CONTRACT.md), gọi qua `TeacherMarketplaceService`, `PricingPackageService`, `TeacherFacade` và `PublicReviewFacade`; không lấy private proof URL hoặc dữ liệu tài khoản.
- **Desktop:** link quay lại danh sách; phần đầu hồ sơ có avatar, tên, khu vực, kinh nghiệm, rating, credential badge và hình thức; nội dung giới thiệu/gói ở cột chính, lịch rảnh/đánh giá ở cột phụ.
- **Mobile:** một cột; avatar và tên nằm đầu trang; gói, lịch và reviews xếp theo thứ tự; CTA nằm trong card gói và vẫn là link HTML.
- **Hành động/no-JS:** quay lại `/teachers`; Guest chọn gói được chuyển tới `/auth/login` với `continueTo` chỉ giữ public profile UUID và `packageId` hợp lệ; sau đăng nhập Student quay lại đúng hồ sơ/gói. Student gửi native POST+CSRF tới `/student/invoices`, nhận PRG tới checkout. Student đang đăng nhập có thể gửi yêu cầu học thử bằng form HTML POST+CSRF tới `/student/trials/requests`; thành công PRG tới `/student/requests?tab=trial`, lỗi nghiệp vụ quay lại hồ sơ với thông tin đã nhập được giữ. Yêu cầu học thử chưa xác nhận lịch; Teacher phản hồi tại `/teacher/trial-requests`. Mọi dữ liệu SSR; không yêu cầu JavaScript.
- **Trạng thái:** profile không tồn tại trả 404 page; lỗi tải profile trả unavailable page; packages, availability và reviews có empty/error state riêng để một vùng lỗi không che phần khác; thiếu avatar dùng illustration, thiếu bio/rating không giả thành số liệu thật.
- **A11y/acceptance:** heading theo thứ tự, link có tên rõ, alt ảnh, ratings có tên truy cập, text dài wrap ở 320px; kiểm tra JS bật/tắt và không tràn ngang.

### `/` — Trang chủ công khai

- **Mục tiêu/role:** giúp Guest bắt đầu tìm gia sư hoặc khám phá môn học; không yêu cầu đăng nhập.
- **Dữ liệu:** tối đa chín môn đang hoạt động và sáu gia sư nổi bật lấy qua `SubjectFacade`/`TeacherMarketplaceService`; hồ sơ gia sư có ảnh thật hoặc chữ cái đầu khi thiếu ảnh. Không hiển thị số đếm giả.
- **Desktop:** header public; hero có tiêu đề, mô tả, form tìm GET và minh họa sách; danh sách môn; ba bước giới thiệu quy trình; gia sư nổi bật dạng thẻ; CTA dành cho gia sư; footer. **Mobile:** các phần xếp thành một cột, form tìm kiếm xếp dọc.
- **Hành động/no-JS:** nhập từ khóa rồi GET `/teachers`; mở môn để tìm gia sư theo subject; link tới trang nội dung/auth. Form và link được SSR.
- **Trạng thái:** dữ liệu môn và gia sư lỗi/rỗng độc lập, mỗi vùng có copy và link khôi phục rõ; không dùng skeleton cho SSR.
- **A11y/acceptance:** một h1, thứ bậc heading, nhãn tìm kiếm, skip link, focus rõ; kiểm tra JS bật/tắt, reduced motion và viewport 320–1440px.

### `/subjects` — Danh mục môn học

- **Mục tiêu/role:** Guest duyệt môn, tìm theo tên/cấp học rồi mở danh sách gia sư tương ứng.
- **Dữ liệu:** `SubjectService` trả trang môn public; contract gốc tại [API_CONTRACT.md](../architecture/API_CONTRACT.md), không gọi HTTP loopback.
- **Desktop:** breadcrumb, tiêu đề/giới thiệu ngắn, form tìm và lọc, số kết quả, danh sách hàng môn học với mô tả/cấp học và link “Xem gia sư”, phân trang. **Mobile:** form xếp dọc; danh sách mỗi môn thành một cột; CTA xuống dưới phần mô tả.
- **Hành động/no-JS:** form GET `/subjects` giữ keyword/cấp học trên URL; phân trang dùng link GET; “Xem gia sư” mở `/teachers` kèm subject ID; xóa lọc quay về `/subjects`.
- **Trạng thái:** validation query không hợp lệ hiển thị ngay cạnh khu vực filter; kết quả rỗng chỉ hướng người dùng đổi tiêu chí; lỗi service có thông báo và link thử lại giữ filter; không render môn giả.
- **A11y/acceptance:** label cho search/select, alert cho filter error, breadcrumb có nhãn, link hành động có tên rõ, bàn phím/focus và reflow 320–1440px; kiểm tra JS bật/tắt.

### `/ranking` — Bảng xếp hạng gia sư

- **Mục tiêu/role:** giúp Guest xem danh sách xếp hạng công khai, lọc theo môn và mở hồ sơ gia sư.
- **URL/query:** `GET /ranking`; query `subjectId`, `page`, `size`. Form lọc và phân trang là GET; URL giữ môn/trang để tải lại hoặc chia sẻ.
- **Desktop:** public shell; breadcrumb, tiêu đề/mô tả; bộ lọc môn trong một hàng; danh sách xếp hạng với tên, đoạn giới thiệu, số buổi hoàn tất, điểm và link hồ sơ.
- **Mobile:** public header; form lọc xếp thành cột; dòng xếp hạng chuyển thành card một cột; nội dung tiếng Việt dài được phép xuống dòng.
- **Dữ liệu:** `TeacherStatsService.getGlobalRanking` và `SubjectFacade.getActiveSubjects`; DTO/API gốc tham chiếu [API_CONTRACT.md](../architecture/API_CONTRACT.md) và các controller/service nguồn. Không tự tính hoặc bịa điểm/thứ hạng.
- **Hành động:** lọc môn bằng form GET; reset về `/ranking`; phân trang dùng link GET giữ bộ lọc. Mỗi gia sư liên kết `/teachers/{id}`.
- **States:** có dữ liệu; không có hồ sơ đủ điều kiện; `subjectId` sai định dạng có recovery; lỗi service có thông báo và link thử lại giữ query. Trang SSR không có skeleton/loading giả.
- **No-JS:** lọc, reset, phân trang và điều hướng hồ sơ dùng form/link HTML.
- **A11y/acceptance:** label gắn select, thứ tự heading, breadcrumb có tên, trạng thái lỗi dùng alert, focus bàn phím, không tràn ở 320–1440px; kiểm tra JavaScript bật/tắt.

### `/how-it-works` — Cách hoạt động

- **Mục tiêu/role:** Guest hiểu hành trình từ tìm gia sư tới theo dõi buổi học; đây là trang thông tin, không tạo booking/payment.
- **Nội dung:** ba bước theo thứ tự: tìm và so sánh gia sư; chọn gửi yêu cầu học thử hoặc mua gói/thanh toán; trao đổi để gia sư xác nhận buổi học và theo dõi việc học. Yêu cầu học thử chưa phải booking xác nhận. Chi tiết nghiệp vụ tham chiếu [CONTEXT.md](../../CONTEXT.md) và [API_CONTRACT.md](../architecture/API_CONTRACT.md).
- **Desktop:** public shell; tiêu đề và mô tả ngắn; một danh sách tuần tự dạng editorial, mỗi bước có số thứ tự mang ý nghĩa trình tự, tiêu đề và đoạn giải thích; cuối trang có CTA tìm gia sư và trở thành gia sư.
- **Mobile:** cùng thứ tự, một cột, tiêu đề và mô tả được wrap tự nhiên; CTA xếp dọc khi thiếu chiều rộng.
- **Hành động/no-JS:** liên kết thường tới `/teachers` và `/become-a-tutor`; không cần JavaScript hoặc request bất đồng bộ.
- **Trạng thái:** nội dung tĩnh SSR; không có loading/skeleton. Trạng thái 404 do route handler chung xử lý.
- **A11y/acceptance:** một `h1`, ba mục trong `ol`, link có tên rõ, focus nhìn thấy, đọc được khi zoom/reflow 320–1440px, không phụ thuộc animation.

### `/become-a-tutor` — Trở thành gia sư

- **Mục tiêu/role:** Guest hiểu các bước tạo và gửi hồ sơ gia sư; CTA bắt đầu đăng ký với role Teacher.
- **Nội dung:** chuẩn bị hồ sơ, môn/kinh nghiệm và giấy tờ; gửi duyệt và theo dõi phản hồi; chỉ sau khi được duyệt mới tạo gói/lịch dạy. Không hứa thu nhập, số học viên hay thời gian duyệt.
- **Desktop/mobile:** dùng public shell và cùng cấu trúc guide tuần tự của `/how-it-works`; desktop giữ heading rộng và ba hàng đọc theo thứ tự, mobile chuyển một cột với CTA toàn chiều rộng.
- **Dữ liệu/hành động:** trang thông tin SSR, không cần API. CTA đăng ký là link `/auth/register?role=TEACHER`; CTA hồ sơ tiếp tục tới `/teacher/profile` sau khi có auth session.
- **States/no-JS:** nội dung tĩnh, không loading/skeleton; link thường hoạt động khi JavaScript tắt. Auth route/session guard chịu trách nhiệm redirect sau khi được triển khai.
- **A11y/acceptance:** một `h1`, ba bước trong `ol`, heading theo cấp, CTA có tên rõ, focus nhìn thấy, nội dung dài không tràn ở 320–1440px.

### `/teacher/booking-requests` — Yêu cầu đặt buổi trả phí

- **Mục tiêu/role:** Teacher xem yêu cầu từ Student dùng package đã mua và phản hồi bằng thời điểm cuối cùng/hình thức học hoặc từ chối có lý do. Nguồn quy tắc là [ADR 0017](../adr/0017-paid-booking-request-workflow.md), domain invariant tại [CONTEXT.md](../../CONTEXT.md).
- **Desktop/mobile:** Teacher workspace shell; mỗi request hiển thị tên học viên, môn, giờ mong muốn, ghi chú và trạng thái. Hai form hành động xếp hai cột desktop, một cột trên mobile; filter status và pagination đứng trên danh sách.
- **Dữ liệu/actions:** SSR list theo Teacher principal; GET filter giữ query; form accept yêu cầu thời điểm cuối cùng, mode và logistics tương ứng; form reject yêu cầu lý do. Mọi mutation dùng POST+CSRF+PRG.
- **States/no-JS:** populated, empty, invalid filter và service unavailable; native GET/POST forms, không cần JavaScript. Accept tạo lịch và reserve buổi theo snapshot đã mua; request pending không giữ lượt.
- **A11y/acceptance:** label từng field, error/status announcement, keyboard focus, test 320–1440px, role/session, CSRF/no-JS và screenshots browser.

### `/student/packages/{id}` — Gửi yêu cầu đặt buổi

- **Bổ sung luồng:** với package `ACTIVE` còn lượt, Student gửi preferred start time và ghi chú; trang hiển thị request pending và form hủy. Đây là yêu cầu, không phải lịch đã xác nhận. Teacher chọn giờ cuối cùng khi accept.
- **No-JS/states:** form native POST+CSRF+PRG; pending request hiện trạng thái và thao tác hủy; package không active/hết lượt không có form; nghiệp vụ hoặc thời gian sai hiển thị flash message an toàn.
- **Dữ liệu/quyền:** package và booking request chỉ đọc theo Student principal; không tin package ID do form gửi thay quyền sở hữu.

### `/terms` — Điều khoản sử dụng

- **Mục tiêu/role:** Guest đọc điều kiện sử dụng hiện có của sản phẩm.
- **Nội dung:** giữ nguyên nội dung tiếng Việt từ route nguồn, gồm ghi chú rõ đây là bản tối thiểu MVP cần cập nhật trước phát hành chính thức; không tự thêm điều khoản pháp lý.
- **Layout:** public shell; breadcrumb; một h1; nội dung đọc một cột với measure giới hạn; liên kết chính sách riêng tư.
- **No-JS/states:** trang tĩnh SSR, link thường; không loading/skeleton hoặc API.
- **A11y/acceptance:** heading theo cấp, đoạn văn có measure dễ đọc, link có tên, keyboard focus, reflow 320–1440px.

### `/privacy` — Chính sách riêng tư

- **Mục tiêu/role:** Guest đọc các nhóm dữ liệu và cách dùng đã được nêu trong route nguồn.
- **Nội dung:** giữ nguyên ba mục về tài khoản/học tập, liên hệ phụ huynh và dữ liệu xác thực/thanh toán; không mở rộng thành cam kết pháp lý mới.
- **Layout:** public shell; breadcrumb; h1; ba mục đọc liên tiếp trong một cột; liên kết điều khoản sử dụng.
- **No-JS/states:** nội dung tĩnh SSR, link thường; không loading/skeleton hoặc API.
- **A11y/acceptance:** một h1, mỗi mục là section với h2, link có tên, keyboard focus, reflow 320–1440px.

### `/support` — Hỗ trợ

- **Mục tiêu/role:** Guest tìm đúng khu vực trong tài khoản để kiểm tra trạng thái; route hiện không gửi ticket hoặc email.
- **Nội dung:** giữ ba chủ đề từ route nguồn: yêu cầu học thử/hoàn tiền/gia hạn, thông báo/kết quả xử lý, trao đổi với gia sư. Các đích `/student/...` cần Student session.
- **Layout:** public shell; tiêu đề/mô tả; danh sách liên kết thường dùng dạng list; ghi chú mã giao dịch/yêu cầu ở cuối.
- **Hành động/no-JS:** link GET tới các route Student; không có form POST/API, không tự tạo địa chỉ liên hệ.
- **States:** nội dung tĩnh SSR, không loading/skeleton. Khi Auth route hoàn tất, protected destinations chuyển login và giữ return URL theo [AUTH_AND_SECURITY.md](AUTH_AND_SECURITY.md).
- **A11y/acceptance:** một h1, danh sách `ul/li`, link có tên, focus bàn phím, reflow 320–1440px.

### Yêu cầu chi tiết các route còn lại

### `/auth/login` — Đăng nhập

- **Mục tiêu/role:** Guest tạo HTML session; nếu đã có session, return path vẫn phải được kiểm tra theo role.
- **Dữ liệu/hành động:** email + password qua POST form, CSRF; `SessionService` là nguồn xác thực. Return path chỉ được chấp nhận khi là local path thuộc workspace role vừa đăng nhập.
- **Layout/states:** trang auth một cột, email/password, lỗi field và lỗi xác thực tổng quát; không giữ mật khẩu khi render lại. Thành công redirect theo saved request an toàn hoặc workspace mặc định.
- **No-JS/a11y:** HTML POST/redirect; label/autocomplete, summary lỗi có alert semantics, focus nhìn thấy. Logout là POST CSRF ở shell sau khi đã đăng nhập.

### `/auth/register` — Tạo tài khoản

- **Mục tiêu/role:** Guest tạo tài khoản Student hoặc Teacher; không cho chọn Admin. Nguồn nghiệp vụ/schema ở [API_CONTRACT.md](../architecture/API_CONTRACT.md), service tại `backend/src/main/java/com/edtech/platform/auth/service/RegistrationService.java`.
- **Dữ liệu/hành động:** tên, email, mật khẩu, role; thông tin phụ huynh tùy chọn cho Student. POST form có CSRF; thành công PRG sang xác minh email. Không tự đăng nhập trước khi xác minh.
- **Layout/states:** form một cột; lỗi validation/domain cạnh form; mật khẩu không được điền lại; duplicate email dùng thông báo nghiệp vụ hiện hành.
- **No-JS/a11y:** role radio và submit thường; mọi field có label/hướng dẫn; không phụ thuộc JavaScript để ẩn trường phụ huynh.

### `/auth/verify-email`, `/auth/forgot-password`, `/auth/reset-password`

- **Mục tiêu/role:** Guest kích hoạt email hoặc phục hồi mật khẩu theo one-time workflow hiện có. Chi tiết payload/error tham chiếu [API_CONTRACT.md](../architecture/API_CONTRACT.md) và [ERROR_CODES.md](../architecture/ERROR_CODES.md), không định nghĩa lại ở đây.
- **Hành động:** verify/reset link đưa token vào server session rồi redirect sang URL sạch; thao tác tiêu thụ token chỉ qua POST có CSRF. Resend và forgot luôn trả nội dung chung để không tiết lộ email tồn tại.
- **States/no-JS:** xác nhận token hợp lệ, token hết hạn/sai, email đã gửi, validation và rate limit; form HTML POST/PRG, không poll/skeleton. Không render token hoặc mật khẩu trong response.
- **A11y:** h1 riêng cho từng nhiệm vụ, alert/status có role phù hợp, label/email/password autocomplete và focus rõ.

### `/auth/oauth/role` và `/oauth2/callback`

- **OAuth entry action:** nút Google dùng link thường tới `/auth/oauth/google`; backend đánh dấu flow HTML trong session, giữ callback provider tại Spring Security và exchange/login phía server. Chi tiết OAuth flow gốc tham chiếu [API_CONTRACT.md](../architecture/API_CONTRACT.md) và [AUTH_AND_SECURITY.md](AUTH_AND_SECURITY.md).
- **Role selection:** chỉ hiển thị lựa chọn Student/Teacher; POST có CSRF, registration token ở session server, success tạo HTML session và điều hướng workspace. Thiếu/hết hạn token có đường về login; không có token hoặc code trong query/HTML.
- **Callback compatibility:** `/oauth2/callback` cũ chỉ redirect về login; provider callback thật đi vào Spring `/login/oauth2/code/google`. Không dùng client callback cũ để nhận credential khi Thymeleaf là UI hoạt động.
- **No-JS/accessibility:** OAuth là link điều hướng; role là radio HTML trong form thường, có label/legend, lỗi dạng alert. Google thật/staging cần smoke trước cutover.

Với mỗi route, bổ sung mục theo mẫu trên trước khi implement: mục tiêu/role; URL/query; desktop/mobile; model/data sources; actions + method/target; role/state gating; empty/error/validation; no-JS fallback; accessibility; acceptance. Trang có `{id}` cần đặc tả detail/actions riêng. Dùng [API_INTEGRATION.md](API_INTEGRATION.md) làm mapping và ghi rõ flow chưa xác minh.

## Đặc tả triển khai chung theo nhóm route

Các dòng dưới xác định page contract mức nhóm. Trước khi code một page, thêm tiêu chí riêng cho từng route động/biến thể vào phần này hoặc link issue/spec của route. Shared shell không thay thế role guard ở server.

### Public/auth

| Route | Dữ liệu và hành động chính | Layout/states/no-JS |
|---|---|---|
| `/` | Public subjects/teachers; link sang tìm kiếm, đăng nhập, become tutor | Public shell; SSR sections; fallback nội dung khi data unavailable, không dùng số liệu giả |
| `/teachers` | Search params + page `TeacherCard`; GET filter/sort/page | Chi tiết tại mục trên; page SSR và URL query chuẩn |
| `/teachers/{id}` | Public teacher detail, packages, availability, reviews | Header hồ sơ, nội dung, gói/CTA; loading không áp dụng cho SSR; not-found/error render trang |
| `/subjects` | Paged subject list/search/education level; link sang teachers có subject filter | Filter GET + grid/list SSR; empty/error/retry |
| `/ranking` | Public teacher ranking/filter subject/page | Bảng/list responsive; paging GET; empty/error |
| `/how-it-works`, `/become-a-tutor`, `/terms`, `/privacy`, `/support` | Nội dung sản phẩm/hỗ trợ | Static/read layout; CTA/link chuẩn; support form chỉ khi endpoint được xác minh |
| `/auth/login`, `/auth/register`, `/auth/verify-email`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/oauth/role`, `/oauth2/callback` | Auth service/OAuth flow; chi tiết endpoint ở API contract | Form POST/PRG; errors inline; callback OAuth phụ thuộc ADR auth và backend route |
| `/forbidden` | Không cần dữ liệu cá nhân | SSR 403 explanation; safe links về home/tìm gia sư; không dựa vào browser history hay JavaScript |

### Student

| Route | Dữ liệu/hành động chính | Layout/states/no-JS |
|---|---|---|
| `/student`, `/student/packages`, `/student/packages/{id}` | Dashboard, package list/detail; package action theo trạng thái | Workspace shell; SSR counters/cards; empty CTA; server ownership |
| `/student/checkout/{invoiceId}`, `/student/payment-result/{invoiceId}`, `/student/payments/callback` | Owned invoice read, provider checkout link, trạng thái hóa đơn mới nhất | Không tin query redirect; GET refresh; pending/paid/expired/cancelled/error; callback provider phải xác minh |
| `/student/bookings` | Danh sách lịch học; lọc trạng thái và phân trang | GET filter/pagination; empty/error; chỉ CTA tìm gia sư, không tạo booking trực tiếp |
| `/student/bookings/{id}` | Chi tiết lịch thuộc học viên; xác nhận chỉ khi service cho phép | Ownership 404; detail SSR; POST+CSRF+PRG confirm; trạng thái/meeting details từ read service |
| `/student/assignments`, `/student/assignments/{id}` | Bài tập, bài nộp và nhận xét | Filter/pagination GET; detail; form multipart POST/CSRF; empty/error/due/grading states |
| `/student/session-reports` | Báo cáo sau buổi học | List/detail; empty/error; dữ liệu SSR từ read service |
| `/student/profile` | Account identity và liên hệ phụ huynh | Profile summary từ principal + identity facade; parent contact GET và POST+CSRF+PRG; lỗi field inline; không cần JS |
| `/student/requests` | Trial request list, refund and extension history; refund/extension start from an owned package detail | GET tabs/pagination; finance POST+CSRF+PRG; trial list only; package context loaded with ownership; validation/error and empty states |
| `/student/messages` | Conversations/messages | SSR inbox/detail, GET pagination, POST/CSRF send/read/open; reload fallback; no live updates yet; empty/unavailable |
| `/student/notifications`, `/teacher/notifications` | Notifications list, filters, read actions | SSR list; GET filters/pagination; POST + CSRF mark-read actions; empty/unavailable; only link destinations that already have an SSR handler |

#### `/student/messages` — Tin nhắn

- **Role/session:** chỉ Student có session; owner ID lấy từ principal. GET hội thoại theo ID và lịch sử tin nhắn đều qua `ChatQueryService`, kiểm tra membership ở backend. Trang có thể mở hội thoại sâu bằng ID kể cả khi nó không nằm trong trang danh sách hiện tại.
- **Nguồn dữ liệu/hành động:** danh sách và detail đọc trực tiếp từ `ChatQueryService`; mở hội thoại dùng `ConversationService`; gửi text/tệp dùng `ChatCommandService` và `AttachmentService`; đánh dấu đã đọc dùng command service. Không gọi REST loopback. Contract gốc tham chiếu [API_CONTRACT.md](../architecture/API_CONTRACT.md); nguồn code tại `backend/src/main/java/com/edtech/platform/communication/web/StudentMessagesPageController.java`, `ChatQueryService.java`, `ChatCommandService.java` và `ConversationService.java`.
- **Desktop:** workspace shell; danh sách hội thoại bên trái, hội thoại đang chọn ở bên phải; mỗi dòng gồm tên, preview, thời gian và unread count; vùng nội dung có lịch sử, link tải lại tin cũ, trạng thái tải/lỗi và form soạn.
- **Mobile:** khi chưa chọn, danh sách ở trên phần chào; khi mở hội thoại, ẩn danh sách để đưa nội dung và form lên đầu, có link thường quay lại danh sách. Không phụ thuộc JS.
- **Hành động/no-JS:** chọn hội thoại bằng GET link; gửi tin nhắn hoặc tệp, đánh dấu đã đọc và mở từ hồ sơ gia sư bằng POST+CSRF+PRG. Sau gửi, trang tải lại lịch sử; cập nhật realtime/STOMP chưa được tích hợp vào HTML này. Upload hint phản ánh giới hạn service hiện tại; contract/source là căn cứ cuối cùng.
- **States:** inbox có dữ liệu/rỗng/lỗi tải; thread có dữ liệu/rỗng/lỗi hoặc hội thoại không tồn tại/không thuộc học viên; gửi thành công/lỗi domain/lỗi mạng; tên người dùng, tin nhắn hoặc attachment thiếu dữ liệu được hiển thị an toàn. SSR đồng bộ nên không có skeleton/animation.
- **A11y/acceptance:** heading/landmark có nhãn, mọi form field có label, focus rõ, unread count có tên truy cập, nội dung tin nhắn được escape, attachment chỉ thành liên kết khi URL HTTP(S) hợp lệ. Mobile có đường quay về danh sách; kiểm tra trình duyệt/screenshot và contrast vẫn cần trước khi route chuyển `Hoàn tất`.

#### `/teacher/stats` — Thống kê hoạt động

- **Role/session:** Teacher session; truy vấn nhận ID từ principal và gọi `TeacherStatsService` trong process. REST contract gốc: [API_CONTRACT.md](../architecture/API_CONTRACT.md); code `backend/src/main/java/com/edtech/platform/ranking/web/TeacherStatsPageController.java`.
- **Nội dung:** số buổi hoàn thành, điểm đánh giá và count, thứ hạng, tỷ lệ hoàn thành, số buổi thử, tỷ lệ chuyển đổi, thời điểm tổng hợp. Chỉ hiển thị các trường có trong `TeacherStatsView`; không tự suy ra xu hướng.
- **Desktop/mobile:** header workspace gia sư, tiêu đề cùng thời điểm cập nhật; ba chỉ số đầu theo hàng, hoạt động giảng dạy thành danh sách tên–giá trị. Mobile xếp các số liệu thành một cột, không cần JS.
- **States/no-JS:** record thiếu render empty; lỗi đọc có link GET thử lại; thành công render SSR. Không có skeleton/animation vì không có tải async.
- **A11y:** section/heading có nhãn, danh sách tỷ lệ là `dl`, số liệu có tabular numerals, tỷ lệ có tên văn bản, focus rõ. Cần kiểm tra screenshot/viewport thật trước khi hoàn tất.

#### `/teacher/messages` — Tin nhắn gia sư

- **Role/session:** chỉ Teacher có session; principal ID là teacher account ID. Danh sách, hội thoại theo ID và lịch sử đều đọc qua `ChatQueryService` để thực hiện membership check; không phụ thuộc hội thoại nằm ở trang list hiện tại.
- **Nguồn dữ liệu/hành động:** đọc từ `ChatQueryService`; gửi text/tệp và đánh dấu đã đọc qua `ChatCommandService`/`AttachmentService`, gọi trực tiếp trong process. Không có thao tác tạo hội thoại từ Teacher page này. Contract gốc: [API_CONTRACT.md](../architecture/API_CONTRACT.md); code: `backend/src/main/java/com/edtech/platform/communication/web/TeacherMessagesPageController.java`, `ChatQueryService.java`, `ChatCommandService.java`.
- **Desktop/mobile:** dùng cùng cấu trúc inbox/thread responsive như `/student/messages`; mobile ẩn danh sách khi mở thread và có link quay lại thường. Header có logout POST + CSRF; nav chỉ tới route Teacher đã triển khai.
- **Hành động/no-JS:** chọn thread bằng GET, gửi file/text và đánh dấu đã đọc bằng POST+CSRF+PRG. Reload lại history sau submit; không có STOMP live update trong SSR.
- **States/a11y:** data, empty, unavailable, membership 404, send/read status, focus/labels, safe attachment URL, unread accessible label; không có skeleton/animation trong SSR. Browser/responsive/a11y thực tế cần xác minh trước khi hoàn tất.

#### `/student/notifications` and `/teacher/notifications` — Thông báo

- **Role/session:** tương ứng Student hoặc Teacher; server derives owner ID from session principal. Role mismatch is denied by Spring Security and rechecked by the page controller.
- **Nguồn dữ liệu:** `NotificationService`; không gọi REST loopback. Contract REST gốc nằm trong [API_CONTRACT.md](../architecture/API_CONTRACT.md), code nguồn tại `backend/src/main/java/com/edtech/platform/communication/service/NotificationService.java` và `backend/src/main/java/com/edtech/platform/communication/web/NotificationPageController.java`.
- **Desktop:** workspace header, tiêu đề và mô tả theo role, nút đánh dấu tất cả, hàng lọc ngang, danh sách thông báo dạng dòng có nội dung/ngày/trạng thái và phân trang.
- **Mobile:** header và hành động xếp dòng, filter cuộn ngang, nội dung thông báo xếp cột; không yêu cầu hover hoặc JavaScript.
- **Hành động/no-JS:** filter và pagination là link GET giữ query; đánh dấu từng thông báo hoặc tất cả dùng form POST có CSRF và PRG. Destination chỉ là link khi route đích đã tồn tại; các route SSR chưa triển khai chỉ hiển thị nội dung thông báo.
- **States:** danh sách có dữ liệu, empty theo filter, bộ lọc sai được đưa về tất cả, lỗi tải có link thử lại, mutation có flash success/error sau redirect. SSR đồng bộ nên không có skeleton hoặc animation tải.
- **A11y/acceptance:** heading một cấp, nav filter có nhãn, liên kết/hành động keyboard reachable, focus rõ, trạng thái unread có chữ, nội dung dài wrap, thao tác thay đổi trạng thái có thông báo role=status/alert. Cần browser responsive/screenshot trước khi route hoàn thành.

#### `/student/requests` — Yêu cầu của học viên

- **Role/session:** Student session; owner ID lấy từ principal. Package context chỉ đọc qua `StudentPackageReadService.detail(packageId, studentId)`, không tin ID/version gửi từ trình duyệt.
- **Nguồn dữ liệu:** list services `TrialRequestService`, `RefundService`, `ExtensionService`; command dùng service trong process và finance idempotency executor. DTO/API gốc ở [API_CONTRACT.md](../architecture/API_CONTRACT.md); code nguồn tại `backend/src/main/java/com/edtech/platform/finance/web/StudentRequestsPageController.java`.
- **Desktop:** workspace shell, heading, vùng chọn package khi đi từ trang gói, biểu mẫu hoàn tiền/gia hạn theo trạng thái package, tab dạng link GET và danh sách lịch sử theo loại yêu cầu.
- **Mobile:** form một cột; danh sách chuyển thành các dòng nội dung/status; nav tab cuộn ngang, không yêu cầu JavaScript.
- **Hành động/no-JS:** tabs/pagination giữ query; form POST+CSRF dùng PRG. Tạo refund/extension phải qua finance command executor, giữ version và idempotency key. Link chọn gói chỉ xuất hiện ở tab refund/extension khi chưa chọn package. Học thử được liệt kê tại đây; Student bắt đầu yêu cầu từ form trên `/teachers/{id}` để gắn với gia sư và môn đã chọn.
- **States:** list có dữ liệu, empty theo tab, lỗi đọc có retry, lỗi nghiệp vụ hiển thị thông báo đã chuẩn hóa, success sau redirect. Form chỉ hiện theo package context; backend vẫn quyết định eligibility.
- **A11y/acceptance:** label đầy đủ, fieldset/legend cho tài khoản hoàn tiền, browser validation bổ sung backend validation, focus rõ, lỗi/success dùng alert/status, không render lại hoặc flash raw số tài khoản. MockMvc/security pass; browser, responsive, session smoke và finance staging chưa xác minh.

#### `/student/bookings` and `/student/bookings/{id}` — Lịch học

- **Role/ownership:** chỉ Student session; list/detail lấy user ID từ principal. Detail không thuộc học viên trả trang 404 chung, không lộ việc booking có tồn tại cho tài khoản khác.
- **Nguồn dữ liệu:** `BookingReadService.list/detail`; xác nhận dùng `BookingService.confirmByStudent`. Contract REST gốc tại [API_CONTRACT.md](../architecture/API_CONTRACT.md); adapter ở `backend/src/main/java/com/edtech/platform/booking/web/StudentBookingPageController.java`.
- **Danh sách:** GET filter trạng thái và pagination; hiển thị thời gian, gia sư, môn, loại học thử và hình thức. Link chi tiết hoạt động với ID của hàng hiện tại. Empty state dẫn tìm gia sư.
- **Chi tiết:** thời gian, gia sư, môn, hình thức, đường tham gia/địa chỉ nếu có, lý do hủy và report nếu service trả. Meeting link chỉ clickable khi là URL HTTP(S) hợp lệ. Nút xác nhận chỉ xuất hiện theo `canConfirm` của read service; form gửi version hiện hành, service kiểm tra lại điều kiện.
- **No-JS/states:** filter/pagination dùng GET; xác nhận dùng POST+CSRF+PRG. Có list empty/error, invalid filter, detail unavailable, 404 ownership, action success/error. SSR đồng bộ nên không có skeleton/animation. Không tạo form đặt buổi trực tiếp từ Student; capability đó chưa được xác minh.
- **A11y/acceptance:** trạng thái có chữ, label rõ, focus visible, text dài wrap, layout một cột trên mobile. MockMvc/security kiểm tra list/detail/ownership/confirm/CSRF; browser responsive, screenshot và session smoke còn cần thực hiện.

#### `/student/profile` — Hồ sơ cá nhân

- **Role/session:** Student session; user ID chỉ lấy từ `AuthenticatedUser`, role được kiểm tra ở Spring Security và page controller.
- **Nguồn dữ liệu:** thông tin tài khoản đọc qua `IdentityFacade`; thông tin phụ huynh đọc/cập nhật qua `ParentContactService`. REST contract gốc và field thuộc [API_CONTRACT.md](../architecture/API_CONTRACT.md); nguồn code tại `backend/src/main/java/com/edtech/platform/auth/web/StudentProfilePageController.java`, `backend/src/main/java/com/edtech/platform/auth/facade/IdentityFacade.java`, `backend/src/main/java/com/edtech/platform/auth/service/ParentContactService.java`. Không gọi REST loopback.
- **Desktop/mobile:** phần đầu hiển thị tên/email/ảnh đại diện nếu identity snapshot có; tiếp theo là form liên hệ phụ huynh. Form hai cột trên desktop, một cột trên mobile; nút lưu nằm sau các field và rộng toàn hàng trên mobile.
- **Hành động/no-JS:** GET hiển thị dữ liệu đã lưu; form POST có CSRF, validation server-side cho input HTML và PRG khi thành công. Không cần JavaScript; checkbox là điều khiển HTML native.
- **States:** thông tin tải được; form liên hệ rỗng; lỗi tải có link tải lại; lỗi kiểm tra gắn với field và giữ input trong response; lỗi lưu có thông báo thử lại; thành công hiển thị notice sau redirect. SSR đồng bộ nên không dùng skeleton/animation.
- **A11y/acceptance:** heading theo cấp, label liên kết input, thông báo lỗi có `aria-describedby`, focus bàn phím rõ, text dài wrap; test GET/POST/validation/role/CSRF. Quy tắc nghiệp vụ và ý nghĩa notify chỉ tham chiếu [API_CONTRACT.md](../architecture/API_CONTRACT.md), không nhân bản tại đây. Browser responsive, ảnh chụp và session smoke còn cần xác minh trước khi hoàn thành.

#### `/student` — Tổng quan học tập

- **Role/session:** Student đã xác thực; route dùng session HTML theo [AUTH_AND_SECURITY.md](AUTH_AND_SECURITY.md). Quyền route do Spring Security kiểm tra; không nhận `studentId` từ query hay form.
- **Nguồn dữ liệu:** `StudentDashboardService` với `AuthenticatedUser.id()`; payload nguồn gốc được mô tả tại [API_CONTRACT.md](../architecture/API_CONTRACT.md), không gọi REST loopback từ controller HTML.
- **Desktop:** workspace navigation phía trên; tiêu đề; hai vùng tóm tắt (lịch kế tiếp, lượt học còn lại); danh sách việc cần theo dõi và một CTA khám phá gia sư ở vùng riêng.
- **Mobile:** navigation cuộn ngang bằng touch/keyboard; hai vùng tóm tắt và panel xếp một cột, không cần hover.
- **Hành động/no-JS:** liên kết HTML thường tới các khu vực Student và tìm gia sư; thử lại bằng GET `/student`. Không có client state hoặc JavaScript bắt buộc.
- **States:** summary có dữ liệu; không có lịch kế tiếp; các bộ đếm bằng 0; lỗi tải summary có alert và link thử lại. Dashboard là request SSR đồng bộ nên không có loading skeleton.
- **A11y/acceptance:** một h1, skip link dùng từ header chung, điều hướng có label/current page, icon trang trí ẩn khỏi accessibility tree, focus rõ và nội dung không tràn với tên/ngày dài. Route chỉ hoàn thành sau MockMvc, kiểm tra role thật và browser desktop/mobile.

#### `/student/packages` và `/student/packages/{id}` — Gói học đã mua

- **Role/ownership:** Student session; service truy vấn theo `AuthenticatedUser.id()`. ID trên URL không thay thế kiểm tra ownership. Dữ liệu đọc từ `StudentPackageReadService`, không gọi REST loopback.
- **Danh sách:** thẻ gói hiển thị môn, tên gói snapshot, gia sư, trạng thái, số buổi còn/đã lên lịch/đã học/đã hoàn tiền, hạn dùng và giá đã thanh toán; filter status và pagination dùng form/link GET, giữ query trong URL.
- **Chi tiết:** cùng dữ liệu sở hữu ở trên, thêm tổng buổi và tiến độ đã học, ngày bắt đầu, lý do khóa nếu có. Không hiển thị hành động đặt buổi Student do capability đó chưa xác minh; không dựng mô tả/thời lượng không có trong view hiện hành.
- **States:** danh sách rỗng có CTA tìm gia sư; filter rỗng có đường xem tất cả; filter bất thường báo lỗi và quay về kết quả tất cả; lỗi đọc có link thử lại; ID không thuộc học viên trả 404; detail read lỗi có thông báo riêng.
- **Responsive/a11y/no-JS:** filter là GET select + submit thường; card là list semantically ordered; trạng thái dùng text và màu; layout một cột trên mobile, long names wrap; không skeleton vì SSR đồng bộ, không có JS bắt buộc.
- **Acceptance:** MockMvc list/detail/empty/error/ownership delegation; browser desktop/mobile, no-JS filter/pagination, 320–1440px và session-role gate trước khi đánh dấu hoàn thành.

#### `/student/assignments` và `/student/assignments/{id}` — Bài tập

- **Role/ownership:** Student session; read và command dùng `StudentAssignmentService` với principal ID. Assignment không thuộc học viên trả 404.
- **Danh sách:** lọc TODO/SUBMITTED/GRADED và pagination bằng query GET; kích thước trang giữ trong danh sách giới hạn; thời hạn hiển thị theo giờ Việt Nam.
- **Chi tiết/nộp:** đề và tệp đính kèm; trạng thái nháp/đã nộp/đã chấm, điểm và phản hồi; form freeform giữ version, lưu nháp/nộp theo POST + CSRF, có thể tải tệp qua `AttachmentService` trong cùng form multipart.
- **Giới hạn đã biết:** `SYSTEM_QUIZ` chưa có schema UI/chấm bài được xác minh; trang chỉ cho xem đề và không đưa ra form freeform sai loại. Xem [API_CONTRACT.md](../architecture/API_CONTRACT.md) và theo dõi gap trước khi đánh dấu route hoàn tất.
- **States/no-JS:** empty, unavailable, invalid filter, closed, overdue, graded, lỗi version, file quá cỡ/loại không hỗ trợ; PRG giữ câu trả lời khi lỗi. GET filter/pagination và form multipart không cần JavaScript. Không có skeleton vì SSR đồng bộ.
- **Acceptance:** MockMvc read/empty/error/404, command version/status/file binding và security session/CSRF; browser no-JS upload, file limits, keyboard, desktop/mobile, text overflow và 320–1440px.

#### `/student/session-reports` — Báo cáo buổi học

- **Role/ownership:** Student session; read model gọi `BookingReadService.findStudentSessionReports` bằng ID từ principal. Service lấy booking cho cả trang theo batch.
- **Nội dung:** thời gian buổi học, nội dung, nhận xét, việc cần làm tiếp và liên kết bản ghi nếu URL hợp lệ; thời gian hiển thị theo giờ Việt Nam. Không link sang booking detail cho tới khi route đích hoạt động.
- **States/no-JS:** danh sách rỗng, lỗi đọc có link thử lại, phân trang bằng GET. SSR đồng bộ nên không có skeleton; không cần JavaScript.
- **Acceptance:** MockMvc data/empty/error/record URL safety/pagination và role gate; browser desktop/mobile, keyboard, no-JS và 320–1440px trước khi hoàn tất.

#### `/student/checkout/{invoiceId}` và `/student/payment-result/{invoiceId}` — Thanh toán hóa đơn

- **Role/ownership:** Student session; mọi lần đọc gọi `InvoiceReadFacade.findOwned(invoiceId, AuthenticatedUser.id())`. Hóa đơn không tồn tại hoặc không thuộc học viên trả 404.
- **Checkout/return:** mã hóa đơn, số tiền, hạn thanh toán và liên kết checkout được lấy từ invoice do backend sở hữu; production link được allowlist theo PayOS, profile `test` chỉ chấp nhận fake link `pay.test`. Hành động dùng liên kết thường, không cần JavaScript. Callback chỉ chuyển tiếp `invoiceId` hợp lệ tới trang kết quả; GET kiểm tra lại hóa đơn; hóa đơn PAID chuyển sang trang kết quả.
- **Kết quả:** chỉ trạng thái snapshot đọc mới nhất từ backend mới quyết định thành công/thất bại/chờ; query redirect không thể xác nhận thanh toán. Pending có thể quay lại checkout hoặc tải lại trạng thái.
- **States/no-JS:** pending, paid, expired, cancelled, trạng thái chưa xác định và read unavailable; tất cả hành động là link GET thường, không có polling/skeleton vì SSR đồng bộ. Không render payload QR thô như ảnh và không phụ thuộc JavaScript.
- **Acceptance:** MockMvc cho ownership 404, pending/paid, retry khi lỗi và query không lấn át trạng thái nguồn; browser desktop/mobile, keyboard, no-JS và external URL allowlist trước khi hoàn tất.

### Teacher

#### `/teacher` — Tổng quan công việc gia sư

- **Mục tiêu/role:** trang làm việc đầu tiên sau đăng nhập Teacher; nêu việc cần xử lý trước. Chỉ Teacher session; mọi read dùng ID từ principal. Response `no-store`.
- **Dữ liệu:** trạng thái hồ sơ từ `TeacherProfileService`; yêu cầu đặt buổi đang `PENDING` từ `BookingRequestService`; buổi học sắp tới từ `BookingReadService` (chỉ booking có `endTime` sau thời điểm hiện tại, sắp theo giờ bắt đầu); yêu cầu học thử đang `PENDING` từ `TrialRequestService`; bài tập gần đây có bài nộp từ `AssignmentReadService`; số dư khả dụng/giữ lại từ `WalletQueryService`. Không gọi REST loopback và không hiển thị metric giả.
- **Bố cục:** desktop đặt trạng thái hồ sơ đầu, tiếp theo là toàn chiều rộng “Yêu cầu đặt buổi đang chờ”; sau đó hai cột “Buổi học sắp tới” và “Yêu cầu học thử đang chờ”; bên dưới là lịch, bài nộp cần xem và ví. Mobile xếp một cột theo cùng thứ tự ưu tiên. Request đặt buổi hiển thị học viên, môn, giờ mong muốn và link mở danh sách đang chờ; card buổi học hiển thị giờ Việt Nam/hình thức và link mở chi tiết.
- **Hành động/no-JS:** dùng link thường tới hồ sơ, danh sách yêu cầu đặt buổi, lịch dạy, yêu cầu học thử, bài tập và ví. Không có mutation trên dashboard, không cần JavaScript, polling, skeleton hay animation.
- **Trạng thái/lỗi:** mỗi vùng dữ liệu có lỗi riêng và link mở trang tương ứng để thử lại; empty state riêng cho yêu cầu đặt buổi, lịch và yêu cầu thử; lý do từ chối hồ sơ chỉ hiện nếu service cung cấp. Text dài tiếng Việt được wrap; không render stack trace.
- **Acceptance:** MockMvc/service/security regression cần kiểm tra query pending và trạng thái empty/error của yêu cầu đặt buổi cùng các vùng hiện có; browser screenshot, responsive/a11y thực tế, session-cookie smoke và runtime query count theo dõi riêng.

#### `/teacher/profile` — Hồ sơ gia sư

- **Role/ownership:** Teacher session; profile service luôn nhận ID của authenticated principal. Không nhận `userId` từ URL hoặc form.
- **Dữ liệu/hành động:** đọc và cập nhật nội dung giảng dạy, ngôn ngữ, kinh nghiệm, hình thức online/offline và video; nơi ở cập nhật riêng theo province/ward và không bị khóa khi profile đang chờ duyệt; submit xét duyệt chỉ hiện cho trạng thái phù hợp từ DTO. Nguồn contract: [API_CONTRACT.md](../architecture/API_CONTRACT.md), quyền riêng tư: [ADR 0012](../adr/0012-residence-and-credential-privacy.md); adapter gọi service trong process, không REST loopback. `locationAddress` không có field update trong request hiện hành nên không xuất hiện như form giả.
- **Layout:** trạng thái xét duyệt nổi bật đầu trang; panel nội dung giảng dạy; panel khu vực; CTA gửi duyệt khi được phép. Desktop dùng hai cột cho field ngắn, mobile chuyển một cột.
- **No-JS:** cập nhật/submit là form POST+CSRF+PRG. Chọn tỉnh/thành là form GET reload để tải danh sách xã/phường, sau đó lưu lựa chọn bằng form POST; không phụ thuộc JavaScript.
- **States/a11y:** profile tải lỗi có retry; domain/field errors giữ nội dung và gắn lỗi inline; submit success/error có status; trường bị khóa trong lúc chờ duyệt; danh sách địa phương lỗi có hướng tải lại. Label, fieldset/legend, focus, status text và long text wrap. MockMvc/security đã có; browser screenshot/responsive/session smoke chưa xác minh.

#### `/teacher/documents` — Giấy tờ xác minh danh tính

- **Role/ownership:** chỉ Teacher session; service dùng owner ID từ principal. Metadata là riêng tư; response HTML đặt `Cache-Control: no-store`.
- **Dữ liệu/hành động:** SSR đọc `TeacherDocumentService`; nhận upload giấy tờ định danh dạng multipart và xóa tài liệu chưa được xác minh bằng form POST+CSRF+PRG. Tài liệu đã xác minh không có nút xóa. Trang chỉ hiển thị tên, trạng thái, dung lượng và thời gian xác minh khi service cung cấp; không render `secureUrl`, không có link tải cho Teacher vì hiện không có route tải riêng. Chi tiết nguồn tại [API_CONTRACT.md](../architecture/API_CONTRACT.md) và [ADR 0012](../adr/0012-residence-and-credential-privacy.md).
- **Bố cục/no-JS:** danh sách tài liệu ở panel đầu, trạng thái riêng theo từng mục; form tải một tệp ở panel dưới. Không dùng JavaScript hoặc kéo-thả bắt buộc. Lỗi/quá cỡ/định dạng đều trả thông báo tiếng Việt qua HTML.
- **Giới hạn nguồn:** chỉ PDF, PNG hoặc JPG/JPEG, tối đa 10 MB theo `TeacherDocumentService`; kiểm tra MIME dựa trên bytes ở service. Không suy đoán progress/rejection reason hoặc upload timestamp vì DTO không cung cấp.
- **A11y/states:** empty, danh sách, load error/retry, upload success/error, delete success/error; label/accept/help text liên kết với file input, trạng thái có chữ, text dài wrap và focus rõ. MockMvc/service/security tests có; browser, upload provider, session-cookie và responsive/a11y thực tế chưa xác minh.

#### `/teacher/credentials` — Minh chứng năng lực

- **Role/ownership:** chỉ Teacher session; list, mutation và proof proxy đều dùng ID principal và `TeacherCredentialService` ownership check. Trang metadata đặt `Cache-Control: no-store`; proof trả bytes cùng `no-store` và `nosniff`, không render URL proof của DTO.
- **Dữ liệu/hành động:** tối đa 10 mục; create bắt buộc label + proof, update yêu cầu version và có thể giữ file cũ, delete gửi version hiện tại. Sửa label hoặc thay proof đưa mục về trạng thái chờ duyệt; mục đã duyệt được cảnh báo trước khi sửa. Mục bị từ chối hiển thị lý do từ service. Nội dung file mở qua route same-origin có kiểm tra quyền, không gọi REST loopback.
- **Bố cục/no-JS:** danh sách/trạng thái bên trái và form thêm/chỉnh sửa bên phải desktop; mobile xếp dọc. Chỉnh sửa mở bằng link GET có `edit` ID; create/update dùng multipart POST+CSRF+PRG; xóa có disclosure HTML xác nhận rồi POST+CSRF+version.
- **Giới hạn/trạng thái:** chỉ JPG/PNG/PDF, tối đa 5 MiB và kiểm tra MIME/extension ở service; UI không thay thế validation server. Có empty, pending/approved/rejected, form validation, conflict/version error, load error/retry, upload/delete success/error và file view error. Không hiển thị dữ liệu giả.
- **A11y/acceptance:** heading/label/status text, feedback `role=alert/status`, focus visible, keyboard-only, rejection text wrap; form file là native. MockMvc/service/security tests có; browser screenshot, responsive/a11y thực tế, Cloudinary smoke và session-cookie chưa xác minh.

#### `/teacher/subjects` — Môn dạy

- **Role/ownership:** chỉ Teacher session; danh sách và mutations gọi `TeacherSubjectService` theo ID principal. Danh mục chọn môn dùng `SubjectFacade.getActiveSubjects()`; không gọi REST loopback.
- **Dữ liệu/hành động:** hiển thị môn, cấp học, mô tả cấp độ/kinh nghiệm và trạng thái active từ `TeacherSubjectView`; thêm môn đang hoạt động với hai mô tả tùy chọn; gỡ môn qua service hiện có. Không tự giới hạn độ dài các mô tả khi API/DTO không quy định.
- **Bố cục/no-JS:** desktop có danh sách môn và form thêm thành hai cột; tablet/mobile xếp form trước danh sách, thẻ chuyển thành một cột ở màn hẹp. Form thêm và gỡ dùng POST+CSRF+PRG; gỡ có disclosure HTML xác nhận, không cần JavaScript.
- **Trạng thái/a11y:** empty list, catalog/list error riêng, không còn môn để thêm, domain error và success notice. Nếu danh sách môn không tải được, vô hiệu hóa mutations để tránh thao tác dựa trên dữ liệu cũ. Select có label, inline error, `aria-describedby`, focus tự về select lỗi; trạng thái còn có text. MockMvc/service/security tests có; browser screenshot, responsive/a11y thực tế và session-cookie chưa xác minh.

#### `/teacher/subject-proposals` — Đề xuất môn học

- **Role/ownership:** chỉ Teacher session; list/create gọi `SubjectProposalService` với ID principal. Teacher chỉ tạo và đọc đề xuất của mình; không có thao tác sửa, xóa hoặc duyệt cho Teacher.
- **Dữ liệu/hành động:** GET filter theo `ALL|PENDING|APPROVED|REJECTED` và pagination; view dùng tên, cấp học, mô tả, trạng thái, phản hồi, thời gian duyệt và subject ID nếu đã được tạo. Form POST tạo đề xuất từ tên môn, cấp học, mô tả tùy chọn; validation theo request DTO/API contract.
- **Bố cục/no-JS:** desktop chia history và form đề xuất; mobile xếp form trước history. Filter GET và link phân trang giữ status trên URL. Tạo dùng POST+CSRF+PRG khi thành công; lỗi validation/domain render lại form và giữ dữ liệu, không cần JavaScript.
- **Trạng thái/a11y:** empty history, lỗi tải và retry, status không hợp lệ fallback về tất cả, phản hồi xét duyệt, success/domain notice và inline validation. Field lỗi có label, `aria-describedby`, focus đầu tiên; status không chỉ biểu thị bằng màu. MockMvc/service/security tests có; browser screenshot, responsive/a11y thực tế và session-cookie chưa xác minh.

| Route | Dữ liệu/hành động chính | Layout/states/no-JS |
|---|---|---|
| `/teacher`, `/teacher/stats`, `/teacher/wallet`, `/teacher/bank-accounts`, `/teacher/payouts` | Dashboard/stats, wallet+ledger, bank accounts, payout | Workspace tables/cards; explicit empty/errors; versioned/idempotent financial POST forms |
| `/teacher/profile`, `/teacher/documents`, `/teacher/credentials`, `/teacher/subjects`, `/teacher/subject-proposals` | Profile, private documents, credential proofs, teaching subjects/proposals | Forms/lists; document/credential limits from services; no proof URL; approval-state guidance |
| `/teacher/packages`, `/teacher/packages/create`, `/teacher/packages/{id}` | Pricing package CRUD/status | List/form/detail; validation; teacher approval gating from backend |
| `/teacher/availability`, `/teacher/students`, `/teacher/students/{studentId}` | Availability replacement, learner/package views | Week/list/detail; GET filters; warn/error; avoid save after failed read |
| `/teacher/bookings`, `/teacher/bookings/{id}`, `/teacher/trial-requests` | Booking list/detail/actions; trial queue/accept/reject | GET filters/paging; POST+CSRF+PRG actions; state/version enforced by services |
| `/teacher/assignments`, `/teacher/assignments/new`, `/teacher/assignments/{id}`, `/teacher/submissions/{id}` | Assignment create/update/publish/close; submission grading | Editor/forms/detail; drafts/errors; file upload constraints from source |
| `/teacher/messages`, `/teacher/notifications` | Conversation/chat, notifications | Same no-JS communication pattern as Student; role/member enforced server-side |

#### `/teacher/assignments`, `/teacher/assignments/new`, `/teacher/assignments/{id}`, `/teacher/submissions/{id}` — Bài tập

- **Role/ownership:** Teacher session; assignment list/read và commands dùng ID principal qua `AssignmentReadService`/`TeacherAssignmentService`. Submission lookup và chấm bài kiểm tra assignment thuộc Teacher. Không thêm API loopback.
- **Danh sách/tạo:** danh sách SSR pagination GET; mỗi dòng có học viên, môn, hạn, trạng thái bài và trạng thái bài nộp. Tạo chỉ chọn học viên có quan hệ học tập (danh sách chọn phân trang GET) và môn đang gắn với Teacher; title, đề tự luận, hạn Việt Nam tùy chọn và multipart đính kèm. Chỉ `FREEFORM` được giao; không dựng editor trắc nghiệm chưa được service hỗ trợ.
- **Chi tiết/hành động:** draft có form sửa, publish và version; published có thao tác close; closed chỉ đọc. Chấm bài hiển thị câu trả lời/attachment; chỉ submission `SUBMITTED` có form điểm 0–10 và nhận xét. Mọi mutation dùng POST+CSRF+PRG; service là nơi quyết định ownership, state, version, hạn và grade validity.
- **Bố cục/no-JS:** cards/list trên desktop và mobile, form một cột, bài nộp là trang riêng; pagination dùng link GET; upload dùng multipart form chuẩn; state/error và action notice SSR.
- **A11y/security:** native labels/controls, focus outline, error anchor, text status; response `no-store`; file URL chỉ clickable khi HTTP(S). Accept upload theo `AttachmentService` (PDF, Office, ZIP, JPEG/PNG/GIF/WebP; 20 MB mỗi tệp). Kiểm thử MockMvc/security/service có; browser screenshot, viewport/a11y thực tế, runtime query count và session-cookie smoke cần xác minh.

#### `/teacher/wallet`, `/teacher/bank-accounts`, `/teacher/payouts` — Ví và rút tiền

- **Role/ownership:** chỉ Teacher session. Các trang gọi `WalletQueryService`, `BankAccountService`, `PayoutService` và `FinanceCommandExecutor` trong process; services nhận ID từ principal và giữ kiểm tra ownership/invariant. HTML đặt `Cache-Control: no-store`.
- **Ví:** số dư khả dụng, chờ quyết toán, đang rút và escrow đang giữ được trình bày riêng; ledger dùng GET pagination. Lỗi tải ví và lỗi ledger độc lập, empty/error có link tải lại. Không coi escrow là tiền có thể rút.
- **Tài khoản nhận tiền:** danh sách chỉ dùng `accountNumberMasked`; form tạo dùng finance idempotency executor, sửa yêu cầu nhập lại số tài khoản đầy đủ và version hiện hành, xóa có disclosure HTML và version. Số đầy đủ không được đi vào flash attribute hoặc HTML lỗi; account đang được tham chiếu thì service từ chối xóa. Mọi mutation dùng POST+CSRF+PRG.
- **Yêu cầu rút:** lọc trạng thái và phân trang bằng GET; form POST mang wallet version, bank account ID, ghi chú tùy chọn và idempotency key, đi qua cùng finance command executor và rate limiter của REST command. Số tiền tối thiểu lấy từ `CreatePayoutRequest`; trạng thái đang chờ/đang xử lý khóa yêu cầu mới. Không gọi REST loopback.
- **Layout/no-JS:** ba route riêng trong teacher workspace, cards cho số dư/lịch sử và disclosure native cho edit/delete. Tất cả filter, pagination, submit và xác nhận dùng link/form HTML thường; không có skeleton hay animation vì dữ liệu SSR đồng bộ.
- **A11y/states:** nhãn cho mọi field, lỗi/success có role thông báo và anchor, trạng thái có chữ, giá trị dài wrap, focus rõ. Kiểm thử controller/security hiện PASS 32/32; browser screenshot, viewport/responsive/a11y thực tế, session-cookie smoke và thao tác payout staging chưa xác minh.

### Admin

#### `/admin`, `/admin/dashboard` — Tổng quan quản trị

- **Role:** chỉ session Admin; handler kiểm tra role và gọi `AdminDashboardService` trực tiếp. Cả hai path render cùng view; không gọi REST loopback. HTML đặt `Cache-Control: no-store`.
- **Dữ liệu:** tài chính cần chú ý (yêu cầu payout/refund đang chờ và tổng payout đang chờ) trước; tiếp theo là GMV, hoa hồng đã ghi nhận, số Teacher/Student và booking theo trạng thái. Chỉ dùng `AdminDashboardView` thật, không bịa chart, trend hoặc metrics.
- **Bố cục:** header workspace có logout POST+CSRF và tab Tổng quan; trang nhóm yêu cầu tài chính thành danh sách ngang ưu tiên, tiếp theo là tổng quan tài chính và người dùng/buổi học. Desktop dùng hai cột cho metrics phụ, mobile xếp dọc; số tiền dùng VND locale Việt. Shortcut tới hàng đợi sẽ được nối khi các route đích có handler SSR, tránh tạo link 404 trong lúc chuyển đổi.
- **Trạng thái/no-JS:** service lỗi hiển thị lỗi chung và link GET thử lại; giá trị 0 là dữ liệu hợp lệ và hàng đợi không có việc chờ có thông báo riêng. Dữ liệu SSR đồng bộ; không skeleton, animation hay JS.
- **Acceptance:** MockMvc render, lỗi dịch vụ, empty queue và security Admin session. Playwright login/session và hai alias trả HTTP 200 ở desktop/mobile, JS on/off; viewport 320–1440 không tràn; keyboard đưa focus đầu vào skip-link; body text contrast đo 9.29:1/17.66:1. Screenshot đã xem; runtime query count và session lifecycle/topology production chưa xác minh.

  #### `/admin/teachers` — Duyệt hồ sơ gia sư

  - **Role/data:** Admin session. Danh sách phân trang đọc qua `AdminApprovalService.teacherApprovals` → `TeacherApprovalFacade`; facade batch-load identity và documents, không REST loopback. Query `status`, `page`, `size`; trạng thái lấy từ `ProfileStatus`, size cho UI là 10/20/50. Mặc định chờ duyệt.
  - **Layout/actions:** Hàng hồ sơ tóm tắt tên/email, kinh nghiệm, hình thức dạy, số giấy tờ. Disclosure HTML mở thông tin giới thiệu và giấy tờ; private proof tải qua `/admin/teachers/documents/{id}/proof`, cùng origin, `no-store`, `nosniff`, chỉ MIME PDF/JPEG/PNG và `Content-Disposition: attachment`. Hồ sơ `PENDING_APPROVAL` có form duyệt (ghi chú tùy chọn) và form từ chối (lý do bắt buộc); cả hai POST+CSRF+PRG, audit context lấy từ request. Các trạng thái khác chỉ xem.
  - **States/no-JS:** Filter và pagination là GET thường; details và decision forms là HTML native. Có empty, service error/retry, action success/error, thiếu trường dữ liệu, danh sách tài liệu rỗng. Không có loading giả hoặc JavaScript.
  - **Acceptance:** MockMvc xác nhận render dữ liệu, filter normalization, empty/error, POST redirect, reject reason, private proof response và Admin-only/CSRF security. Browser/responsive/a11y screenshot và proof storage thực chưa xác minh.

  #### `/admin/credentials` — Duyệt minh chứng năng lực

  - **Role/data:** Admin session; list qua `TeacherCredentialService.adminList(status)` với đúng ba status `PENDING`, `APPROVED`, `REJECTED`. Service hiện tải toàn bộ danh sách của một status và không có pagination, vì vậy UI không tạo query phân trang. DTO có `teacherId`, label, status, rejection reason, version; không hiển thị `proofUrl`.
  - **Actions:** Proof tải qua route same-origin `/admin/credentials/{id}/proof`, gọi `TeacherCredentialService.adminProof`, trả PDF/JPEG/PNG bằng `no-store`, `nosniff`, attachment. Pending có form duyệt hoặc form từ chối; cả hai POST+CSRF+PRG và gửi version hiện tại. Lý do từ chối bắt buộc; service chịu trách nhiệm version, trạng thái, audit và cache revalidation.
  - **States/no-JS:** Ba link trạng thái là GET thường; có empty/error/retry, success/error sau command, lý do đã nhập giữ qua redirect validation. Các mục approved/rejected chỉ đọc; rejected hiển thị lý do nếu có. Không có modal, skeleton, animation hoặc JavaScript.
  - **Acceptance:** MockMvc xác nhận status normalization, dữ liệu/rejection reason, không rò REST proof URL, versioned PRG actions, proof headers và Admin-only/CSRF. Browser screenshot/responsive/a11y, session-cookie và storage provider thực chưa xác minh.

#### `/admin/subjects` — Duyệt đề xuất môn học

- **Role/data:** Admin session. Queue GET gọi `AdminApprovalService.subjectProposals(PageRequest)`; chỉ dữ liệu `PENDING` theo facade hiện tại. Danh mục lựa chọn gọi `SubjectFacade.getActiveSubjects()`. Query `page` và `size` là GET, size giới hạn ở 10/20/50; thứ tự ổn định theo thời điểm tạo và ID. Không có API tên gia sư trong snapshot nên hiển thị teacher UUID.
- **Actions:** Mỗi đề xuất có ba disclosure native: gắn với môn active, tạo môn mới hoặc từ chối. Tất cả dùng POST+CSRF+PRG; approve/reject mang version hiện tại và Admin audit context. Tạo mới dùng các giới hạn code/name/level/description/note của `ApproveSubjectProposalRequest`; reject yêu cầu reason theo DTO. Service kiểm tra trạng thái/version, môn active, trùng code/slug và gắn môn đã duyệt cho Teacher.
- **States/no-JS:** Có empty, lỗi queue và lỗi catalog độc lập, retry GET, success/error sau POST. Validation hoặc conflict PRG mở lại disclosure tương ứng và giữ dữ liệu đã nhập. Không có JavaScript, skeleton hoặc animation.
- **Scope boundary:** Route chỉ duyệt đề xuất. [API_CONTRACT.md](../architecture/API_CONTRACT.md) hiện ghi `POST|PUT /api/admin/subjects`, nhưng không có mapping tương ứng trong `AdminApprovalController` hoặc controller Admin khác đã kiểm tra; chưa có bằng chứng cho CRUD danh mục nên route không giả định capability đó.
- **Acceptance:** Page + security MockMvc PASS 38/38, gồm render, empty/error độc lập, normalization, versioned create/link/reject và Admin session/CSRF. Browser screenshot, responsive/a11y thực tế và session-cookie smoke chưa xác minh.

#### `/admin/users` — Quản lý người dùng

- **Role/data:** Chỉ Admin session. GET gọi `AdminUserDirectoryService.findUsers` → `IdentityDirectoryFacade.searchUsers`; directory chỉ trả Student và Teacher, không liệt kê Admin. Bộ lọc query: keyword tối đa 100 UTF-16 units, role `STUDENT|TEACHER`, status `ACTIVE|LOCKED`, page, size `20|50|100`, sort theo `createdAt|fullName|email|role|status|lastLoginAt` và `asc|desc`; mặc định ngày tạo mới nhất. Thứ tự có ID làm tie-breaker. Thời gian hiển thị `Asia/Ho_Chi_Minh`.
- **Layout/actions:** Heading và tổng số kết quả, GET filter/search/sort/page-size, danh sách hàng có tên/email/vai trò/trạng thái/ngày tạo/lần đăng nhập cuối. Mỗi hàng có link tới nhật ký `targetType=USER` theo ID và disclosure native để khóa hoặc mở khóa, yêu cầu lý do theo giới hạn request. POST+CSRF+PRG gọi `AdminApprovalService.changeUserStatus`, audit actor/IP/User-Agent; facade kiểm tra self/admin/valid state, cập nhật cache trạng thái và thu hồi refresh token khi khóa.
- **States/no-JS:** Form GET native giữ query; page lỗi có retry, empty/result-empty, keyword quá dài có cách sửa, invalid filters dùng mặc định an toàn. Form quyết định mở bằng `<details>`; lý do được giữ sau domain/conflict error. Không skeleton/loading giả, animation hoặc JavaScript. Nội dung dài wrap; khi hẹp, hàng chuyển thành bố cục nhiều dòng.
- **Acceptance:** AdminUsersPageControllerTest + SecurityChainIsolationTest PASS 40/40 (9 page, 31 security/isolation), gồm filter/sort/page, invalid input, empty/error, PRG retention và Admin session/CSRF. Browser screenshot, viewport/responsive/a11y thực tế và session-cookie smoke chưa xác minh.

#### `/admin/refunds` — Đối soát hoàn tiền

- **Role/data:** Chỉ Admin session. Danh sách phân trang từ `RefundService.findAdminRefunds`; filter nhận các trạng thái của request và mặc định tất cả. Mỗi dòng dùng `RefundRequestView` hiện hành, thời gian hiển thị theo `Asia/Ho_Chi_Minh`; không render `proofUrl` hoặc số tài khoản đầy đủ.
- **Layout/actions:** Bộ lọc và phân trang GET. Yêu cầu `PENDING` có form native duyệt số buổi hoặc từ chối; yêu cầu `APPROVED` có form multipart ghi mã giao dịch, thời điểm chuyển khoản và chứng từ. Mọi mutation là POST+CSRF+PRG, gửi version và idempotency key qua `FinanceCommandExecutor`; invariant/tính tiền/audit thuộc `RefundService`. Chứng từ đã lưu tải qua `/admin/refunds/{id}/proof`, kiểm tra trạng thái và trả cùng origin `no-store`/`nosniff`/attachment.
- **States/no-JS:** Có danh sách, empty, queue error/retry, action success/error. Validation giữ phần dữ liệu văn bản không nhạy cảm và mở disclosure tương ứng; file cần chọn lại sau redirect. Tệp giới hạn theo `FinanceProofStorage` (JPG/PNG/PDF, 10 MiB), server kiểm tra MIME; không có JavaScript, skeleton giả hoặc animation.
- **Security caveat:** Finance proof hiện được upload qua `FileStoragePort` legacy; HTML chỉ gọi server-side `CredentialProofStorage.downloadLegacy` bằng public ID đọc từ entity, không gửi URL đó ra trình duyệt. Runtime provider access chưa được xác minh.
- **Acceptance:** `AdminRefundsPageControllerTest` PASS 7/7 và `SecurityChainIsolationTest` PASS 33/33 trong regression hiện tại. Browser screenshot, responsive/a11y thực tế, provider storage và session-cookie smoke chưa xác minh.

#### `/admin/extensions` — Duyệt gia hạn

- **Role/data:** Chỉ Admin session. GET đọc danh sách phân trang qua `ExtensionService.findAdminExtensions`; filter trạng thái dùng enum hiện có. Thời gian hiển thị và nhập theo `Asia/Ho_Chi_Minh`; hồ sơ chỉ trình bày ID học viên/gói, lý do, ngày hết hạn đề nghị/đã duyệt và ghi chú.
- **Layout/actions:** Filter, phân trang và retry dùng link/form GET. Yêu cầu `PENDING` có form duyệt ngày hết hạn mới kèm ghi chú hoặc form từ chối có lý do. Mutation dùng POST+CSRF+PRG và `FinanceCommandExecutor` idempotency; service xử lý thay đổi package, trạng thái, audit và notification. Request/service hiện không có version nên HTML không gửi version giả.
- **States/no-JS:** Có empty, unavailable/retry, success/error; form lỗi mở lại disclosure, giữ ngày/ghi chú/lý do và autofocus trường đầu. Ngày mặc định lấy từ ngày đề nghị nhưng Admin có thể chỉnh. SSR đồng bộ nên không có skeleton, animation hoặc JavaScript.
- **Acceptance:** `AdminExtensionsPageControllerTest` PASS 5/5; `SecurityChainIsolationTest` PASS 33/33 trong regression hiện tại. Browser screenshot, viewport/responsive/a11y, session-cookie và notification provider smoke chưa xác minh.

#### `/admin/payouts` — Chi trả gia sư

- **Role/data:** Chỉ Admin session. Queue phân trang qua `PayoutService.findAdminPayoutWorkspace`; filter `ALL|PENDING|PROCESSING|SUCCEEDED|REJECTED|FAILED` là GET và thứ tự mới nhất trước, có ID làm tie-breaker. Request hiển thị teacher UUID, số tiền VND, ghi chú, trạng thái, mã giao dịch và thời điểm đã chuyển. Thông tin ngân hàng được tải theo lô từ `TeacherBankAccountRepository`, giải mã server-side rồi chỉ render trong disclosure `<details>` trên trang Admin `no-store`; số tài khoản không thêm vào `PayoutRequestView`/REST response. Không có tên gia sư trong view model hiện tại.
- **Actions:** `PENDING` có POST bắt đầu xử lý và từ chối; `PROCESSING` có POST từ chối hoặc multipart xác nhận đã chuyển. Form gửi optimistic version hiện hành và UUID idempotency key qua `FinanceCommandExecutor`; CSRF+PRG luôn bật. Chứng từ giới hạn 10 MiB, MIME/đuôi file được `FinanceProofStorage` kiểm tra. Thành công ghi nhận chuyển khoản qua service hiện có, không chuyển tiền tự động. Proof chỉ tải qua `/admin/payouts/{id}/proof`, kiểm tra `SUCCEEDED` phía server và trả same-origin `no-store`/`nosniff`/attachment; UI không render `proofUrl`.
- **States/no-JS:** Filter/page/retry dùng GET; có empty, unavailable, success/error. Form lỗi mở disclosure tương ứng và giữ lại lý do hoặc metadata văn bản; tệp phải chọn lại sau redirect. Thông tin ngân hàng không giải mã được hiện cảnh báo rõ. Synchronous SSR không cần JavaScript, skeleton hay animation.
- **Acceptance:** `AdminPayoutsPageControllerTest` PASS 7/7, `PayoutServiceTest` PASS 3/3 và `SecurityChainIsolationTest` PASS 34/34. Browser screenshot, responsive/a11y thực tế, session-cookie, số lượng query runtime và storage provider chưa xác minh.

#### `/admin/booking-settlements` — Quyết toán buổi học

- **Role/data:** Chỉ Admin session. `BookingReadService.adminSettlements` cung cấp trang settlement, batch-load booking và identity/teacher names. GET filter hỗ trợ status enum hiện hành và page; thứ tự mới nhất trước, ID làm tie-breaker. Hiển thị học viên/gia sư, trạng thái booking và settlement, khung giờ, xác nhận từng bên, deadline, net amount và lý do/thời điểm khiếu nại.
- **Actions:** Form native gửi `version` của settlement cùng ghi chú bắt buộc. `DISPUTE_PENDING` cho phép mở lại vòng xác nhận hoặc giữ khoản tiền; `AWAITING_ADMIN_DECISION` cho phép release/retain; `REOPENED` chỉ hiện quyết định sau deadline nếu chưa đủ xác nhận. Những trạng thái khác không hiện action. Các mutation gọi `BookingService` trực tiếp, nơi giữ optimistic version, invariant tiền, audit và chuyển trạng thái; không thêm endpoint/API hoặc idempotency giả.
- **States/no-JS:** Filter/page/retry là GET; có empty/error/retry và PRG success/error. Lỗi giữ ghi chú và mở lại đúng disclosure để xử lý version conflict; synchronous SSR không có skeleton/animation hoặc phụ thuộc JavaScript. Response `no-store` vì dữ liệu tài chính và tranh chấp.
- **Acceptance:** `AdminBookingSettlementsPageControllerTest` PASS 6/6 và `SecurityChainIsolationTest` PASS 35/35. Browser screenshot, responsive/a11y thực tế, session-cookie và runtime query count chưa xác minh.

#### `/admin/settings` — Cài đặt nền tảng

- **Role/data:** Chỉ Admin session. SSR đọc/ghi qua `AdminSettingsService`; form dùng đúng `UpdatePlatformSettingsRequest` và Bean Validation hiện hành cho commission rate, Bayesian minimum reviews, reminder hours và expiration hours. Actor ID chuyển vào service để audit. Hiển thị thời điểm cập nhật theo `Asia/Ho_Chi_Minh`.
- **Layout/actions:** Một form POST+CSRF gồm bốn input số có label/help text rõ, commit bằng nút Lưu. Gửi form trực tiếp kể cả khi tắt JavaScript; thành công/lỗi dùng PRG. Không tạo trường version, setting hoặc rule ngoài DTO.
- **States/no-JS:** Có giá trị hiện tại, lỗi tải kèm retry, thành công và validation/domain error. Input đã nhập được giữ qua redirect lỗi; form ẩn khi không tải được giá trị hiện hành để tránh ghi đè bằng dữ liệu giả. Trang `no-store`, không skeleton hoặc animation.
- **Acceptance:** `AdminSettingsPageControllerTest` PASS 5/5 và `SecurityChainIsolationTest` PASS 36/36. Browser/responsive/a11y/session-cookie chưa xác minh.

#### `/admin/audit-logs` — Nhật ký thao tác

- **Role/data:** Chỉ Admin session. Đọc qua `AuditLogQueryService.findAuditLogs`; filter GET gồm actor UUID, action enum, target type và target UUID, page cố định 20 bản ghi. Sắp xếp `createdAt DESC` như repository. Hiển thị thời điểm Việt Nam, actor/target, IP, user-agent; before/after JSON chỉ mở trong disclosure. JSON và user-provided audit values đi qua Thymeleaf escaped text, không render HTML thô.
- **Layout/actions:** Bộ lọc GET có tùy chọn action lấy từ `AuditAction`; link phân trang giữ filter. Từ `/admin/users`, link “Xem nhật ký thao tác” lọc theo targetType `USER` và user ID. Không có mutation.
- **States/no-JS:** Có empty/error/retry; UUID sai và target type quá dài báo cách sửa ngay tại trang. Synchronous SSR không có JavaScript, skeleton hoặc animation. Response `no-store` do chứa dữ liệu audit nhạy cảm.
- **Acceptance:** `AdminAuditLogsPageControllerTest` PASS 4/4 và `SecurityChainIsolationTest` PASS 36/36. Browser/responsive/a11y/session-cookie và chính sách lưu audit runtime chưa xác minh.

| Route | Dữ liệu/hành động chính | Layout/states/no-JS |
|---|---|---|
| `/admin`, `/admin/dashboard` | Admin dashboard/read model | Finance queues first, platform activity second; synchronous SSR errors; no fake loading state or JavaScript dependency |
| `/admin/teachers`, `/admin/credentials` | Approval/credential queues and decisions | Filter/pagination GET, detail review form; private proof proxied no-store |
| `/admin/subjects` | Review subject proposals | Paginated GET; native link/create/reject disclosure forms, versioned POST+CSRF+PRG; no subject CRUD claim |
| `/admin/users` | User directory/moderation | Query GET search/filter/sort/page; native status-change disclosure, reason-required POST+CSRF+PRG |
| `/admin/payouts` | Payout queue/actions/proof completion | Bank destination details inside disclosure; versioned native POST; multipart proof validation; no public proof URL |
| `/admin/booking-settlements` | Escrow/dispute queue and release/retain/reopen | Status filter/page GET; batch-enriched records; state-appropriate versioned native forms; no JavaScript dependency |
| `/admin/settings` | Platform settings read/update | DTO validation; native POST+CSRF+PRG; actor audit |
| `/admin/audit-logs` | Audit read/filter/page | GET filters/page; escaped JSON disclosures; no-store |

#### `/teacher/packages`, `/teacher/packages/create`, `/teacher/packages/{id}` — Gói học

- **Role/ownership:** Teacher session; owner ID luôn lấy từ `AuthenticatedUser`. `PricingPackageService` là nơi thực thi ownership, approval, môn đã gắn, version và quy tắc package đã mua. Không truy cập repository module khác.
- **Dữ liệu/hành động:** danh sách SSR có pagination GET; form tạo/sửa dùng trường `UpsertPricingPackageRequest` hiện hành và môn đang hoạt động đã gắn với gia sư. Tạo/sửa/chuyển trạng thái gọi trực tiếp `PricingPackageService`; không gọi REST loopback. Mỗi mutation dùng POST+CSRF; tạo, sửa, đổi trạng thái thành công dùng PRG.
- **Bố cục/no-JS:** list hiển thị môn, tên, số buổi, thời lượng, hạn, giá và trạng thái. Create/detail dùng form có label cho tất cả trường; chi tiết có khối trạng thái riêng với form chuyển trạng thái. Tất cả link và form native, không có phụ thuộc JavaScript.
- **Quy tắc an toàn:** package version gửi lại khi sửa/trạng thái; xung đột yêu cầu xem lại bản mới. Khi package đã có StudentPackage, nội dung bị khóa và UI chỉ đưa thao tác tạm ngưng; nếu không kiểm tra được trạng thái mua, khóa sửa/bật trạng thái cho đến khi tải lại. Backend vẫn xác nhận mọi invariant. Package ACTIVE chỉ hợp lệ khi Teacher được duyệt.
- **Trạng thái/a11y:** danh sách empty/load-error/retry, form field/domain errors giữ dữ liệu, success notice, trạng thái package có cả chữ và màu, nội dung dài wrap, focus-visible. SSR không có skeleton hay animation. MockMvc/security regression tests có; browser screenshot, viewport/a11y thực tế và session-cookie smoke chưa xác minh.

#### `/teacher/packages`, `/teacher/packages/create`, `/teacher/packages/{id}` — Gói học

- **Role/ownership:** Teacher session; owner ID luôn lấy từ `AuthenticatedUser`. `PricingPackageService` là nơi thực thi ownership, approval, môn đã gắn, version và quy tắc package đã mua. Không truy cập repository module khác.
- **Dữ liệu/hành động:** danh sách SSR có pagination GET; form tạo/sửa dùng trường `UpsertPricingPackageRequest` hiện hành và môn đang hoạt động đã gắn với gia sư. Tên gói giới hạn theo cột lưu trữ hiện hành; không tự giới hạn mô tả hoặc các giá trị khác nếu nguồn không quy định. Tạo/sửa/chuyển trạng thái gọi trực tiếp `PricingPackageService`; không gọi REST loopback. Mỗi mutation dùng POST+CSRF; tạo, sửa, đổi trạng thái thành công dùng PRG.
- **Bố cục/no-JS:** list hiển thị môn, tên, số buổi, thời lượng, hạn, giá và trạng thái. Create/detail dùng form có label cho tất cả trường; chi tiết có khối trạng thái riêng với form chuyển trạng thái ngay. Tất cả link và form native, không có phụ thuộc JavaScript.
- **Quy tắc an toàn:** package version gửi lại khi sửa/trạng thái; xung đột giữ dữ liệu nhập và hiển thị thông báo để người dùng tải lại bản mới. Chỉ Teacher `APPROVED` mới tạo/cập nhật/bật bán. Khi package đã có StudentPackage, nội dung bị khóa và UI chỉ đưa thao tác tạm ngưng; nếu không kiểm tra được trạng thái mua, khóa sửa/bật trạng thái cho đến khi tải lại. Backend vẫn xác nhận mọi invariant.
- **Trạng thái/a11y:** danh sách empty/load-error/retry, form field/domain errors giữ dữ liệu và liên kết với input, success notice, trạng thái package có cả chữ và màu, nội dung dài wrap, focus-visible. Page query ngoài phạm vi được chuẩn hóa về trang cuối hợp lệ để tránh trạng thái rỗng sai. SSR không có skeleton hay animation. Browser evidence và phần còn thiếu theo từng route được ghi tại [PROGRESS.md](PROGRESS.md) và [STATUS.md](../STATUS.md).

#### `/teacher/availability` — Lịch rảnh gia sư

- **Role/ownership:** chỉ Teacher session; đọc/thay thế theo principal qua `TeacherAvailabilityService`. Mọi trang đặt lịch tiếp tục dùng availability service như nguồn xác nhận; giao diện không tự quyết định slot còn hợp lệ.
- **Dữ liệu/hành động:** mỗi dòng gồm ngày trong tuần, giờ bắt đầu/kết thúc, IANA timezone và trạng thái hoạt động. Cho phép nhiều dòng trong cùng ngày; validation overlap/range/timezone cuối cùng do service thực thi. Lưu là thay thế toàn bộ danh sách trong transaction, nên cảnh báo rõ trước nút lưu.
- **Bố cục/no-JS:** desktop mỗi khung là một dòng gọn; tablet/mobile xếp thành nhóm field. Nút thêm và xóa gửi POST cùng toàn bộ form rồi server render lại để giữ các giá trị đã nhập; nút lưu dùng POST+CSRF+PRG. Không phụ thuộc JavaScript hoặc drag/drop.
- **Trạng thái/a11y:** không có khung giờ, lỗi tải kèm link thử lại (khóa thao tác), lỗi field/domain giữ input, notice thành công. Các trường đều có label, field error liên kết, trạng thái hoạt động có chữ; summary lỗi nhận focus sau render lại, còn field error liên kết qua `aria-describedby`. Thời gian hiển thị theo timezone của từng dòng. SSR không có loading/skeleton/animation. Bằng chứng browser và phần còn thiếu theo từng route ở [PROGRESS.md](PROGRESS.md) và [STATUS.md](../STATUS.md).

#### `/teacher/students`, `/teacher/students/{studentId}` — Học viên và gói học

- **Role/ownership:** chỉ Teacher session. `TeacherLearnerService.list` lấy Teacher hiện tại từ principal và chỉ trả các student package thuộc gia sư đó. Trang chi tiết gọi lại service với `studentId`; không có gói liên quan thì trả 404, không xác nhận quan hệ qua dữ liệu ngoài service.
- **Dữ liệu/hành động:** danh sách nhóm các gói theo học viên trong trang hiện tại; có pagination GET. Chi tiết phân trang gói đã mua, hiển thị snapshot tên gói/môn, trạng thái, tổng/còn lại/đã đặt/đã tiêu thụ/đã hoàn, giá mua và hạn dùng. Không có endpoint hồ sơ học viên riêng hoặc thao tác sửa/xóa gói ở khu vực này.
- **Bố cục/no-JS:** danh sách hiển thị tên, số gói, vài dòng tóm tắt gói và link chi tiết; trang chi tiết có breadcrumb, các chỉ số gói và link quay lại. Link phân trang là GET thường, không cần JavaScript.
- **Trạng thái/a11y:** danh sách có empty và lỗi tải kèm link thử lại; chi tiết trả HTTP 404 cùng HTML SSR thân thiện khi học viên không có gói thuộc Teacher hiện tại, không tiết lộ việc học viên có tồn tại ở nơi khác hay không. Lỗi tải chi tiết có link thử lại; tên dài được wrap, avatar chữ cái trang trí được ẩn với trợ năng. Response là `no-store`; link điều hướng và logout POST có CSRF. MockMvc/browser đã xác minh một phần; JavaScript-off bị browser permission gate chặn, một số error/empty state và full keyboard sweep còn thiếu. Bằng chứng acceptance/cutover xem [PROGRESS.md](PROGRESS.md) và [STATUS.md](../STATUS.md).

#### `/teacher/trial-requests` — Yêu cầu học thử

- **Role/ownership:** chỉ Teacher session; danh sách và command gọi `TrialRequestService` theo user ID từ principal. Service giới hạn list theo Teacher profile; accept/reject kiểm tra sở hữu, trạng thái PENDING và version hiện tại.
- **Dữ liệu:** GET phân trang theo yêu cầu mới nhất; view batch-load tên học viên/môn từ facade theo trang. Không thêm trạng thái lọc vì REST Teacher hiện không nhận status filter.
- **Bố cục/no-JS:** danh sách thẻ gồm tên học viên, môn, thời gian đề xuất, lời nhắn và trạng thái. Yêu cầu đang chờ mở `<details>` native có hai form: chọn giờ/hình thức/thông tin kết nối hoặc từ chối có lý do. Accept dùng POST+CSRF+PRG rồi chuyển tới booking vừa tạo; reject quay về cùng trang. Thời gian nhập là giờ `Asia/Ho_Chi_Minh`; không cần JavaScript hoặc modal.
- **Trạng thái/a11y:** empty/error/retry, nhãn status bằng chữ, field label, focus/anchor tới lỗi sau PRG, version và page nằm trong form. Tên/lời nhắn dài được wrap; response `no-store`. Controller/service/security MockMvc có; browser screenshot, responsive/a11y thực tế và session-cookie chưa xác minh.

#### `/teacher/bookings`, `/teacher/bookings/{id}` — Lịch dạy

- **Role/ownership:** chỉ Teacher session. Danh sách dùng `BookingReadService.listTeacherWorkspace` theo Teacher profile lấy từ principal; chi tiết dùng `BookingReadService.detail(..., teacherRole=true)`, trả 404 khi không thuộc gia sư hiện tại.
- **Dữ liệu/hành động:** danh sách lọc trạng thái, ngày bắt đầu/kết thúc và phân trang qua GET, sắp xếp buổi gần nhất trước; ngày được diễn giải theo `Asia/Ho_Chi_Minh`; read path riêng batch-load tên học viên/môn học theo trang thay vì dựng detail đầy đủ cho từng dòng. Chi tiết lấy booking, report, settlement và `canDispute`/`canConfirm` từ read service. Hủy chỉ hiện với booking `SCHEDULED`; gửi report cho trial sau khi kết thúc, hoặc với booking trả phí khi read service cho phép xác nhận; khiếu nại chỉ hiện khi `canDispute` là true. Command gọi `BookingService`; version lấy từ booking; report là dữ liệu bắt buộc theo service.
- **Bố cục/no-JS:** list gồm thời gian, học viên, môn, hình thức, trạng thái và badge học thử; detail gồm thông tin buổi, settlement, report cũ và các form action. Filter/pagination dùng link/form GET; action dùng form POST+CSRF+PRG. Không cần JavaScript.
- **Trạng thái/an toàn:** list phân biệt chưa có buổi nào với bộ lọc không khớp, cảnh báo ngày sai định dạng, error/retry; detail 404 cho resource không thuộc gia sư, error/retry khi service lỗi; report/meeting link chỉ clickable nếu là HTTP(S) URL hợp lệ. Status và action eligibility được đọc từ service; mọi invariant vẫn do `BookingService` thực thi. Response `no-store`. MockMvc/service/security regression tests có; browser screenshot, responsive/a11y thực tế và session-cookie smoke chưa xác minh.

## Shared shell and implementation contract

- Layouts: public shell; auth shell; workspace shell with role navigation; admin finance tables share components but not permissions.
- Every route uses explicit `PageState`: success, empty, validation/domain error, unauthorized/forbidden, unavailable. SSR pages do not show fake skeleton.
- Form mutations use POST/redirect/GET; search/filter/pagination use GET; return URL is role allowlisted.
- All dates display `Asia/Ho_Chi_Minh`; data and invariants remain backend-owned.
