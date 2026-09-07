# Kế hoạch giao diện Tutor Match

Ngày: 07/09/2026. Phạm vi: rà soát mã nguồn FE/BE hiện có và đề xuất thiết kế; chưa chạy kiểm thử giao diện trên trình duyệt, chưa sửa mã ứng dụng. Các route dưới đây là cấu trúc đích, không đồng nghĩa tất cả đã tồn tại. Controller/DTO trong mã nguồn là căn cứ chính, không coi tài liệu kế hoạch cũ là API đã triển khai.

## 1. Kết luận và vấn đề hiện tại

Sản phẩm nên có hai trải nghiệm liên kết: khu khám phá giúp chọn gia sư và khu làm việc theo vai trò giúp học/dạy/vận hành. Trang chủ chỉ tóm tắt; danh sách giúp so sánh; trang chi tiết giúp quyết định; dashboard giúp xử lý việc tiếp theo.

FE đã có homepage chia section, danh sách môn, thẻ gia sư, hồ sơ riêng và nhiều trang nghiệp vụ. Không cần đập bỏ toàn bộ. Những điểm cần sửa trước khi làm đẹp:

| Bằng chứng trong repo | Vấn đề | Hướng xử lý |
|---|---|---|
| `TeacherSearchRepository` lấy MIN(price_vnd); `TeacherCard.tsx` ghi /buổi | Giá cả gói bị diễn giải thành giá buổi | Ghi “Gói từ …đ”; chỉ tính giá/buổi khi biết số buổi của cùng gói |
| BE `PricingPackageView`: totalSessions, sessionDurationMinutes, durationDays; FE public.ts/TeacherPackagesTab dùng sessionCount, durationMinutes | Thông tin gói có thể thiếu dù API có dữ liệu | Chuẩn hóa DTO và adapter, hiển thị số buổi, thời lượng buổi, hạn sử dụng |
| TeacherPackagesTab dùng grid-cols, p-6…; package.json không có Tailwind và không tìm thấy CSS tương ứng | Nhiều class không có định nghĩa trong source được kiểm tra | Dùng CSS Modules + Ant Design thống nhất, kiểm tra computed styles khi triển khai |
| StudentAppLayout link /student nhưng chưa có page; Sidebar còn /student/dashboard, /student/schedule, /student/chat | Điều hướng không thống nhất | Một danh mục route chuẩn; bỏ link chết hoặc redirect route cũ |
| Navbar luôn trỏ hồ sơ học viên, có /student/settings chưa tồn tại | Menu không theo vai trò | Menu tài khoản riêng cho STUDENT/TEACHER/ADMIN |
| Teacher layout chỉ có 5 mục | Ví, tin nhắn, bài tập… khó được tìm thấy | Nhóm sidebar theo công việc, bổ sung mục có trang thật |
| StudentAppLayout giữ marginLeft:248 khi sidebar collapse | Nguy cơ thừa khoảng trắng trên mobile | Main offset phụ thuộc breakpoint; kiểm tra ở 360/390/768/1280px |
| Homepage bắt lỗi chung cho Promise.all và giữ danh sách rỗng | API lỗi dễ trông như không có gia sư/môn học | Tải và báo lỗi độc lập từng section |
| Sidebar dùng user.status !== APPROVED | Trộn trạng thái tài khoản và trạng thái hồ sơ gia sư | Tách UserStatus và ProfileStatus; quyền dựa trên dữ liệu thực |

## 2. Cấu trúc và phong cách chung

- Nhãn tiếng Việt thống nhất: “Gia sư”, “Học viên”, “Môn học”, “Gói học”, “Buổi học”. Không đổi luân phiên giáo viên/giảng viên/gia sư.
- Giữ nhận diện xanh hiện có: xanh chính #0F766E, nền #FBFAF8, mặt thẻ #FFFFFF, chữ #1C1917, viền #E7E3DC, nhấn #B45309. Màu trạng thái lấy token hiện có và luôn kèm chữ.
- Tiêu đề Be Vietnam Pro, nội dung Inter với fallback hỗ trợ tiếng Việt. Nội dung 16px, nhãn phụ 13–14px, H1 32–40px desktop/28px mobile. Nạp font thực tế, tránh chỉ khai báo tên.
- Container public 1200px; khoảng cách section 56–72px desktop/32px mobile; card padding 20–24px, radius 12px; ưu tiên viền nhẹ và khoảng trắng.
- Đặc trưng: thanh tìm gia sư theo “Môn cần học → Hình thức → Ngân sách”, sau tìm kiếm thể hiện tiêu chí thành chip có thể bỏ. Không tạo điểm “match 98%” khi không có thuật toán.
- Public: header 64px, logo, tìm gia sư, môn học, xếp hạng, trở thành gia sư, tài khoản. Footer chứa hướng dẫn, chính sách và liên hệ đã được chốt nội dung.
- Khu đăng nhập: sidebar 248px + topbar 64px + nội dung; menu tài khoản, thông báo, liên kết ra marketplace. Admin dùng cùng token nhưng ưu tiên bảng dày vừa phải.
- Mobile: 1 cột, bộ lọc trong drawer, menu mở được sau khi sidebar ẩn; không giữ offset desktop. Điều hướng dưới tối đa 4–5 mục, các mục còn lại trong “Thêm”.

