# ĐẶC TẢ KỸ THUẬT ĐỒ ÁN LẬP TRÌNH WEB
**Nền tảng Quản lý Gia sư & Lớp học trực tuyến 1-1**
- **Phiên bản:** 1.0
- **Mô hình:** EdTech Marketplace
- **Thời gian thực hiện:** 8 tuần

## 1. Tổng quan
### 1.1. Mục tiêu
Hệ thống là marketplace kết nối trực tiếp học sinh và giáo viên:
- Học sinh tìm kiếm giáo viên, xem lịch rảnh, mua gói học và theo dõi quá trình học.
- Giáo viên quản lý hồ sơ, môn dạy, gói học, lịch học, bài tập và thu nhập.
- Nền tảng thu hoa hồng 5% trên giá trị thực tế của các buổi học đã hoàn thành.
- Admin duyệt giáo viên, môn học mới, refund, gia hạn gói, payout và xử lý vi phạm.
- payOS được dùng để thu tiền tự động qua VietQR.
- Payout cho giáo viên và refund cho học sinh được Admin chuyển khoản thủ công.

### 1.2. Phạm vi MVP
Bao gồm:
- Đăng ký, đăng nhập, JWT và Google OAuth2.
- Duyệt hồ sơ giáo viên.
- Danh mục môn học dùng chung.
- Hồ sơ và lịch rảnh giáo viên.
- Tìm kiếm, lọc và xếp hạng giáo viên.
- Gói học theo số buổi.
- Thanh toán payOS.
- Booking 1-1 do giáo viên tạo.
- Buổi học thử miễn phí.
- Báo cáo buổi học.
- Bài tập, bài nộp và file đính kèm.
- Chat 1-1 và notification.
- Wallet, ledger, payout và refund.
- Dashboard quản trị.
- Scheduled jobs và kiểm soát concurrency.

### 1.3. Ngoài phạm vi
- Multi-tenancy, trung tâm và Tenant Admin.
- Subscription Free/Pro.
- Lớp nhóm hoặc chat nhóm.
- Học sinh tự tạo Booking.
- Tự động chuyển payout bằng API ngân hàng/payOS.
- Tự động tạo phòng Zoom hoặc Google Meet.
- Livestream/video call tích hợp trực tiếp.
- Ví điện tử pháp lý hoặc hệ thống kế toán hoàn chỉnh.

## 2. Công nghệ
### 2.1. Backend
- Java 21.
- Spring Boot 3.
- Spring Web.
- Spring Security.
- Spring Data JPA/Hibernate.
- Bean Validation.
- Spring WebSocket/STOMP.
- Spring Mail.
- Flyway Migration.
- PostgreSQL Driver.
- Redis.
- Testcontainers.
- JUnit 5, Mockito.

### 2.2. Frontend
- React.
- TypeScript.
- Vite.
- Ant Design.
- React Router.
- TanStack Query.
- Axios.
- STOMP.js.
- React Hook Form.

### 2.3. Hạ tầng
- PostgreSQL lưu dữ liệu chính.
- Redis lưu cache, rate-limit và trạng thái WebSocket tạm thời.
- Cloudinary lưu avatar, chứng chỉ, tài liệu và chứng từ chuyển khoản.
- payOS tạo Payment Link/VietQR và gửi webhook.
- Docker Compose chạy Backend, Frontend, PostgreSQL và Redis.

### 2.4. Quy ước
- Thời gian lưu trong database dưới dạng UTC.
- Hiển thị theo múi giờ Asia/Ho_Chi_Minh.
- Tiền tệ chỉ dùng VND, lưu dưới dạng số nguyên, không dùng float/double.
- ID public dùng UUID.
- API trao đổi JSON UTF-8.
- Ngày giờ API dùng ISO-8601, ví dụ `2026-08-18T09:00:00+07:00`.

## 3. Kiến trúc hệ thống
```text
React Frontend
      │
      ├── REST/JSON
      └── WebSocket/STOMP
              │
Spring Boot Application
      │
      ├── Controller
      ├── Application/Service
      ├── Domain
      ├── Repository
      └── Integration Adapter
              │
      ├── PostgreSQL
      ├── Redis
      ├── Cloudinary
      ├── payOS
      └── Email Provider
```

### 3.1. Các module Backend
- **auth:** tài khoản, JWT, refresh token, OAuth2.
- **user:** hồ sơ cơ bản và trạng thái tài khoản.
- **teacher:** hồ sơ, chứng chỉ, availability.
- **subject:** danh mục môn học và đề xuất môn mới.
- **package:** PricingPackage và StudentPackage.
- **payment:** Invoice, payOS và webhook.
- **booking:** lịch học, conflict checking, SessionReport.
- **learning:** Assignment, Submission, Attachment.
- **communication:** Chat, Message, Notification.
- **finance:** Wallet, Ledger, Payout, Refund.
- **ranking:** Review, TeacherStats và Bayesian Average.
- **admin:** duyệt, cấu hình, khiếu nại và dashboard.
- **scheduler:** các tác vụ định kỳ.
- **common:** exception, response DTO, auditing và security utilities.

### 3.2. BaseEntity
Các entity nghiệp vụ thông thường kế thừa:
```text
BaseEntity
- id: UUID
- created_at: Instant
- updated_at: Instant
- is_deleted: boolean
```
- Không có `tenant_id`.
- Các bảng tài chính như `ledger_entries`, `payment_transactions` và `audit_logs` không sử dụng soft delete.

## 4. Vai trò và phân quyền

| Chức năng | Guest | Student | Teacher | Admin |
| :--- | :--- | :--- | :--- | :--- |
| Xem danh mục môn học | Có | Có | Có | Có |
| Tìm và xem giáo viên | Có | Có | Có | Có |
| Xem bảng xếp hạng | Có | Có | Có | Có |
| Mua gói học | Không | Có | Không | Không |
| Tạo Booking | Không | Không | Có | Không |
| Hủy/hoàn thành Booking | Không | Không | Có | Không |
| Quản lý PricingPackage | Không | Không | Có | Không |
| Đề xuất môn mới | Không | Không | Có | Không |
| Làm/nộp bài tập | Không | Có | Không | Không |
| Giao/chấm bài | Không | Không | Có | Không |
| Yêu cầu refund/gia hạn | Không | Có | Không | Không |
| Yêu cầu payout | Không | Không | Có | Không |
| Duyệt giáo viên/môn học | Không | Không | Không | Có |
| Duyệt refund/payout | Không | Không | Không | Có |
| Khóa tài khoản | Không | Không | Không | Có |
| Cấu hình hoa hồng/ranking | Không | Không | Không | Có |

**Quyền sở hữu phải được kiểm tra trong Service.** Việc biết UUID của tài nguyên không cho phép người dùng truy cập tài nguyên của người khác.