## 3. Các trang công khai

### 3.1 Trang chủ `/`

Mục đích: hiểu dịch vụ, chọn một hướng khám phá trong vài giây.

```text
Logo     Tìm gia sư | Môn học | Xếp hạng       Tài khoản
------------------------------------------------------
Tìm gia sư cho môn bạn muốn tiến bộ
[Môn học] [Online/Offline] [Ngân sách] [Tìm gia sư]
------------------------------------------------------
Khám phá môn học                         Xem tất cả →
[Toán] [Tiếng Anh] [Vật lý] [Các môn từ API...]
------------------------------------------------------
Gia sư được đánh giá cao                 Xem tất cả →
[Thẻ gia sư] [Thẻ gia sư] [Thẻ gia sư]
[Thẻ gia sư] [Thẻ gia sư] [Thẻ gia sư]
------------------------------------------------------
Chọn gia sư → Gửi yêu cầu học thử / Mua gói → Thống nhất lịch
------------------------------------------------------
Bạn muốn trở thành gia sư?                Tạo hồ sơ →
Footer
```

- Hero gọn, không chiếm cả màn hình; CTA chính là tìm gia sư.
- Môn học: 8 mục, 4 cột desktop/2 mobile. Tên, cấp học, icon theo môn. Click → `/teachers?subjectId=…`; “Xem tất cả” → `/subjects`.
- Gia sư: 6 thẻ, 3 cột desktop/2 tablet/1 mobile. Nguồn GET `/api/public/teachers?sort=rating_desc&page=0&size=6`. Không gọi là cá nhân hóa.
- Hướng dẫn 3 bước là nội dung tĩnh, phân biệt học thử cần gia sư chấp nhận và mua gói cần xác nhận thanh toán.
- Không dùng “hàng ngàn gia sư”, tỷ lệ thành công hoặc testimonial nếu không có dữ liệu chứng minh. Không cần kéo toàn bộ ranking lên homepage.
- Mỗi section có loading/error/empty riêng. Lỗi tải có nút thử lại; danh sách rỗng không thay bằng số liệu demo.

### 3.2 Danh sách gia sư `/teachers`

- Đầu trang: tiêu đề, tìm từ khóa, số kết quả đúng meta từ BE.
- Trái 260px: môn, hình thức, khoảng giá gói, mức sao, ngày và giờ. Bộ lọc giờ phải gửi đủ dayOfWeek/startTime/endTime theo DTO.
- Phải: chip đang lọc + xóa bộ lọc; sort; lưới 3 thẻ khi đủ rộng, giảm còn 2 khi nội dung chật; phân trang 12 kết quả/trang.
- Sort đúng BE: price_asc, price_desc, rating_desc, experience_desc. Không thêm “gần bạn” hoặc “phù hợp nhất” như một sort thật.
- URL giữ bộ lọc để refresh/back/share không mất trạng thái. UI trang 1 ↔ API page 0, đổi bộ lọc về trang đầu.
- Không có kết quả: giữ tiêu chí, gợi ý bỏ giá/giờ, nút xóa bộ lọc. Không tự đổi điều kiện âm thầm.
- Chưa có lọc tỉnh, khoảng cách, giới tính, trường đại học, ngôn ngữ trong TeacherSearchParams; không làm control giả.

### 3.3 Thẻ gia sư dùng chung

```text
[Avatar] Nguyễn Văn A        [Đã xác minh nếu BE có]
         Toán · Vật lý · +1
3 năm kinh nghiệm           Online · Offline
4,8 ★ (24 đánh giá)
Gói từ 1.200.000đ
                                     Xem hồ sơ →
```