## 5. Xác thực và tài khoản
### 5.1. Đăng ký
Người dùng chọn một trong hai loại tài khoản:
- `STUDENT`
- `TEACHER`

Admin không được đăng ký công khai; tài khoản Admin được seed hoặc tạo bằng quy trình nội bộ.

Với tài khoản `STUDENT`, request đăng ký có thể gửi thêm `parentFullName`, `parentPhone`, `parentEmail`; cả ba field đều không bắt buộc. Nếu có ít nhất `parentEmail` hoặc `parentPhone`, hệ thống tự đặt `notify_parent = true`. Student có thể cập nhật hoặc xóa các thông tin này sau khi đăng ký.

Không có role `PARENT` và không có bảng liên kết phụ huynh–học sinh. Phụ huynh không đăng nhập hệ thống; thông tin trên chỉ là kênh liên hệ bị động. MVP chỉ gửi email tới `parent_email` khi đủ điều kiện, không tự động gửi SMS; `parent_phone` chỉ dùng để tham khảo hoặc liên hệ thủ công qua Admin.

### 5.2. Trạng thái User
- `PENDING_VERIFICATION`
- `ACTIVE`
- `LOCKED`
- `DISABLED`

- Tài khoản `LOCKED` không được đăng nhập hoặc refresh token.
- Khóa tài khoản phải lưu lý do, Admin thực hiện và thời điểm thực hiện.
- Teacher có `User.status = ACTIVE` vẫn phải có `TeacherProfile.status = APPROVED` mới được xuất hiện trên marketplace.

### 5.3. Token
- Access token: 15 phút.
- Refresh token: 7 ngày.
- Refresh token được rotate sau mỗi lần sử dụng.
- Chỉ lưu hash refresh token trong database.
- Logout thu hồi refresh token hiện tại.
- Thay đổi mật khẩu hoặc khóa tài khoản thu hồi toàn bộ refresh token.

### 5.4. Google OAuth2
- Tài khoản Google mới phải chọn vai trò Student hoặc Teacher.
- Nếu chọn Teacher, hệ thống tạo TeacherProfile ở trạng thái `DRAFT`.
- Không tự động liên kết Google account với tài khoản email/password nếu chưa xác minh quyền sở hữu.

## 6. Mô hình dữ liệu
### 6.1. User và Teacher
- **users**
  - id
  - email, unique, lowercase
  - password_hash, nullable với OAuth-only account
  - full_name
  - avatar_url
  - phone
  - parent_full_name, nullable
  - parent_phone, nullable
  - parent_email, nullable
  - notify_parent, boolean, nullable, mặc định `false`; tự động `true` khi đăng ký Student có `parent_email` hoặc `parent_phone`
  - role: `STUDENT` | `TEACHER` | `ADMIN`
  - status
  - email_verified
  - oauth_provider
  - oauth_subject
  - last_login_at
  - audit fields

- **refresh_tokens**
  - id
  - user_id
  - token_hash, unique
  - expires_at
  - revoked_at
  - device_info
  - ip_address

- **teacher_profiles**
  - id
  - user_id, unique
  - bio
  - years_of_experience
  - languages, varchar[]
  - supports_online
  - supports_offline
  - location_address
  - introduction_video_url
  - profile_status
  - rejection_reason
  - verified_badge
  - is_visible
  - approved_at
  - approved_by

Trạng thái:
```text
DRAFT → PENDING_APPROVAL → APPROVED
                         → REJECTED
REJECTED → PENDING_APPROVAL
APPROVED → DRAFT
```
Nếu Teacher sửa các trường nhạy cảm như chứng chỉ hoặc thông tin định danh, hồ sơ có thể được đưa về `PENDING_APPROVAL`.

- **teacher_documents**
  - id
  - teacher_id
  - document_type: `DEGREE` | `CERTIFICATE` | `IDENTITY` | `OTHER`
  - title
  - cloudinary_public_id
  - secure_url
  - mime_type
  - file_size
  - verification_status
  - verified_by
  - verified_at

- **teacher_availabilities**
  - id
  - teacher_id
  - day_of_week: `MONDAY` ... `SUNDAY`
  - start_time
  - end_time
  - timezone, mặc định Asia/Ho_Chi_Minh
  - is_active

**Ràng buộc:**
- `start_time` < `end_time`.
- Các khoảng active của cùng Teacher trong cùng ngày không được overlap.
- Availability là thông tin tham khảo, không phải hard constraint khi tạo Booking.

### 6.2. Môn học
- **subjects**
  - id
  - code, unique
  - name
  - slug, unique
  - education_level
  - description
  - is_active
  - created_source: `SEED` | `ADMIN` | `TEACHER_PROPOSAL`

- **subject_proposals**
  - id
  - teacher_id
  - proposed_name
  - education_level
  - description
  - status: `PENDING` | `APPROVED` | `REJECTED`
  - review_note
  - reviewed_by
  - reviewed_at
  - created_subject_id, nullable

**Khi duyệt:**
- Admin kiểm tra môn tương tự.
- Nếu đã có Subject phù hợp, liên kết đề xuất vào Subject đó.
- Nếu chưa có, tạo Subject mới.
- Tự động tạo TeacherSubject.
- Cập nhật proposal thành `APPROVED`.

- **teacher_subjects**
  - id
  - teacher_id
  - subject_id
  - level_description
  - experience_description
  - is_active

Unique: (`teacher_id`, `subject_id`).

### 6.3. PricingPackage
- **pricing_packages**
  - id
  - teacher_id, bắt buộc
  - subject_id, bắt buộc
  - name
  - description
  - total_sessions
  - duration_days
  - price_vnd
  - session_duration_minutes
  - status: `DRAFT` | `ACTIVE` | `INACTIVE`
  - version

**Business rules:**
- TeacherProfile phải `APPROVED`.
- Subject phải nằm trong TeacherSubject đang active.
- total_sessions > 0.
- duration_days > 0.
- price_vnd > 0.
- Không sửa giá, số buổi, thời hạn hoặc môn của gói đã được mua.
- Khi gói đã phát sinh StudentPackage, Teacher chỉ được ngừng bán và tạo gói mới.

### 6.4. Invoice và Payment
- **invoices**
  - id
  - invoice_number, unique
  - student_id
  - teacher_id
  - pricing_package_id
  - amount_vnd
  - status: `PENDING` | `PAID` | `CANCELLED` | `EXPIRED`
  - payos_order_code, unique
  - payos_payment_link_id, unique, nullable trước khi tạo link thành công
  - checkout_url
  - qr_code
  - payment_expired_at
  - paid_at

- **payment_transactions**
  - id
  - invoice_id
  - provider: `PAYOS`
  - provider_reference, unique
  - order_code
  - amount_vnd
  - transaction_datetime
  - raw_payload, JSONB
  - signature_valid
  - processed_at

**Webhook phải kiểm tra:**
- Chữ ký hợp lệ.
- orderCode tồn tại.
- Invoice đang `PENDING` hoặc đã `PAID`.
- Số tiền bằng chính xác `Invoice.amount_vnd`.
- Provider reference chưa được xử lý.
- Webhook lặp lại phải trả HTTP 2xx nhưng không tạo StudentPackage hoặc ledger lần thứ hai.
- Return URL từ trình duyệt không được dùng để xác nhận thanh toán. Nguồn xác nhận duy nhất là webhook hợp lệ hoặc kết quả đối soát server-to-server với payOS.

### 6.5. StudentPackage
- **student_packages**
  - id
  - student_id
  - teacher_id
  - subject_id
  - pricing_package_id
  - invoice_id, unique
  - package_name_snapshot
  - total_sessions
  - remaining_sessions
  - reserved_sessions
  - completed_sessions
  - refunded_sessions
  - purchase_price_vnd
  - commission_rate, mặc định snapshot 5.00
  - starts_at
  - expires_at
  - status
  - locked_reason
  - version

**Trạng thái:**
```text
PENDING_PAYMENT → ACTIVE
ACTIVE → COMPLETED
ACTIVE → REFUND_PENDING → REFUNDED
ACTIVE → LOCKED_EXPIRED
LOCKED_EXPIRED → ACTIVE             (gia hạn)
LOCKED_EXPIRED → REFUND_PENDING
REFUND_PENDING → ACTIVE             (refund bị từ chối)
REFUND_PENDING → LOCKED_EXPIRED     (refund bị từ chối với gói hết hạn)
```

**Bất biến:**
- `total_sessions` = `remaining_sessions` + `reserved_sessions` + `completed_sessions` + `refunded_sessions`
- Tạo Booking: `remaining_sessions - 1`, `reserved_sessions + 1`.
- Hoàn thành: `reserved_sessions - 1`, `completed_sessions + 1`.
- Hủy/expired: `reserved_sessions - 1`, `remaining_sessions + 1`.
- Refund: chuyển số lượt được hoàn từ `remaining_sessions` sang `refunded_sessions`.
- Khi `completed_sessions` = `total_sessions`, trạng thái là `COMPLETED`.
- Booking phải có `end_time <= expires_at`.
- Khi gói chuyển `LOCKED_EXPIRED`, các Booking `SCHEDULED` đã tạo trước thời điểm hết hạn vẫn được giữ nguyên và diễn ra bình thường vì lượt học đã được chuyển sang `reserved_sessions`. `LOCKED_EXPIRED` chỉ chặn tạo Booking mới, không tự động hủy Booking đã lên lịch.

### 6.6. Booking
- **bookings**
  - id
  - teacher_id
  - student_id
  - student_package_id, nullable nếu trial
  - subject_id
  - start_time
  - end_time
  - delivery_mode: `ONLINE` | `OFFLINE`
  - meeting_link
  - location_address
  - status: `SCHEDULED` | `COMPLETED` | `CANCELLED` | `EXPIRED`
  - is_trial
  - outside_availability_warning
  - cancel_reason
  - cancel_initiated_by: `TEACHER` | `STUDENT_REQUEST` | `SYSTEM`
  - completed_at
  - cancelled_at
  - expired_at
  - settlement_processed
  - version

**Validation:**
- `start_time` < `end_time`.
- Không tạo Booking trong quá khứ.
- Teacher và Student phải active.
- Teacher phải dạy Subject tương ứng.
- Booking chính thức phải dùng StudentPackage của đúng Student, Teacher và Subject.
- StudentPackage phải `ACTIVE`.
- Trial Booking không có StudentPackage.
- Không overlap lịch Teacher hoặc Student.
- Khoảng thời gian sử dụng quy ước `[start_time, end_time)`, vì vậy buổi 09:00–10:00 không conflict với buổi 10:00–11:00.

- **session_reports**
  - id
  - booking_id, unique
  - record_link
  - content
  - feedback
  - follow_up_note
  - teacher_self_rating
  - submitted_at

Endpoint hoàn thành Booking phải tạo SessionReport và chuyển trạng thái Booking trong cùng transaction. Không tồn tại trạng thái `COMPLETED` nhưng thiếu SessionReport.

**Trial Session**
- Tối đa một Trial đang `SCHEDULED` hoặc đã `COMPLETED` cho mỗi cặp Student–Teacher.
- Trial `CANCELLED`/`EXPIRED` không tiêu thụ quyền học thử.
- Trial không tạo ledger, commission hoặc wallet balance.
- Trial vẫn có SessionReport nếu `COMPLETED`.

### 6.7. Review và Ranking
- **reviews**
  - id
  - booking_id, unique
  - student_id
  - teacher_id
  - rating, từ 1 đến 5
  - comment
  - is_visible
  - moderated_by
  - created_at

Chỉ Student của Booking `COMPLETED` được đánh giá.

- **teacher_stats**
  - teacher_id, PK
  - average_rating
  - bayesian_rating
  - review_count
  - completed_session_count
  - completion_rate
  - trial_session_count
  - trial_conversion_rate
  - global_rank
  - calculated_at

**Công thức:**
`Bayesian Rating = (v / (v + m)) × R + (m / (v + m)) × C`
- R: rating trung bình Teacher.
- v: số review hợp lệ.
- C: rating trung bình toàn hệ thống.
- m: ngưỡng tối thiểu do Admin cấu hình.
- Xếp hạng giảm dần theo Bayesian Rating.
- Nếu bằng nhau: ưu tiên số buổi hoàn thành, sau đó completion rate.

### 6.8. Wallet và Ledger
- **wallets**
  Mỗi Teacher có đúng một Wallet:
  - id
  - teacher_id, unique
  - pending_balance_vnd
  - available_balance_vnd
  - reserved_balance_vnd
  - version

- **ledger_entries**
  - id
  - wallet_id
  - entry_type
  - amount_vnd
  - balance_bucket: `PENDING` | `AVAILABLE` | `RESERVED`
  - direction: `CREDIT` | `DEBIT`
  - reference_type
  - reference_id
  - idempotency_key, unique
  - description
  - created_at

Các loại bút toán:
- `PACKAGE_FUNDED`
- `SESSION_RELEASE_PENDING`
- `SESSION_CREDIT_AVAILABLE`
- `COMMISSION_RECOGNIZED`
- `PAYOUT_RESERVED`
- `PAYOUT_SUCCEEDED`
- `PAYOUT_RELEASED`
- `REFUND_DEBIT_PENDING`
- `ADJUSTMENT`

Ledger là append-only. Không update hoặc delete bút toán cũ; sai sót được sửa bằng bút toán đảo.