Đây là ví dụ minh họa bố cục, không phải dữ liệu thật. Giới hạn tên 2 dòng, môn 2 chip + số còn lại; không đưa bio dài, video, bảng lịch, toàn bộ gói học hoặc chứng chỉ vào card. Có thể bỏ bio hoàn toàn. Chiều cao card trong cùng hàng đồng đều. Cả thẻ là liên kết tới `/teachers/{id}` có focus bàn phím rõ, không lồng nút vào link.

Map từ DTO: fullName, avatarUrl, verifiedBadge, subjects, yearsOfExperience, supportsOnline/supportsOffline, averageRating, reviewCount, startingPriceVnd. Không tráo bayesianRating thành sao đánh giá thông thường. reviewCount=0 → “Chưa có đánh giá”. Không có gói → “Chưa mở gói học”; BE hiện cần xử lý giá null rõ ràng vì rs.getLong có thể trả 0, không tự quảng cáo miễn phí.

### 3.4 Hồ sơ gia sư `/teachers/[id]`

```text
Trang chủ / Gia sư / Tên gia sư
Avatar | Tên, môn, kinh nghiệm, online/offline, đánh giá
------------------------------------------------------
Nội dung chính ~8/12              Khung hành động ~4/12
Giới thiệu / Gói / Lịch / Đánh giá Chọn môn/gói
Bio, video nếu có                Tổng giá, số buổi
Gói học                          [Mua gói học]
Lịch rảnh                        [Yêu cầu học thử]
Đánh giá                         Điều kiện áp dụng
```

- Dùng thanh neo tới section để người dùng thấy đầy đủ nội dung; trên mobile khung mua nằm sau phần tóm tắt, CTA dưới không che nội dung.
- Gói: tên, môn, totalSessions, sessionDurationMinutes, durationDays, priceVnd. Hiển thị giá trọn gói là chính, giá chia mỗi buổi chỉ là phép tính phụ và phải ghi rõ.
- Lịch public là lịch rảnh lặp theo tuần, không hứa slot cụ thể còn trống. Không gắn “đặt ngay” vào từng ô.
- Đánh giá: sao, người đánh giá theo DTO thực, ngày, nội dung; có phân trang riêng. Gói cũng cần phân trang khi meta có nhiều trang.
- Guest bấm mua/học thử → đăng nhập với returnTo nội bộ giữ teacherId/packageId. Sau đăng nhập quay lại bước xác nhận, không tự tạo hóa đơn.
- STUDENT được mua/gửi yêu cầu học thử. TEACHER/ADMIN xem công khai nhưng không thấy nút mua như học viên.
- Form học thử: teacherId, subjectId, preferredStartTime, note; nút “Gửi yêu cầu học thử”, trạng thái sau gửi “Chờ gia sư phản hồi”. Không hứa miễn phí khi chưa xác minh chính sách.
- Chỉ mở hội thoại đã có quyền truy cập. Chưa có REST endpoint khởi tạo tư vấn tùy ý từ hồ sơ; muốn CTA “Nhắn tin trước khi mua” cần bổ sung contract.
- Public detail chưa trả verifiedBadge/chứng chỉ; không tự gắn badge/chứng chỉ như dữ liệu đã có. Subject detail hiện là tên môn, form học thử cần ánh xạ ID từ nguồn có ID, tránh ghép tên không duy nhất.

### 3.5 Môn học `/subjects`

- Tiêu đề + tìm tên môn + chọn cấp học; lưới 4/2/1 cột tùy chiều rộng, phân trang 12–16 mục.
- Card: icon nội bộ, tên, educationLevel, mô tả tối đa 2 dòng; CTA “Tìm gia sư môn này”.
- Nguồn `/api/public/subjects`: id/code/name/slug/educationLevel/description. Không có totalTeachers/thumbnailUrl trong DTO nên không hiển thị số gia sư hay ảnh API giả.
- Click → danh sách gia sư đã lọc môn. MVP không cần thêm một trang chi tiết môn chỉ lặp nội dung. `/subjects/[slug]` là mở rộng khi có mô tả/lộ trình và API tra cứu slug thật.

### 3.6 Xếp hạng `/ranking`