**Quy tắc tính tiền**
Ví dụ gói 1.000.000 VND, 10 buổi, commission 5%:
- Gross mỗi buổi dự kiến: 100.000 VND.
- Platform commission: 5.000 VND.
- Teacher net: 95.000 VND.

Sau mỗi Booking `COMPLETED`:
- Pending của Teacher giảm 95.000.
- Available của Teacher tăng 95.000.
- Platform ghi nhận commission 5.000.

Nếu giá gói không chia đều cho số buổi, dùng phép tính tích lũy và phân bổ phần dư vào lần quyết toán cuối để:
`Tổng gross các buổi + tổng refund = purchase_price_vnd`
Không được phát sinh sai lệch do làm tròn VND.

### 6.9. Payout
- **teacher_bank_accounts**
  - id
  - teacher_id
  - bank_bin
  - bank_name
  - account_number_encrypted
  - account_holder_name
  - is_verified
  - is_default

- **payout_requests**
  - id
  - teacher_id
  - wallet_id
  - bank_account_id
  - amount_vnd
  - status: `PENDING` | `PROCESSING` | `SUCCEEDED` | `REJECTED` | `FAILED`
  - teacher_note
  - admin_note
  - bank_reference
  - proof_public_id
  - proof_url
  - transferred_at
  - processed_by
  - processed_at

**Luồng:**
1. Teacher gửi yêu cầu.
2. Lock Wallet.
3. Kiểm tra amount <= available_balance.
4. Chuyển amount từ available sang reserved.
5. Admin duyệt và chuyển khoản thủ công.
6. Admin nhập mã giao dịch, thời điểm và ảnh chứng từ.
7. Hệ thống chuyển payout thành `SUCCEEDED`, giảm reserved.
8. Nếu từ chối/thất bại, reserved được trả về available.

### 6.10. Refund và gia hạn
- **refund_requests**
  - id
  - student_package_id
  - student_id
  - reason
  - requested_sessions
  - approved_sessions
  - refund_amount_vnd
  - status: `PENDING` | `APPROVED` | `PROCESSING` | `REFUNDED` | `REJECTED` | `FAILED`
  - admin_note
  - bank_name
  - bank_bin
  - account_number_encrypted
  - account_holder_name
  - bank_reference
  - proof_public_id
  - proof_url
  - processed_by
  - processed_at

**Điều kiện gửi refund:**
- Gói `ACTIVE` hoặc `LOCKED_EXPIRED`.
- remaining_sessions > 0.
- Không có Booking `SCHEDULED`.
- Không có RefundRequest đang xử lý.
- Khi gửi yêu cầu, gói chuyển `REFUND_PENDING`, ngăn tạo Booking mới.
- Admin không được duyệt số buổi lớn hơn remaining_sessions.

**Công thức tính tiền refund:**
- `refund_amount_vnd = floor(approved_sessions × purchase_price_vnd / total_sessions)` theo đơn vị VND.
- Backend tính `refund_amount_vnd`; Admin không nhập tùy ý số tiền refund.
- Nếu một gói được duyệt refund nhiều lần, phần dư phát sinh do làm tròn xuống được cộng dồn vào `refund_amount_vnd` của lần duyệt cuối cùng để tổng tiền refund khớp phần giá trị các lượt được hoàn.

- **package_extension_requests**
  - id
  - student_package_id
  - student_id
  - reason
  - requested_expiry_date
  - approved_expiry_date
  - status: `PENDING` | `APPROVED` | `REJECTED`
  - admin_note
  - reviewed_by
  - reviewed_at

Chỉ gói `LOCKED_EXPIRED` được yêu cầu gia hạn. Khi duyệt:
- expires_at = approved_expiry_date.
- Gói trở lại `ACTIVE`.
- Ngày mới phải lớn hơn thời điểm hiện tại.

### 6.11. Assignment và Submission
- **assignments**
  - id
  - teacher_id
  - student_id
  - subject_id
  - title
  - assignment_type: `SYSTEM_QUIZ` | `FREEFORM`
  - content_blocks, JSONB
  - quiz_schema, JSONB, nullable
  - due_at
  - status: `DRAFT` | `PUBLISHED` | `CLOSED`

- **submissions**
  - id
  - assignment_id
  - student_id
  - content_blocks, JSONB
  - submitted_at
  - status: `DRAFT` | `SUBMITTED` | `GRADED`
  - score
  - feedback_text
  - graded_at

Cấu trúc `content_blocks`:
```json
[
  {
    "type": "TEXT",
    "content": "Nội dung..."
  },
  {
    "type": "IMAGE",
    "attachmentId": "uuid"
  },
  {
    "type": "FILE",
    "attachmentId": "uuid"
  },
  {
    "type": "LINK",
    "url": "https://example.com",
    "label": "Tài liệu"
  }
]
```

- **attachments**
  - id
  - owner_id
  - attachable_type
  - attachable_id
  - cloudinary_public_id
  - secure_url
  - original_filename
  - mime_type
  - file_size

### 6.12. Chat và Notification
- **conversations**
  - id
  - teacher_id
  - student_id
  - last_message_at
  Unique: (`teacher_id`, `student_id`).

- **messages**
  - id
  - conversation_id
  - sender_id
  - message_type: `TEXT` | `IMAGE` | `FILE`
  - content
  - attachment_id
  - sent_at
  - read_at

- **notifications**
  - id
  - user_id
  - type
  - title
  - content
  - reference_type
  - reference_id
  - is_read
  - created_at

Chat chỉ được mở khi Student và Teacher có ít nhất một trong các quan hệ:
- StudentPackage hợp lệ.
- Trial Booking.
- Booking chính thức.
- Conversation đã được tạo từ một trong các quan hệ trên.

## 7. Luồng nghiệp vụ chính
### 7.1. Teacher onboarding
1. Người dùng đăng ký role Teacher.
2. Hệ thống tạo User và TeacherProfile `DRAFT`.
3. Teacher cập nhật hồ sơ, upload chứng chỉ.
4. Teacher gửi duyệt.
5. Profile chuyển `PENDING_APPROVAL`.
6. Admin duyệt hoặc từ chối kèm lý do.
7. Khi `APPROVED`, Teacher được chọn môn, tạo gói và xuất hiện công khai.

### 7.2. Đề xuất môn mới
1. Teacher tìm danh mục Subject.
2. Nếu chưa có, gửi SubjectProposal.
3. Admin kiểm tra trùng tên/nội dung.
4. Admin liên kết vào Subject cũ hoặc tạo Subject mới.
5. Hệ thống tạo TeacherSubject.
6. Teacher được phép tạo PricingPackage cho môn đó.

### 7.3. Mua gói qua payOS
1. Student chọn PricingPackage `ACTIVE`.
2. Backend snapshot giá, số buổi, thời hạn và commission.
3. Tạo Invoice `PENDING`.
4. Gọi payOS tạo Payment Link.
5. Trả checkout URL/QR cho Frontend.
6. payOS gửi webhook.
7. Backend xác minh chữ ký và số tiền.
8. Tạo PaymentTransaction.
9. Invoice chuyển `PAID`.
10. StudentPackage chuyển `ACTIVE`.
11. Tạo Wallet cho Teacher nếu chưa có.
12. Ghi net dự kiến vào pending balance.
13. Gửi notification cho Student và Teacher; nếu Student có `parent_email` và `notify_parent = true`, gửi thêm email xác nhận thanh toán thành công tới phụ huynh.
*Tất cả bước 8–12 chạy trong cùng transaction.*

### 7.4. Tạo Booking
1. Teacher chọn StudentPackage của Student.
2. Backend lock theo thứ tự:
   - Teacher/User hoặc hàng lock tương ứng.
   - Student/User.
   - StudentPackage.
3. Kiểm tra conflict Teacher.
4. Kiểm tra conflict Student.
5. Kiểm tra hạn gói và số buổi.
6. Trừ remaining_sessions, tăng reserved_sessions.
7. Tạo Booking `SCHEDULED`.
8. Nếu ngoài availability, lưu warning nhưng vẫn cho tạo.
9. Gửi notification/email cho Student.

### 7.5. Hoàn thành Booking
1. Sau end_time, Teacher gửi SessionReport.
2. Backend lock Booking, StudentPackage và Wallet.
3. Kiểm tra Booking đang `SCHEDULED`.
4. Tạo SessionReport.
5. Booking chuyển `COMPLETED`.
6. StudentPackage chuyển một lượt từ reserved sang completed.
7. Chuyển phần net của buổi từ pending sang available.
8. Ghi nhận commission của buổi.
9. Gửi báo cáo và notification cho Student; nếu Student có `parent_email` và `notify_parent = true`, gửi thêm email SessionReport tới phụ huynh.
10. Cho phép Student tạo Review.

### 7.6. Hủy và auto-expire
**Hủy thủ công:**
- Chỉ Teacher gọi API.
- Bắt buộc cancel_reason.
- Nếu Student yêu cầu qua chat, dùng `STUDENT_REQUEST`.
- Hoàn một lượt vào StudentPackage.
- Không phát sinh doanh thu.

**Auto-expire:**
- Sau end_time + 11 giờ, gửi nhắc Teacher.
- Sau end_time + 12 giờ, Booking còn `SCHEDULED` chuyển `EXPIRED`.
- Lý do: “Teacher không xác nhận buổi học trong 12 giờ”.
- Hoàn lượt vào gói.
- Không tạo SessionReport, commission hoặc thu nhập Teacher.

### 7.7. Gói sắp hết hạn
- Trước hạn 7 ngày và 3 ngày, nếu còn lượt, gửi cảnh báo cho Student và Teacher; nếu Student có `parent_email` và `notify_parent = true`, gửi thêm email cảnh báo tới phụ huynh.
- Khi đến hạn, gói chuyển `LOCKED_EXPIRED`.
- Không tạo Booking mới.
- Student chọn:
  - Yêu cầu gia hạn.
  - Yêu cầu refund.
- Admin duyệt một trong hai yêu cầu.

## 8. API contract
### 8.1. Response chuẩn
**Thành công:**
```json
{
  "success": true,
  "data": {},
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 100
  },
  "timestamp": "2026-08-18T02:00:00Z"
}
```

**Thất bại:**
```json
{
  "success": false,
  "error": {
    "code": "BOOKING_TIME_CONFLICT",
    "message": "Giáo viên đã có lịch trong khoảng thời gian này",
    "fieldErrors": []
  },
  "timestamp": "2026-08-18T02:00:00Z"
}
```

### 8.2. Auth
| Method | Endpoint | Quyền | Chức năng |
| :--- | :--- | :--- | :--- |
| POST | `/api/auth/register` | Public | Đăng ký Student/Teacher |
| POST | `/api/auth/login` | Public | Đăng nhập |
| POST | `/api/auth/refresh` | Public | Rotate refresh token |
| POST | `/api/auth/logout` | Authenticated | Thu hồi refresh token |
| POST | `/api/auth/forgot-password` | Public | Gửi email đặt lại mật khẩu |
| POST | `/api/auth/reset-password` | Public | Đặt mật khẩu mới |
| GET | `/oauth2/authorization/google` | Public | Bắt đầu Google OAuth2 |

Với `POST /api/auth/register` có `role = STUDENT`, request nhận thêm ba field optional: `parentFullName`, `parentPhone`, `parentEmail`. Request đăng ký không nhận `notifyParent`; Backend tự tính theo quy tắc tại mục 5.1.

### 8.3. Public
| Method | Endpoint | Chức năng |
| :--- | :--- | :--- |
| GET | `/api/public/subjects` | Danh mục môn học |
| GET | `/api/public/teachers` | Tìm kiếm/lọc Teacher |
| GET | `/api/public/teachers/{id}` | Hồ sơ công khai |
| GET | `/api/public/teachers/{id}/packages` | Gói đang bán |
| GET | `/api/public/teachers/{id}/availability` | Lịch rảnh |
| GET | `/api/public/teachers/{id}/reviews` | Review công khai |
| GET | `/api/public/teachers/ranking` | Xếp hạng toàn nền tảng |

**Filter Teacher:**
- keyword
- subjectId
- minPrice
- maxPrice
- minRating
- deliveryMode
- dayOfWeek
- startTime
- endTime
- page
- size
- sort

**Filter availability khớp khi:**
`availability.start_time <= requested.start_time AND availability.end_time >= requested.end_time`