- Giới thiệu ngắn cách hiểu xếp hạng; lọc môn; danh sách thứ hạng, avatar, tên, điểm đúng nhãn theo API, số buổi hoàn thành nếu được trả.
- Click → hồ sơ. Desktop có thể nhấn nhẹ top 3, nhưng vẫn giữ danh sách đọc được; mobile dùng hàng gọn.
- GET `/api/public/teachers/ranking`; không dùng “top tháng” khi API không có khoảng thời gian đó.

### 3.7 Hướng dẫn, trở thành gia sư, chính sách

- `/how-it-works`: hai phần học viên/gia sư, quy trình thật và link đi tiếp; nội dung FE tĩnh.
- `/become-a-tutor`: điều kiện chuẩn bị hồ sơ, quá trình xét duyệt; CTA đăng ký TEACHER hoặc tiếp tục hồ sơ hiện có.
- `/policies`, `/contact`: chỉ xuất bản nội dung vận hành đã thống nhất. Không tự viết thời hạn hoàn tiền hoặc mức phí thành cam kết.

## 4. Đăng nhập và phân quyền

Giữ các route `/auth/login`, `/auth/register`, `/auth/verify-email`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/oauth/role`. Layout form gọn tối đa 460px, có nhãn thật, lỗi ngay trường, trạng thái gửi và link quay về. Đăng ký chỉ STUDENT/TEACHER; ADMIN không có lựa chọn đăng ký công khai.

BE có 3 role: STUDENT, TEACHER, ADMIN. UserStatus: PENDING_VERIFICATION/ACTIVE/LOCKED/DISABLED. ProfileStatus riêng: DRAFT/PENDING_APPROVAL/APPROVED/REJECTED. Không tạo role PARENT; parent-contact là thông tin liên hệ trong tài khoản học viên.

| Khả năng | Guest | STUDENT active | TEACHER chưa duyệt | TEACHER approved | ADMIN |
|---|---|---|---|---|---|
| Khám phá công khai | Có | Có | Có | Có | Có |
| Mua gói, gửi học thử | Đăng nhập | Dữ liệu của mình | Không | Không | Không |
| Xem học tập/nộp bài/đánh giá | Không | Theo quan hệ sở hữu và trạng thái BE | Không | Không | Không mặc định |
| Soạn hồ sơ gia sư, tài liệu | Không | Không | Có, tùy trạng thái cho phép sửa | Hồ sơ của mình | Duyệt theo API |
| Tạo buổi, hoàn thành/hủy buổi, giao/chấm bài | Không | Không | UI khóa thao tác vận hành theo chính sách được BE thực thi | Theo quyền BE và quan hệ học viên | Không mặc định |
| Ví, ngân hàng, rút tiền | Không | Không | Kiểm tra điều kiện endpoint; không đồng nhất với quyền tạo gói | Dữ liệu của mình | Xử lý yêu cầu qua API admin |
| Quản trị | Không | Không | Không | Không | Có |

Đây là ma trận UI mục tiêu; kiểm tra điều kiện service/security cho từng thao tác trước khi triển khai. Không giả định mọi endpoint TEACHER đều yêu cầu APPROVED chỉ dựa vào role. FE ẩn/khóa đúng UX nhưng BE phải kiểm tra quyền, sở hữu và chuyển trạng thái.

- Chưa đăng nhập → login; sai role đã đăng nhập → `/forbidden`, không vòng lặp login.
- Chưa xác minh → xác minh email; locked/disabled → thông báo tình trạng và hành động phù hợp, không hiện dashboard thao tác được.
- Sau login: STUDENT `/student`, TEACHER chưa duyệt `/teacher/profile`, TEACHER duyệt `/teacher`, ADMIN `/admin`.
- DRAFT: checklist + lưu + gửi duyệt. PENDING_APPROVAL: trạng thái đã gửi, quyền sửa theo BE. REJECTED: lý do + sửa/gửi lại. APPROVED: mở công việc và trạng thái hiển thị công khai theo dữ liệu profile.

## 5. Khu học viên

Sidebar: Tổng quan · Gói học · Lịch học · Bài tập · Tin nhắn · Yêu cầu · Thông báo. Hồ sơ trong menu tài khoản.

| Trang đích | Bố cục và nội dung | Bấm vào đâu / lưu ý dữ liệu |
|---|---|---|
| `/student` (mới) | Trên: buổi sắp tới; giữa: gói đang học và bài cần làm; bên: thông báo cần xử lý. Học viên mới thấy CTA tìm gia sư | Tới booking/gói/bài tập. Tổng hợp từ API hiện có, không suy ra tổng toàn hệ thống từ trang dữ liệu đầu |
| `/student/packages` | Tabs trạng thái; card tên gói, gia sư, môn, số buổi còn lại, hạn dùng | `/student/packages/[id]`; GET student/packages |
| `/student/packages/[id]` | Tóm tắt gói, tiến độ theo dữ liệu, lịch liên quan, thông tin mua, khối yêu cầu | Xin gia hạn/hoàn tiền mở form có sẵn packageId; không tự đặt buổi học thường |
| `/student/bookings` | Chuyển danh sách/tuần, lọc thời gian/trạng thái; mỗi buổi tên gia sư, môn, giờ, trạng thái | Chi tiết buổi. Có GET list; chỉ thể hiện lịch theo khoảng đã tải, không giả định trang đầu là toàn bộ lịch |
| `/student/bookings/[id]` (mới, cần API detail) | Thông tin buổi, địa điểm/link nếu được API trả, báo cáo buổi, đánh giá | STUDENT không có API tự hủy hiện tại; đánh giá theo điều kiện BE. MVP giữ drawer từ list nếu đủ dữ liệu, ghi rõ giới hạn deep link |
| `/student/assignments` | Cần nộp/đã nộp/đã chấm; dòng bài, môn/gia sư, hạn nộp | `/student/assignments/[id]`; list API đã có |
| `/student/assignments/[id]` | Đề và tài liệu trên, vùng nộp bên cạnh/dưới; kết quả chấm khi có | Nộp qua endpoint hiện có; API đọc chi tiết và dữ liệu kết quả cần bổ sung/đối chiếu |
| `/student/checkout/[invoiceId]` | Tóm tắt hóa đơn + thanh toán; trạng thái chờ/xử lý/thất bại/hết hạn | Dùng POST invoices và GET invoices/id; không tự quyết định thành công từ URL |
| `/student/payment-result/[invoiceId]` | Kết quả đã đối chiếu BE, gói được cấp khi thành công | “Xem gói học”, “Tiếp tục kiểm tra”. Callback cũ gom về một luồng canonical |
| `/student/requests` | Tabs Hoàn tiền/Gia hạn; dòng lý do, gói, ngày, trạng thái, phản hồi | Mở chi tiết từ dữ liệu list. Học thử thêm tab khi có API danh sách của học viên |
| `/student/messages` | Danh sách hội thoại + vùng chat + tóm tắt đối tượng; mobile list/detail | REST đọc, WebSocket gửi theo contract hiện có; không truy cập hội thoại người khác |
| `/student/notifications` | Chưa đọc/Tất cả; mỗi mục nội dung, ngày, trạng thái | Click tới thực thể đúng quyền; đánh dấu đọc theo API |
| `/student/profile` | Thông tin tài khoản; liên hệ phụ huynh thành section riêng | PUT parent-contact đã có; chỉnh hồ sơ cá nhân tổng quát cần API riêng nếu chưa có |

Không tạo ví học viên vì BE hiện thể hiện mua bằng hóa đơn, ví thuộc gia sư. Trang lịch sử hóa đơn đầy đủ là mở rộng: chưa thấy GET danh sách invoices, không dựng từ localStorage làm lịch sử chính thức.

## 6. Khu gia sư

Sidebar chia nhóm: Công việc (Tổng quan, Lịch dạy, Học thử, Học viên, Bài tập); Hồ sơ & dịch vụ (Hồ sơ, Môn, Tài liệu, Gói học, Lịch rảnh); Thu nhập (Ví, Rút tiền, Ngân hàng); Trao đổi (Tin nhắn, Thông báo). Thống kê có thể là mục con của Tổng quan. Không mở rộng tất cả nhóm cùng lúc trên màn hình nhỏ.

| Trang đích | Bố cục và nội dung | API / điều kiện triển khai |
|---|---|---|
| `/teacher` (mới) | Hôm nay dạy gì, học thử cần phản hồi, bài cần chấm; thống kê gọn | Stats có; các danh sách lịch/bài cần bổ sung API đọc. Không hiển thị 0 cho nguồn chưa có |
| `/teacher/profile` | Checklist hoàn thiện + form theo nhóm + preview hồ sơ riêng; trạng thái xét duyệt và lý do từ chối | GET/PUT/submit profile có; map chính xác field DTO |
| `/teacher/documents` | Danh sách tài liệu, loại, trạng thái nếu có, tải lên/xóa | POST/DELETE có; lấy danh sách từ profile nếu DTO có, nếu không cần read endpoint; không public hóa tài liệu riêng |
| `/teacher/subjects` | Môn đang dạy + tìm/thêm môn đã có | GET/POST/DELETE teacher subjects có |
| `/teacher/subject-proposals` | Form đề xuất và lịch sử duyệt | GET/POST đã có; link từ “Không tìm thấy môn?” |
| `/teacher/availability` | Lịch tuần chỉnh giờ rảnh; nút lưu rõ; giải thích đây là thời gian nhận dạy lặp lại | GET/PUT có; tách khỏi lịch buổi đã đặt |
| `/teacher/packages` | Bảng/card gói: tên, môn, số buổi, giá, thời hạn, trạng thái | Tạo/sửa/đổi trạng thái có; thiếu GET private list, public list không thay được gói draft/inactive |
| `/teacher/packages/create`, `/teacher/packages/[id]` | Form chia thông tin gói, buổi học, giá/hạn; xem trước card | Cần read detail cho refresh trang sửa; version theo DTO để xử lý xung đột |
| `/teacher/bookings` (mới) | Lịch tuần/list, lọc ngày/trạng thái, CTA tạo buổi | POST/create/complete/cancel có; thiếu GET lịch gia sư |
| `/teacher/bookings/[id]` (mới) | Thông tin buổi, học viên/gói, tài liệu nếu có; hoàn thành với báo cáo hoặc hủy có lý do | Cần GET detail và quyền sở hữu; CTA theo trạng thái BE |
| `/teacher/trial-requests` (mới) | Hàng yêu cầu: học viên, môn, giờ mong muốn, lời nhắn; panel chấp nhận/từ chối | GET/accept/reject có; chấp nhận tạo booking theo BE, không chỉ đổi nhãn local |
| `/teacher/students` (mới) | Danh sách học viên có quan hệ học; môn/gói và buổi còn lại khi API trả | Cần API danh sách/chi tiết học viên và gói thuộc gia sư; không phải danh sách mọi user |
| `/teacher/students/[id]` (mới) | Hồ sơ học tập trong phạm vi gia sư, gói, lịch, bài tập; CTA tạo buổi/giao bài | Cần read model và ownership; không lộ thông tin không cần thiết |
| `/teacher/assignments`, `/teacher/assignments/new`, `/teacher/assignments/[id]` | Danh sách → form giao bài → nội dung và bài nộp; số cần chấm nếu API trả | POST giao bài có; GET list/detail chưa thấy controller |
| `/teacher/submissions/[id]` | Bài làm/file bên trái; điểm/nhận xét bên phải | POST grade có; GET submission detail cần bổ sung |
| `/teacher/wallet` | Số dư theo từng loại BE trả, lịch sử ledger, CTA rút tiền | GET wallet/ledger có; không gộp số dư khả dụng và khoản chưa rút được |
| `/teacher/payouts` | Yêu cầu mới + danh sách và tiến trình xử lý | POST/GET có; số tiền/điều kiện theo BE |
| `/teacher/bank-accounts` | Tài khoản nhận tiền, thêm/sửa/xóa; số tài khoản che bớt khi xem | CRUD hiện có |
| `/teacher/stats` | Chỉ số và thứ hạng từ stats; giải thích điểm | GET có; không thêm đồ thị tháng nếu response chỉ có tổng |
| `/teacher/messages`, `/teacher/notifications` | Cùng interaction với học viên, dữ liệu theo người đăng nhập | API communication hiện có |

## 7. Khu admin

Một AdminLayout dùng chung với guard; route canonical `/admin`, redirect `/admin/dashboard` nếu giữ tương thích. Sidebar: Tổng quan · Duyệt gia sư · Đề xuất môn · Hoàn tiền · Gia hạn · Rút tiền · Cài đặt · Nhật ký. Không giả định admin có quyền đọc toàn bộ chat/bài học.

| Trang | Layout và nội dung | Hành động / giới hạn |
|---|---|---|
| `/admin` | Chỉ số thật từ dashboard, hàng đợi cần xử lý, liên kết đến từng loại | Không tạo biểu đồ xu hướng từ một giá trị tổng |
| `/admin/teachers` | Bảng chờ duyệt, filter có API hỗ trợ; bên phải panel hồ sơ/tài liệu | Duyệt/từ chối kèm lý do; cần đảm bảo approval response đủ dữ liệu panel, không dùng public profile cho hồ sơ pending |
| `/admin/subjects` | Ghi tiêu đề “Đề xuất môn học”; bảng đề xuất và nội dung đối chiếu | API duyệt/từ chối đã có; CRUD danh mục môn trực tiếp là phạm vi mới |
| `/admin/refunds` | Bảng yêu cầu, panel số tiền/gói/lý do và tiến trình | approve/reject/complete theo trạng thái, xác nhận trước hành động |
| `/admin/extensions` | Bảng gói/yêu cầu/ngày; panel lý do | approve/reject theo API |
| `/admin/payouts` | Bảng yêu cầu rút tiền; thông tin cần đối soát, lịch sử trạng thái | process/complete/reject, không coi nhấn “hoàn tất” là chuyển ngân hàng tự động |
| `/admin/settings` | Form chia nhóm đúng field DTO, mô tả tác động, lưu/xung đột/lỗi | GET/PUT có; không thêm setting chỉ lưu local |
| `/admin/audit-logs` | Bộ lọc + bảng thời gian/người thao tác/đối tượng; mở snapshot nếu API trả | Read-only |
| `/admin/users` (mở rộng) | Tìm người dùng, trạng thái, panel quản lý | PATCH status có, chưa thấy GET user list/detail; cần BE trước khi mở menu |

Các thao tác nhỏ và chi tiết đủ từ list dùng drawer/modal. Chỉ tạo route detail riêng khi cần deep link hoặc khối nội dung lớn và có API đọc trực tiếp; không tạo trang chi tiết giả dựa duy nhất vào navigation state.

## 8. Khoảng trống BE cần chốt

### Ưu tiên để luồng end-to-end dùng được

1. Chuẩn hóa giá gói/giá buổi và trường null khi gia sư chưa có gói; đồng nhất filter giá với nhãn UI.
2. GET lịch gia sư và GET booking detail; API đọc học viên/gói thuộc gia sư để tạo lịch đúng đối tượng.
3. GET private teacher packages list/detail, bao gồm draft/inactive; không lấy public list thay thế.
4. GET teacher assignments/detail, submission detail; GET student assignment detail và trạng thái bài nộp/kết quả phù hợp.
5. GET danh sách/chi tiết yêu cầu học thử của học viên để xem lại sau refresh; hiện mới có POST phía học viên.
6. Rà DTO profile/documents/admin approvals để panel đọc được đủ dữ liệu đúng quyền; public detail badge và subjectId cần contract rõ.

### Có thể để sau

- GET lịch sử hóa đơn; GET/PUT hồ sơ cá nhân; GET admin users.
- Bắt đầu hội thoại tư vấn trước mua, favorite gia sư, so sánh lưu lâu dài.
- Chi tiết môn theo slug, số gia sư từng môn, lọc vị trí, bài viết/lộ trình, dashboard phụ huynh.
- Matching chấm điểm/cá nhân hóa. MVP dùng search/filter hiện có và gọi đúng tên.

Các endpoint mới ở trên là đề xuất, chưa phải tính năng BE đang sẵn sàng. Những màn hình phụ thuộc phải có trạng thái “chưa triển khai” trong môi trường phát triển hoặc chưa đưa vào menu phát hành; không fake success hoặc dùng mảng rỗng để che API 404.

## 9. Luồng quan trọng

### Học viên mới

Homepage → môn/danh sách → hồ sơ → chọn gói → login nếu cần → xác nhận tạo hóa đơn → thanh toán → BE xác nhận → gói đã mua → gia sư sắp lịch → học viên xem lịch → học → xem báo cáo/đánh giá.

Nhánh học thử: hồ sơ → gửi yêu cầu → gia sư chấp nhận/từ chối → xem buổi học được tạo nếu chấp nhận. Lịch mong muốn không phải đặt chỗ thành công.

### Gia sư mới

Đăng ký TEACHER → xác minh → soạn hồ sơ/môn/tài liệu → gửi duyệt → admin duyệt hoặc trả lý do → mở dịch vụ theo điều kiện BE → quản lý gói/lịch rảnh → nhận yêu cầu → dạy/giao bài/chấm → xem ví/rút tiền.

### Xử lý ngoại lệ

Thanh toán pending có nút kiểm tra lại; hết hạn không hiện thành công. Gói hết hạn/hết buổi chỉ mở yêu cầu phù hợp. Booking xung đột tải lại dữ liệu và chọn giờ khác. 403 giữ người dùng ở trang giải thích quyền; 404 thực thể có link trở về danh sách; mất mạng giữ dữ liệu form và cho retry.

## 10. Kế hoạch thực hiện

| Đợt | Công việc | Điều kiện hoàn thành |
|---|---|---|
| 1. Nền tảng/contract | DTO/adapters; sửa giá; CSS strategy; route map; guard và trạng thái; thống nhất shell public/student/teacher/admin | Menu không link chết; không có giá sai đơn vị; không trộn trạng thái tài khoản/hồ sơ |
| 2. Marketplace | Homepage gọn; thẻ gia sư/môn; tìm/lọc/phân trang; hồ sơ/gói/lịch/review; login returnTo | Guest khám phá trọn luồng; URL share được; mobile không tràn |
| 3. Học thử và mua gói | Gửi/duyệt học thử; tạo hóa đơn; kết quả theo BE; gói học viên | Thành công/chờ/lỗi/hết hạn đều có bước tiếp; không cấp gói dựa trên query string |
| 4. Không gian học/dạy | Bổ sung read API ưu tiên; dashboard; lịch/detail; học viên của gia sư; bài tập/nộp/chấm; chat/thông báo | Hai tài khoản đúng vai trò hoàn thành được một buổi và một bài tập |
| 5. Vận hành | Shell admin; duyệt hồ sơ/môn; hoàn/gia hạn/rút; ví; settings/audit | Thao tác theo state BE, lỗi/xung đột có giải thích và dữ liệu cập nhật |
| 6. Hoàn thiện | Responsive, focus, keyboard, contrast, loading/empty/error; tối ưu ảnh/truy vấn; kiểm tra link/deep link | Các trang chính dùng được trên 360px và desktop; refresh detail không mất dữ liệu |

Chốt UI theo lát cắt hoàn chỉnh (tìm gia sư → mua → xem gói) trước khi dàn đều hàng chục trang. Các phần BE còn thiếu có thể triển khai song song theo backlog, nhưng không tính màn hình chỉ có mock là đã hoàn thành.

## 11. Tiêu chí nghiệm thu

- Mỗi card/list có đích đến rõ; không nhồi hồ sơ dài lên homepage.
- Tổng số, badge, giá, số buổi, trạng thái lấy đúng field BE; null/empty phân biệt lỗi.
- Bộ lọc gửi đúng enum; API page bắt đầu 0, UI bắt đầu 1; back/refresh giữ trạng thái.
- Mỗi role truy cập trực tiếp URL đúng/sai quyền đều cho kết quả rõ ràng; BE chặn IDOR và chuyển trạng thái không hợp lệ.
- Mọi thao tác mutate có loading, chống gửi lặp, phản hồi thành công/lỗi, cập nhật dữ liệu liên quan.
- Thông báo đưa đến đúng đối tượng, giữ quyền truy cập; chat mất kết nối có báo trạng thái.
- Xác nhận thanh toán lấy từ server; các khoản tiền hiển thị đúng đơn vị và ý nghĩa.
- Có trạng thái người dùng mới, gia sư chờ duyệt/bị từ chối, không có gói, không có lịch và không có đánh giá.
- Kiểm tra bằng trình duyệt thật sau triển khai ở 360/390/768/1280px, cùng keyboard navigation. Bản kế hoạch này chưa thay thế việc QA đó.

## 12. Nguồn đối chiếu chính

- FE: src/app/(public), src/app/student, src/app/teacher, src/app/admin; shared/components/layouts/Navbar.tsx và Sidebar.tsx; shared/components/layout/StudentAppLayout.tsx; features/marketplace/components/TeacherGrid.tsx và TeacherPackagesTab.tsx; shared/components/data-display/TeacherCard.tsx; shared/api/public.ts.
- BE: catalog/dto/TeacherCard.java, TeacherPublicDetail.java, TeacherSearchParams.java, PricingPackageView.java; catalog/repository/TeacherSearchRepository.java; subject/dto/SubjectSummary.java.
- Controller thuộc auth, teacher, catalog, booking, payment, learning, communication, finance, admin và ranking; auth/domain/Role.java, UserStatus.java; teacher/domain/ProfileStatus.java.

Các thay đổi ứng dụng chưa commit sẵn có trong workspace không bị chỉnh sửa bởi bản kế hoạch này.