### 8.4. Teacher
| Method | Endpoint | Chức năng |
| :--- | :--- | :--- |
| GET | `/api/teacher/profile` | Xem hồ sơ |
| PUT | `/api/teacher/profile` | Cập nhật hồ sơ |
| POST | `/api/teacher/profile/submit` | Gửi duyệt |
| POST | `/api/teacher/documents` | Upload chứng chỉ |
| DELETE | `/api/teacher/documents/{id}` | Xóa chứng chỉ chưa duyệt |
| GET | `/api/teacher/subjects` | Xem môn đang dạy |
| POST | `/api/teacher/subjects/{subjectId}` | Gán môn có sẵn |
| DELETE | `/api/teacher/subjects/{subjectId}` | Ngừng dạy môn |
| POST | `/api/teacher/subject-proposals` | Đề xuất môn mới |
| GET | `/api/teacher/subject-proposals` | Theo dõi đề xuất |
| POST | `/api/teacher/packages` | Tạo PricingPackage |
| PUT | `/api/teacher/packages/{id}` | Sửa gói chưa phát sinh giao dịch |
| PATCH | `/api/teacher/packages/{id}/status` | Bật/ngừng bán |
| GET | `/api/teacher/availability` | Xem availability |
| PUT | `/api/teacher/availability` | Thay toàn bộ lịch rảnh |
| POST | `/api/teacher/bookings` | Tạo Booking |
| POST | `/api/teacher/bookings/{id}/complete` | Hoàn thành và tạo report |
| POST | `/api/teacher/bookings/{id}/cancel` | Hủy Booking |
| GET | `/api/teacher/students` | Danh sách học sinh |
| POST | `/api/teacher/assignments` | Tạo bài tập |
| POST | `/api/teacher/submissions/{id}/grade` | Chấm bài |
| GET | `/api/teacher/wallet` | Xem số dư |
| GET | `/api/teacher/wallet/ledger` | Xem lịch sử ví |
| POST | `/api/teacher/payout-requests` | Yêu cầu rút tiền |
| GET | `/api/teacher/payout-requests` | Theo dõi payout |
| GET | `/api/teacher/stats` | Thống kê cá nhân |

**Ví dụ tạo Booking:**
```json
{
  "studentPackageId": "uuid",
  "startTime": "2026-08-20T19:00:00+07:00",
  "endTime": "2026-08-20T20:30:00+07:00",
  "deliveryMode": "ONLINE",
  "meetingLink": "https://meet.example/abc"
}
```

### 8.5. Student
| Method | Endpoint | Chức năng |
| :--- | :--- | :--- |
| POST | `/api/student/invoices` | Tạo đơn mua gói/payOS link |
| GET | `/api/student/invoices/{id}` | Kiểm tra trạng thái |
| GET | `/api/student/packages` | Danh sách gói đã mua |
| GET | `/api/student/packages/{id}` | Chi tiết và số buổi |
| GET | `/api/student/bookings` | Lịch học |
| POST | `/api/student/trials/requests` | Gửi yêu cầu học thử |
| POST | `/api/student/refund-requests` | Yêu cầu refund |
| POST | `/api/student/extension-requests` | Yêu cầu gia hạn |
| GET | `/api/student/assignments` | Xem bài tập |
| POST | `/api/student/assignments/{id}/submissions` | Nộp bài |
| GET | `/api/student/session-reports` | Xem báo cáo |
| POST | `/api/student/bookings/{id}/review` | Đánh giá Teacher |
| GET | `/api/student/notifications` | Xem notification |
| PUT | `/api/student/parent-contact` | Cập nhật/xóa thông tin liên hệ phụ huynh và tùy chọn nhận email |

Student không có endpoint hủy Booking trực tiếp.

`PUT /api/student/parent-contact` là phép thay thế toàn bộ thông tin liên hệ. Student có thể gửi `null` để xóa từng field; nếu xóa toàn bộ thông tin phụ huynh, Backend chuẩn hóa `notify_parent = false`.

### 8.6. Admin
| Method | Endpoint | Chức năng |
| :--- | :--- | :--- |
| GET | `/api/admin/teacher-approvals` | Hồ sơ chờ duyệt |
| POST | `/api/admin/teacher-approvals/{id}/approve` | Duyệt Teacher |
| POST | `/api/admin/teacher-approvals/{id}/reject` | Từ chối |
| GET | `/api/admin/subject-proposals` | Đề xuất môn |
| POST | `/api/admin/subject-proposals/{id}/approve` | Duyệt/liên kết Subject |
| POST | `/api/admin/subject-proposals/{id}/reject` | Từ chối |
| POST | `/api/admin/subjects` | Tạo Subject |
| PUT | `/api/admin/subjects/{id}` | Sửa Subject |
| GET | `/api/admin/refund-requests` | Danh sách refund |
| POST | `/api/admin/refund-requests/{id}/approve` | Duyệt refund |
| POST | `/api/admin/refund-requests/{id}/reject` | Từ chối |
| POST | `/api/admin/refund-requests/{id}/complete` | Xác nhận đã chuyển tiền |
| GET | `/api/admin/extension-requests` | Danh sách gia hạn |
| POST | `/api/admin/extension-requests/{id}/approve` | Duyệt gia hạn |
| POST | `/api/admin/extension-requests/{id}/reject` | Từ chối |
| GET | `/api/admin/payout-requests` | Danh sách payout |
| POST | `/api/admin/payout-requests/{id}/process` | Bắt đầu xử lý |
| POST | `/api/admin/payout-requests/{id}/complete` | Xác nhận chuyển tiền |
| POST | `/api/admin/payout-requests/{id}/reject` | Từ chối |
| PATCH | `/api/admin/users/{id}/status` | Khóa/mở tài khoản |
| GET | `/api/admin/dashboard` | Thống kê nền tảng |
| GET | `/api/admin/audit-logs` | Nhật ký quản trị |
| GET/PUT | `/api/admin/settings` | Commission, Bayesian và cron config |

### 8.7. Webhook
- `POST /api/webhooks/payos`
- Không yêu cầu JWT.
- Bắt buộc xác minh signature.
- Có rate limit riêng.
- Log request ID nhưng không log secret.
- Trả 2xx khi payload hợp lệ và đã được xử lý trước đó.

### 8.8. WebSocket
- **Handshake:** `/ws`
- **Subscribe:**
  - `/user/queue/messages`
  - `/user/queue/notifications`
  - `/topic/conversations/{conversationId}`
- **Send:**
  - `/app/chat.send`
  - `/app/chat.read`
- Server phải xác minh người gửi là thành viên Conversation.

## 9. Error codes chính
- `AUTH_INVALID_CREDENTIALS`
- `AUTH_TOKEN_EXPIRED`
- `ACCOUNT_LOCKED`
- `FORBIDDEN_RESOURCE`
- `TEACHER_NOT_APPROVED`
- `SUBJECT_NOT_ASSIGNED`
- `PACKAGE_NOT_ACTIVE`
- `PACKAGE_EXPIRED`
- `PACKAGE_NO_REMAINING_SESSION`
- `PACKAGE_HAS_SCHEDULED_BOOKING`
- `BOOKING_TIME_CONFLICT`
- `BOOKING_INVALID_STATE`
- `BOOKING_REPORT_REQUIRED`
- `TRIAL_ALREADY_USED`
- `PAYMENT_AMOUNT_MISMATCH`
- `PAYMENT_SIGNATURE_INVALID`
- `PAYMENT_ALREADY_PROCESSED`
- `REFUND_AMOUNT_EXCEEDED`
- `PAYOUT_INSUFFICIENT_BALANCE`
- `PAYOUT_INVALID_STATE`
- `FILE_TYPE_NOT_ALLOWED`
- `RATE_LIMIT_EXCEEDED`

**HTTP mapping:**
- **400:** dữ liệu hoặc state transition không hợp lệ.
- **401:** chưa xác thực/token hết hạn.
- **403:** sai role hoặc ownership.
- **404:** không tìm thấy tài nguyên.
- **409:** conflict, duplicate, concurrency.
- **422:** business rule không thỏa mãn.
- **429:** vượt rate limit.
- **500:** lỗi hệ thống không dự kiến.
- **502/503:** lỗi dịch vụ ngoài như payOS/Cloudinary.

## 10. Concurrency và transaction
### 10.1. Booking lock
Trong `@Transactional`:
1. Lock Teacher.
2. Lock Student.
3. Lock StudentPackage.
4. Query conflict.
5. Cập nhật bộ đếm session.
6. Insert Booking.

Mọi service phải giữ cùng thứ tự lock để tránh deadlock.
Ngoài Pessimistic Lock, PostgreSQL sử dụng partial exclusion constraint với `tstzrange` và extension `btree_gist` để ngăn overlap theo `teacher_id` và `student_id`. Constraint chỉ áp dụng với `WHERE (status = 'SCHEDULED' AND is_deleted = false)`; Booking `CANCELLED`, `COMPLETED`, `EXPIRED` không tham gia kiểm tra overlap.

### 10.2. Wallet lock
Các thao tác settlement, payout và refund phải:
1. Lock StudentPackage.
2. Lock Wallet.
3. Kiểm tra idempotency key.
4. Ghi ledger.
5. Cập nhật balance.
6. Commit trong một transaction.

### 10.3. Optimistic version
Các entity sau có cột `version`:
- PricingPackage.
- StudentPackage.
- Booking.
- Wallet.
- PayoutRequest.
- RefundRequest.
Nếu version không khớp, trả 409 `CONFLICT`.

## 11. Scheduled jobs

| Job | Lịch đề xuất | Chức năng |
| :--- | :--- | :--- |
| Booking reminder | Mỗi 15 phút | Nhắc Booking đã qua end time 11 giờ |
| Booking expiration | Mỗi 5 phút | Chuyển sang EXPIRED sau 12 giờ |
| Package expiration | Mỗi giờ | Khóa gói hết hạn |
| Package warning | 08:00 mỗi ngày | Nhắc trước 7 và 3 ngày |
| TeacherStats | 02:00 mỗi ngày | Tính Bayesian ranking |
| Invoice reconciliation | Mỗi 10 phút | Đối soát Invoice PENDING |
| Payment link expiration | Mỗi 10 phút | Đóng Invoice quá hạn |
| Notification cleanup | Hàng tuần | Xóa dữ liệu tạm theo retention policy |

**Mỗi job phải:**
- Có distributed lock nếu chạy nhiều instance.
- Idempotent.
- Log số bản ghi thành công/thất bại.
- Xử lý theo batch.
- Không dừng toàn bộ batch khi một bản ghi lỗi.

## 12. Cache
Redis cache cho:
- Danh sách Subject active.
- Teacher public profile.
- Kết quả global ranking.
- Search result phổ biến.
- Rate-limit counters.
- WebSocket session metadata.

**Cache invalidation:**
- Teacher được duyệt/sửa hồ sơ.
- PricingPackage bật hoặc ngừng bán.
- Availability thay đổi.
- TeacherStats được tính lại.
- Subject được thêm/sửa.

*Không cache Wallet, Ledger, StudentPackage balance hoặc trạng thái payment.*

## 13. File upload
**Giới hạn đề xuất:**
- Avatar/image: JPG, PNG, WEBP; tối đa 5 MB.
- Chứng chỉ: JPG, PNG, PDF; tối đa 10 MB.
- Bài tập: PDF, DOCX, XLSX, PPTX, ZIP, ảnh; tối đa 20 MB.
- Chứng từ payout/refund: JPG, PNG, PDF; tối đa 10 MB.

**Backend phải:**
- Kiểm tra extension và MIME type thực tế.
- Đặt tên file do hệ thống tạo.
- Không tin filename từ client.
- Lưu Cloudinary public ID để có thể quản lý file.
- Chỉ tạo Attachment sau khi upload thành công.
- Chặn quyền truy cập Attachment không thuộc người dùng.

## 14. Frontend specification
### 14.1. Public
- Landing page.
- Danh sách môn học.
- Danh sách giáo viên nổi bật.
- Search/filter giáo viên.
- Hồ sơ Teacher.
- PricingPackage.
- Availability.
- Review.
- Global ranking.

### 14.2. Student dashboard
- Tổng quan gói học.
- Số buổi còn lại/đang giữ/đã hoàn thành.
- Lịch học.
- Checkout payOS.
- Trạng thái thanh toán.
- Yêu cầu trial.
- Refund/gia hạn.
- Bài tập và bài nộp.
- SessionReport.
- Review.
- Chat.
- Notification.

### 14.3. Teacher dashboard
- Tiến độ duyệt hồ sơ.
- Quản lý chứng chỉ.
- Môn dạy và đề xuất môn.
- PricingPackage.
- Availability.
- Lịch Teacher và Student.
- Tạo/hủy/hoàn thành Booking.
- SessionReport.
- Assignment và chấm bài.
- Wallet và ledger.
- Bank account và payout.
- Ranking/thống kê.
- Chat và notification.

### 14.4. Admin dashboard
- Teacher approval queue.
- Subject proposal queue.
- User moderation.
- Refund queue.
- Extension queue.
- Payout queue.
- Khiếu nại.
- Cấu hình commission/Bayesian.
- Doanh thu hoa hồng.
- Tổng GMV.
- Booking và completion rate.
- Audit logs.

### 14.5. Route guard
- Public route.
- Authenticated route.
- Role-based route.
- Teacher approval guard.
- Redirect rõ ràng khi token hết hạn hoặc không đủ quyền.

## 15. Dashboard và thống kê
**Admin**
- Tổng User theo role.
- Teacher đang chờ duyệt.
- Teacher active/locked.
- Tổng giá trị gói bán ra, GMV.
- Commission đã ghi nhận.
- Tổng payout thành công.
- Tổng refund.
- Booking scheduled/completed/cancelled/expired.
- Completion rate toàn nền tảng.
- Top Teacher theo Bayesian Rating.
- Tỷ lệ chuyển đổi trial.

**Teacher**
- Pending balance.
- Available balance.
- Reserved balance.
- Tổng thu nhập.
- Gói đã bán.
- Số buổi hoàn thành.
- Completion rate.
- Trial conversion rate.
- Rating và global rank.
- Payout gần nhất.

## 16. Audit log
- **audit_logs**
  - id
  - actor_id
  - action
  - target_type
  - target_id
  - before_data, JSONB
  - after_data, JSONB
  - ip_address
  - user_agent
  - created_at

**Bắt buộc audit:**
- Duyệt/từ chối Teacher.
- Duyệt/từ chối SubjectProposal.
- Khóa/mở tài khoản.
- Đổi commission rate.
- Duyệt/từ chối refund.
- Duyệt/từ chối gia hạn.
- Xác nhận payout/refund.
- Điều chỉnh ledger.

## 17. Yêu cầu phi chức năng
**Hiệu năng**
- Public search thông thường phản hồi dưới 500 ms ở dữ liệu demo.
- API đọc thông thường dưới 300 ms khi cache hit.
- Webhook phản hồi dưới 2 giây.
- Pagination mặc định 20, tối đa 100 bản ghi.
- Không load toàn bộ Message, Ledger hoặc Booking không phân trang.

**Bảo mật**
- BCrypt cho mật khẩu.
- CORS whitelist.
- CSRF không dùng cho stateless REST JWT; OAuth callback được bảo vệ state.
- Rate limit login và reset password.
- Không trả email, số điện thoại, thông tin liên hệ phụ huynh hoặc tài khoản ngân hàng trong public DTO.
- Mã hóa số tài khoản ngân hàng khi lưu.
- Mask số tài khoản khi hiển thị.
- Không log JWT, password, API key hoặc checksum key.
- Validate chữ ký payOS theo tài liệu chính thức.

**Tin cậy**
- Webhook và scheduled job idempotent.
- Backup PostgreSQL định kỳ trong môi trường triển khai.
- Có health check cho PostgreSQL, Redis và external integrations.
- External API dùng timeout, retry có giới hạn và circuit breaker.

## 18. Test plan
### 18.1. Unit test
- Commission và rounding.
- StudentPackage counter invariant.
- Booking state machine.
- Invoice state machine.
- Refund/payout state machine.
- Bayesian Average.
- Availability matching.
- Permission/ownership rules.

### 18.2. Integration test
- Repository với PostgreSQL Testcontainers.
- Booking overlap constraint.
- Pessimistic lock khi đặt lịch đồng thời.
- Webhook idempotency.
- Wallet settlement.
- Refund rollback.
- Payout reserve/release.
- Redis cache invalidation.
- Cloudinary adapter mock.
- payOS adapter mock và webhook fixture.

### 18.3. Security test
- Student gọi API Teacher/Admin.
- Teacher truy cập gói hoặc Booking của Teacher khác.
- IDOR trên Attachment, Conversation và StudentPackage.
- JWT hết hạn/refresh token bị thu hồi.
- Webhook sai chữ ký.
- Upload file sai MIME.
- Rate-limit login/chat.

### 18.4. Acceptance scenarios
- Teacher đăng ký và gửi hồ sơ.
- Admin duyệt Teacher.
- Teacher chọn môn hoặc đề xuất môn mới.
- Teacher tạo PricingPackage.
- Student tìm Teacher theo môn, giá, rating và giờ rảnh.
- Student mua gói bằng payOS.
- Webhook kích hoạt StudentPackage đúng một lần.
- Teacher tạo Booking mà không conflict.
- Booking hoàn thành tạo SessionReport và giải ngân một buổi.
- Student đánh giá Teacher.
- Teacher tạo payout; Admin chuyển tiền và tải chứng từ.
- Booking không xác nhận tự chuyển `EXPIRED` sau 12 giờ.
- Gói gần hết hạn gửi cảnh báo.
- Gói hết hạn bị khóa.
- Student yêu cầu gia hạn hoặc refund.
- Admin xử lý refund mà không làm âm Wallet.

## 19. Dữ liệu demo
**Tối thiểu:**
- 1 Admin.
- 5 Teacher đã duyệt.
- 1 Teacher chờ duyệt.
- 10 Student.
- 10–15 Subject.
- 2–3 PricingPackage mỗi Teacher.
- 20 StudentPackage.
- Booking đủ bốn trạng thái.
- 15 Review.
- 10 Assignment và Submission.
- Một payout thành công/thất bại.
- Một refund thành công/từ chối.
- Một gói `LOCKED_EXPIRED`.
- TeacherStats có global ranking.
- Tài khoản và API key demo phải dùng environment variables, không commit vào Git.

## 20. Kế hoạch triển khai 8 tuần
- **Tuần 1 — Nền tảng:** Chốt ERD và API contract. Khởi tạo Backend/Frontend. Docker Compose. PostgreSQL, Redis và Flyway. BaseEntity, response/error format. CI build/test.
- **Tuần 2 — Auth và Teacher onboarding:** JWT, refresh token, Google OAuth2. RBAC. User, TeacherProfile, TeacherDocument. Admin duyệt Teacher. Subject catalog, TeacherSubject và SubjectProposal.
- **Tuần 3 — Marketplace:** PricingPackage. TeacherAvailability. Public profile. Search/filter Teacher. Các màn hình marketplace.
- **Tuần 4 — Payment và Package:** Invoice. payOS payment link. Webhook signature/idempotency. StudentPackage. Wallet/Ledger nền tảng. Checkout frontend.
- **Tuần 5 — Booking:** Booking concurrency. Trial Session. Hủy, hoàn thành và SessionReport. Auto-expire. Notification/email. Calendar UI.
- **Tuần 6 — Learning và Communication:** Assignment. Submission. Attachment/Cloudinary. Chat WebSocket/STOMP. Notification center.
- **Tuần 7 — Finance và Ranking:** Payout thủ công. Refund. Gia hạn. TeacherStats/Bayesian. Admin và Teacher dashboard.
- **Tuần 8 — Hoàn thiện:** Unit/integration/security test. Sửa lỗi. Seed dữ liệu demo. Tối ưu query/index/cache. Docker hóa. Viết báo cáo. Chuẩn bị slide và kịch bản demo.

## 21. Tiêu chí hoàn thành đồ án
Đồ án được xem là hoàn thành khi:
- Không còn Tenant, `tenant_id`, Hibernate Tenant Filter hoặc Tenant Subscription.
- Bốn role hoạt động đúng quyền.
- Teacher phải được Admin duyệt trước khi bán gói.
- Student thanh toán payOS và StudentPackage tự kích hoạt bằng webhook.
- Booking không bị double-booking hoặc trừ buổi hai lần.
- Booking `EXPIRED`/`CANCELLED` hoàn lượt chính xác.
- Chỉ Booking `COMPLETED` tạo thu nhập Teacher và commission 5%.
- Wallet/Ledger luôn đối soát được.
- Payout/refund có mã giao dịch và ảnh chứng từ.
- Search hỗ trợ filter giờ rảnh.
- Ranking Teacher là global Bayesian ranking.
- Có demo end-to-end và test cho các business rule quan trọng.
