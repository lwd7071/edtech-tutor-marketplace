# API Contract — Nền tảng Quản lý Gia sư & Lớp học trực tuyến 1-1

> Phiên bản API: v1 (MVP)  
> Base path REST: `/api`  
> Media type: `application/json; charset=UTF-8`  
> Thời gian: ISO-8601; server lưu UTC, client hiển thị `Asia/Ho_Chi_Minh`  
> Tiền: VND nguyên (`integer/int64`)

Tài liệu này là nguồn thống nhất cho Frontend và Backend. Thay đổi endpoint/DTO phải sửa contract trước hoặc trong cùng pull request.

## 1. Quy ước giao tiếp

### 1.1. Authentication

REST dùng access token JWT:

```http
Authorization: Bearer <access-token>
```

- Access token: 15 phút.
- Refresh token: 7 ngày, rotate mỗi lần refresh; client phải thay token cũ bằng token mới.
- Endpoint public và webhook không yêu cầu JWT.
- Endpoint role-specific yêu cầu đúng role và kiểm tra ownership tại Service.

### 1.2. Response envelope

Thành công với một resource:

```json
{
  "success": true,
  "data": {
    "id": "b62a79c5-98f6-4bf0-80fd-37f598db97e6"
  },
  "timestamp": "2026-08-18T02:00:00Z"
}
```

Thành công có phân trang:

```json
{
  "success": true,
  "data": [],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true
  },
  "timestamp": "2026-08-18T02:00:00Z"
}
```

Thất bại:

```json
{
  "success": false,
  "error": {
    "code": "BOOKING_TIME_CONFLICT",
    "message": "Giáo viên đã có lịch trong khoảng thời gian này",
    "fieldErrors": [
      { "field": "startTime", "message": "Khoảng thời gian bị trùng lịch" }
    ]
  },
  "timestamp": "2026-08-18T02:00:00Z",
  "requestId": "01K30T7A7EBMKQGZJGF3Z92RWB"
}
```

- `data` không xuất hiện trong response lỗi; `error` không xuất hiện trong response thành công.
- `meta` chỉ xuất hiện khi cần, chủ yếu với list phân trang.
- `message` phục vụ hiển thị; FE dùng `error.code` cho logic, không so sánh message.
- Không trả stack trace, secret, JWT, checksum key hay dữ liệu tài chính chưa mask.

### 1.3. HTTP status

| Status | Dùng khi |
|---:|---|
| `200 OK` | Đọc/cập nhật/action thành công |
| `201 Created` | Tạo resource thành công |
| `202 Accepted` | Tác vụ đã nhận và xử lý bất đồng bộ nếu có |
| `204 No Content` | Xóa/ngừng liên kết thành công và không cần body |
| `400 Bad Request` | JSON/validation/state transition không hợp lệ |
| `401 Unauthorized` | Thiếu/sai/hết hạn token |
| `403 Forbidden` | Sai role, ownership hoặc tài khoản bị khóa |
| `404 Not Found` | Resource không tồn tại hoặc không được phép tiết lộ sự tồn tại |
| `409 Conflict` | Duplicate, overlap, idempotency, optimistic/concurrent conflict |
| `422 Unprocessable Entity` | Business rule hợp lệ về cú pháp nhưng không thỏa mãn |
| `429 Too Many Requests` | Vượt rate limit |
| `500` | Lỗi không dự kiến |
| `502/503` | Dịch vụ ngoài lỗi/không sẵn sàng |

### 1.4. Pagination, filter và sort

- Mặc định `page=0`, `size=20`; `size` tối đa `100`.
- `sort=<field>,asc|desc`; có thể lặp tham số `sort` nếu endpoint cho phép.
- Field sort không nằm trong allow-list trả `400 VALIDATION_ERROR`.
- List Message/Ledger/Booking luôn phân trang.

### 1.5. Concurrency và idempotency

- Resource có optimistic lock nhận field `version` trong request update/action hoặc header `If-Match`; MVP chuẩn hóa dùng field `version`.
- Sai version trả `409 CONCURRENT_MODIFICATION`.
- Các lệnh tạo payment, payout và action tài chính nhận header:

```http
Idempotency-Key: <UUID do client tạo>
```

- Gửi lại cùng key và cùng payload trả kết quả cũ; cùng key nhưng khác payload trả `409 IDEMPOTENCY_KEY_REUSED`.

### 1.6. Kiểu dữ liệu chung

```text
UUID        chuỗi canonical UUID
Instant     ISO-8601 có offset hoặc Z, ví dụ 2026-08-20T12:00:00Z
LocalTime   HH:mm:ss, ví dụ 19:00:00
MoneyVnd    integer int64, ví dụ 1000000
Page<T>     data: T[], meta: PageMeta
```

## 2. Auth API

| Method | Endpoint | Quyền | Request | Response `data` |
|---|---|---|---|---|
| POST | `/api/auth/register` | Public | `RegisterRequest` | `AuthResult` (`201`) |
| POST | `/api/auth/login` | Public | `LoginRequest` | `AuthResult` |
| POST | `/api/auth/refresh` | Public | `RefreshRequest` | `AuthResult` |
| POST | `/api/auth/logout` | Authenticated | `RefreshRequest` | `null` |
| POST | `/api/auth/verify-email` | Public | `VerifyEmailRequest` | `null` |
| POST | `/api/auth/resend-verification` | Public | `ForgotPasswordRequest` | `null` |
| POST | `/api/auth/forgot-password` | Public | `ForgotPasswordRequest` | `null` |
| POST | `/api/auth/reset-password` | Public | `ResetPasswordRequest` | `null` |
| POST | `/api/auth/oauth2/complete-registration` | Public | `CompleteOAuthRegistrationRequest` | `AuthResult` |
| GET | `/oauth2/authorization/google` | Public | — | Redirect Google |

```json
// RegisterRequest
{
  "email": "student@example.com",
  "password": "StrongPassword123!",
  "fullName": "Nguyễn Minh An",
  "role": "STUDENT",
  "parentFullName": "Nguyễn Văn Bình",
  "parentPhone": "0901234567",
  "parentEmail": "parent@example.com"
}
```

`role` chỉ nhận `STUDENT | TEACHER`.

`parentFullName`, `parentPhone`, `parentEmail` đều optional và chỉ áp dụng khi `role = STUDENT`; với Teacher, các field này phải được bỏ khỏi request. RegisterRequest không nhận `notifyParent`. Nếu có ít nhất `parentEmail` hoặc `parentPhone`, Backend tự đặt `notifyParent = true`; nếu không có, giá trị mặc định là `false`. Phụ huynh không có role hoặc tài khoản đăng nhập riêng.

```json
// LoginRequest
{
  "email": "student@example.com",
  "password": "StrongPassword123!",
  "deviceInfo": "Chrome 140 / Windows 11"
}
```

```json
// AuthResult
{
  "accessToken": "eyJ...",
  "refreshToken": "raw-token-only-returned-once",
  "tokenType": "Bearer",
  "accessTokenExpiresIn": 900,
  "user": {
    "id": "2e5307a4-a06c-41fb-9ea0-d4ab4d6a7082",
    "email": "student@example.com",
    "fullName": "Nguyễn Minh An",
    "role": "STUDENT",
    "status": "ACTIVE",
    "avatarUrl": null
  }
}
```

```json
// RefreshRequest
{ "refreshToken": "raw-refresh-token" }
```

`forgot-password` luôn trả cùng response dù email tồn tại hay không để tránh enumeration.

`complete-registration` nhận OAuth registration token ngắn hạn do callback phát hành và `role=STUDENT|TEACHER`; endpoint không nhận Google access token trực tiếp từ browser.

## 3. Public API

| Method | Endpoint | Query/Request | Response `data` |
|---|---|---|---|
| GET | `/api/public/subjects` | `keyword`, `educationLevel`, `page`, `size`, `sort` | `SubjectSummary[]` |
| GET | `/api/public/teachers` | `TeacherSearchParams` | `TeacherCard[]` |
| GET | `/api/public/teachers/{id}` | — | `TeacherPublicDetail` |
| GET | `/api/public/teachers/{id}/packages` | `page`, `size`, `sort` | `PricingPackageView[]` |
| GET | `/api/public/teachers/{id}/availability` | — | `AvailabilityView[]` |
| GET | `/api/public/teachers/{id}/reviews` | `page`, `size`, `sort` | `ReviewView[]` |
| GET | `/api/public/teachers/ranking` | `subjectId?`, `page`, `size` | `TeacherRankingItem[]` |

`TeacherSearchParams`:

```text
keyword, subjectId, minPrice, maxPrice, minRating,
deliveryMode=ONLINE|OFFLINE, dayOfWeek=MONDAY..SUNDAY,
startTime=HH:mm:ss, endTime=HH:mm:ss, page, size, sort
```

Khi filter giờ rảnh, phải truyền đủ `dayOfWeek`, `startTime`, `endTime`. Khớp khi `availability.startTime <= startTime AND availability.endTime >= endTime`.

```json
// TeacherCard
{
  "id": "94aa56f2-47d7-4f04-a88b-48d94b1cb19e",
  "fullName": "Trần Thu Hà",
  "avatarUrl": "https://res.cloudinary.com/...",
  "bioExcerpt": "Giáo viên Toán THPT...",
  "yearsOfExperience": 6,
  "verifiedBadge": true,
  "supportsOnline": true,
  "supportsOffline": false,
  "subjects": [{ "id": "uuid", "name": "Toán 11" }],
  "startingPriceVnd": 800000,
  "averageRating": 4.8,
  "bayesianRating": 4.65,
  "reviewCount": 35,
  "globalRank": 4
}
```

Public DTO không chứa email, phone, account number, document URL riêng tư hoặc thông tin định danh.

## 4. Teacher API

Tất cả endpoint yêu cầu role `TEACHER`; endpoint bán gói/booking yêu cầu profile `APPROVED`.

### 4.1. Profile, document, subject và availability

| Method | Endpoint | Request | Response `data` |
|---|---|---|---|
| GET | `/api/teacher/profile` | — | `TeacherProfileDetail` |
| PUT | `/api/teacher/profile` | `UpdateTeacherProfileRequest` | `TeacherProfileDetail` |
| POST | `/api/teacher/profile/submit` | — | `TeacherProfileDetail` |
| POST | `/api/teacher/documents` | `multipart/form-data` | `TeacherDocumentView` (`201`) |
| DELETE | `/api/teacher/documents/{id}` | — | — (`204`) |
| GET | `/api/teacher/subjects` | — | `TeacherSubjectView[]` |
| POST | `/api/teacher/subjects/{subjectId}` | `AssignSubjectRequest` | `TeacherSubjectView` (`201`) |
| DELETE | `/api/teacher/subjects/{subjectId}` | — | — (`204`) |
| POST | `/api/teacher/subject-proposals` | `CreateSubjectProposalRequest` | `SubjectProposalView` (`201`) |
| GET | `/api/teacher/subject-proposals` | `status?`, pagination | `SubjectProposalView[]` |
| GET | `/api/teacher/availability` | — | `AvailabilityView[]` |
| PUT | `/api/teacher/availability` | `ReplaceAvailabilityRequest` | `AvailabilityView[]` |

```json
// UpdateTeacherProfileRequest
{
  "bio": "6 năm dạy Toán THPT và luyện thi đại học.",
  "yearsOfExperience": 6,
  "languages": ["vi", "en"],
  "supportsOnline": true,
  "supportsOffline": false,
  "locationAddress": null,
  "introductionVideoUrl": "https://youtube.com/watch?v=..."
}
```

Upload document dùng parts `file`, `documentType`, `title`. MIME/size theo `CODING_CONVENTION.md` và spec upload.

```json
// ReplaceAvailabilityRequest
{
  "items": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "19:00:00",
      "endTime": "21:00:00",
      "timezone": "Asia/Ho_Chi_Minh",
      "isActive": true
    }
  ]
}
```

PUT availability thay toàn bộ danh sách trong một transaction; overlap trả `409 AVAILABILITY_TIME_CONFLICT`.

### 4.2. Pricing package và booking

| Method | Endpoint | Request | Response `data` |
|---|---|---|---|
| POST | `/api/teacher/packages` | `UpsertPricingPackageRequest` | `PricingPackageView` (`201`) |
| PUT | `/api/teacher/packages/{id}` | `UpsertPricingPackageRequest` | `PricingPackageView` |
| PATCH | `/api/teacher/packages/{id}/status` | `ChangePackageStatusRequest` | `PricingPackageView` |
| POST | `/api/teacher/bookings` | `CreateBookingRequest` | `BookingDetail` (`201`) |
| POST | `/api/teacher/bookings/{id}/complete` | `CompleteBookingRequest` | `BookingDetail` |
| POST | `/api/teacher/bookings/{id}/cancel` | `CancelBookingRequest` | `BookingDetail` |
| GET | `/api/teacher/trial-requests` | `status?`, pagination | `TrialRequestView[]` |
| POST | `/api/teacher/trial-requests/{id}/accept` | `AcceptTrialRequest` | `BookingDetail` (`201`) |
| POST | `/api/teacher/trial-requests/{id}/reject` | `RejectTrialRequest` | `TrialRequestView` |
| GET | `/api/teacher/students` | `keyword?`, pagination | `TeacherStudentView[]` |

```json
// UpsertPricingPackageRequest
{
  "subjectId": "0cd80fd5-df93-4ac8-857d-f1e599b86381",
  "name": "Toán 11 — 10 buổi",
  "description": "Ôn kiến thức và luyện bài tập theo chuyên đề.",
  "totalSessions": 10,
  "durationDays": 90,
  "priceVnd": 1000000,
  "sessionDurationMinutes": 90,
  "status": "DRAFT",
  "version": 0
}
```

```json
// CreateBookingRequest
{
  "studentPackageId": "43c6445f-2f69-4bd1-a108-17647845d66a",
  "startTime": "2026-08-20T19:00:00+07:00",
  "endTime": "2026-08-20T20:30:00+07:00",
  "deliveryMode": "ONLINE",
  "meetingLink": "https://meet.example/abc",
  "locationAddress": null
}
```

```json
// CompleteBookingRequest
{
  "version": 2,
  "report": {
    "recordLink": "https://drive.example/record",
    "content": "Đã hoàn thành chương Dao động điều hòa.",
    "feedback": "Học sinh nắm tốt công thức cơ bản.",
    "followUpNote": "Làm bài 1–10 trước buổi sau.",
    "teacherSelfRating": 4
  }
}
```

```json
// CancelBookingRequest
{
  "version": 2,
  "reason": "Học sinh yêu cầu đổi lịch qua chat",
  "initiatedBy": "STUDENT_REQUEST"
}
```

Hoàn thành Booking và tạo SessionReport là một transaction. Response không được có Booking `COMPLETED` thiếu `sessionReport`.

StudentPackage phải `ACTIVE` tại thời điểm tạo Booking mới. Nếu package chuyển `LOCKED_EXPIRED` sau khi Booking đã được tạo, Booking `SCHEDULED` đó vẫn được giữ nguyên và diễn ra bình thường; hệ thống không tự động hủy hoặc hoàn lượt chỉ vì package hết hạn.

```json
// AcceptTrialRequest
{
  "startTime": "2026-08-21T19:00:00+07:00",
  "endTime": "2026-08-21T20:00:00+07:00",
  "deliveryMode": "ONLINE",
  "meetingLink": "https://meet.example/trial",
  "locationAddress": null
}
```

Accept tạo Booking trial và đổi TrialRequest sang `ACCEPTED` trong cùng transaction; vẫn phải kiểm tra conflict Teacher/Student.

### 4.3. Learning, wallet và payout

| Method | Endpoint | Request | Response `data` |
|---|---|---|---|
| POST | `/api/teacher/assignments` | `CreateAssignmentRequest` | `AssignmentDetail` (`201`) |
| POST | `/api/teacher/submissions/{id}/grade` | `GradeSubmissionRequest` | `SubmissionDetail` |
| GET | `/api/teacher/wallet` | — | `WalletView` |
| GET | `/api/teacher/wallet/ledger` | filters, pagination | `LedgerEntryView[]` |
| GET | `/api/teacher/bank-accounts` | — | `BankAccountView[]` |
| POST | `/api/teacher/bank-accounts` | `UpsertBankAccountRequest` | `BankAccountView` (`201`) |
| PUT | `/api/teacher/bank-accounts/{id}` | `UpsertBankAccountRequest` | `BankAccountView` |
| DELETE | `/api/teacher/bank-accounts/{id}` | — | — (`204`) |
| POST | `/api/teacher/payout-requests` | `CreatePayoutRequest` | `PayoutRequestView` (`201`) |
| GET | `/api/teacher/payout-requests` | `status?`, pagination | `PayoutRequestView[]` |
| GET | `/api/teacher/stats` | — | `TeacherStatsView` |

Các endpoint bank account là phần bổ sung bắt buộc để hỗ trợ luồng payout đã mô tả trong spec.

```json
// CreateAssignmentRequest
{
  "studentId": "cfcc90fc-eed1-4647-9b71-8f3ad6343779",
  "subjectId": "0cd80fd5-df93-4ac8-857d-f1e599b86381",
  "title": "Bài tập Dao động điều hòa",
  "assignmentType": "FREEFORM",
  "contentBlocks": [
    { "type": "TEXT", "content": "Hoàn thành bài 1 đến 10." },
    { "type": "FILE", "attachmentId": "6f676682-2207-4344-96dd-85c29248fa9e" }
  ],
  "quizSchema": null,
  "dueAt": "2026-08-25T23:59:00+07:00",
  "status": "PUBLISHED"
}
```

```json
// CreatePayoutRequest
{
  "bankAccountId": "e5871555-b979-432c-a31f-aa03927ffb41",
  "amountVnd": 950000,
  "teacherNote": "Rút thu nhập tháng 8",
  "walletVersion": 5
}
```

```json
// WalletView
{
  "id": "uuid",
  "pendingBalanceVnd": 190000,
  "availableBalanceVnd": 950000,
  "reservedBalanceVnd": 0,
  "version": 5
}
```

## 5. Student API

Tất cả endpoint yêu cầu role `STUDENT` và ownership của resource.

| Method | Endpoint | Request/Query | Response `data` |
|---|---|---|---|
| POST | `/api/student/invoices` | `CreateInvoiceRequest` | `InvoiceDetail` (`201`) |
| GET | `/api/student/invoices/{id}` | — | `InvoiceDetail` |
| GET | `/api/student/packages` | `status?`, pagination | `StudentPackageSummary[]` |
| GET | `/api/student/packages/{id}` | — | `StudentPackageDetail` |
| GET | `/api/student/bookings` | `status?`, `from?`, `to?`, pagination | `BookingDetail[]` |
| POST | `/api/student/trials/requests` | `CreateTrialRequest` | `TrialRequestView` (`201`) |
| POST | `/api/student/refund-requests` | `CreateRefundRequest` | `RefundRequestView` (`201`) |
| POST | `/api/student/extension-requests` | `CreateExtensionRequest` | `ExtensionRequestView` (`201`) |
| GET | `/api/student/assignments` | `status?`, pagination | `AssignmentDetail[]` |
| POST | `/api/student/assignments/{id}/submissions` | `CreateSubmissionRequest` | `SubmissionDetail` (`201`) |
| GET | `/api/student/session-reports` | pagination | `SessionReportView[]` |
| POST | `/api/student/bookings/{id}/review` | `CreateReviewRequest` | `ReviewView` (`201`) |
| GET | `/api/student/notifications` | `isRead?`, pagination | `NotificationView[]` |
| PATCH | `/api/student/notifications/{id}/read` | — | `NotificationView` |
| PUT | `/api/student/parent-contact` | `UpdateParentContactRequest` | `ParentContactResponse` |

Student không có endpoint hủy Booking trực tiếp.

```json
// UpdateParentContactRequest
{
  "parentFullName": "Nguyễn Văn Bình",
  "parentPhone": "0901234567",
  "parentEmail": "parent@example.com",
  "notifyParent": true
}
```

```json
// ParentContactResponse
{
  "parentFullName": "Nguyễn Văn Bình",
  "parentPhone": "0901234567",
  "parentEmail": "parent@example.com",
  "notifyParent": true,
  "updatedAt": "2026-08-19T09:00:00Z"
}
```

`PUT /api/student/parent-contact` thay thế toàn bộ thông tin liên hệ phụ huynh của Student đang đăng nhập. Ba field liên hệ đều nullable; gửi `null` để xóa field tương ứng. Nếu cả ba field đều `null`, Backend bắt buộc chuẩn hóa `notifyParent = false`. Email phụ huynh chỉ được gửi khi `parentEmail` tồn tại và `notifyParent = true`; MVP không tự động gửi SMS. Parent contact chỉ xuất hiện trong DTO riêng dành cho Student sở hữu, không xuất hiện trong public DTO hoặc user summary.

```json
// CreateInvoiceRequest
{
  "pricingPackageId": "ebc78c1d-31d1-4e75-b528-cd5d6c44dd54",
  "returnUrl": "https://app.example/payment/result",
  "cancelUrl": "https://app.example/payment/cancel"
}
```

```json
// InvoiceDetail
{
  "id": "48a753cd-89ca-4ca9-a0be-2ea6489720cc",
  "invoiceNumber": "INV-20260819-000123",
  "pricingPackageId": "ebc78c1d-31d1-4e75-b528-cd5d6c44dd54",
  "amountVnd": 1000000,
  "status": "PENDING",
  "checkoutUrl": "https://pay.payos.vn/web/...",
  "qrCode": "000201...",
  "paymentExpiredAt": "2026-08-19T03:30:00Z",
  "paidAt": null
}
```

Return URL chỉ dùng cho UX. FE phải poll invoice hoặc nhận notification; không tự coi thanh toán thành công từ query parameter trình duyệt.

```json
// CreateTrialRequest
{
  "teacherId": "94aa56f2-47d7-4f04-a88b-48d94b1cb19e",
  "subjectId": "0cd80fd5-df93-4ac8-857d-f1e599b86381",
  "preferredStartTime": "2026-08-21T19:00:00+07:00",
  "note": "Em muốn thử học phần dao động cơ."
}
```

```json
// CreateRefundRequest
{
  "studentPackageId": "43c6445f-2f69-4bd1-a108-17647845d66a",
  "reason": "Không thể tiếp tục lịch học",
  "requestedSessions": 3,
  "bankName": "Vietcombank",
  "bankBin": "970436",
  "accountNumber": "0123456789",
  "accountHolderName": "NGUYEN MINH AN",
  "packageVersion": 4
}
```

```json
// CreateExtensionRequest
{
  "studentPackageId": "43c6445f-2f69-4bd1-a108-17647845d66a",
  "reason": "Nghỉ điều trị trong tháng 8",
  "requestedExpiryDate": "2026-10-31T23:59:59+07:00",
  "packageVersion": 4
}
```

```json
// CreateReviewRequest
{
  "rating": 5,
  "comment": "Giáo viên giải thích rõ ràng và đúng giờ."
}
```

## 6. Admin API

Tất cả endpoint yêu cầu role `ADMIN`. Mọi action thay đổi trạng thái phải tạo AuditLog.

| Method | Endpoint | Request | Response `data` |
|---|---|---|---|
| GET | `/api/admin/teacher-approvals` | `status?`, pagination | `TeacherApprovalView[]` |
| POST | `/api/admin/teacher-approvals/{id}/approve` | `ApproveTeacherRequest` | `TeacherApprovalView` |
| POST | `/api/admin/teacher-approvals/{id}/reject` | `RejectRequest` | `TeacherApprovalView` |
| GET | `/api/admin/subject-proposals` | `status?`, pagination | `SubjectProposalView[]` |
| POST | `/api/admin/subject-proposals/{id}/approve` | `ApproveSubjectProposalRequest` | `SubjectProposalView` |
| POST | `/api/admin/subject-proposals/{id}/reject` | `RejectRequest` | `SubjectProposalView` |
| POST | `/api/admin/subjects` | `UpsertSubjectRequest` | `SubjectView` (`201`) |
| PUT | `/api/admin/subjects/{id}` | `UpsertSubjectRequest` | `SubjectView` |
| GET | `/api/admin/refund-requests` | `status?`, pagination | `RefundRequestView[]` |
| POST | `/api/admin/refund-requests/{id}/approve` | `ApproveRefundRequest` | `RefundRequestView` |
| POST | `/api/admin/refund-requests/{id}/reject` | `RejectRequest` | `RefundRequestView` |
| POST | `/api/admin/refund-requests/{id}/complete` | `CompleteTransferRequest` | `RefundRequestView` |
| GET | `/api/admin/extension-requests` | `status?`, pagination | `ExtensionRequestView[]` |
| POST | `/api/admin/extension-requests/{id}/approve` | `ApproveExtensionRequest` | `ExtensionRequestView` |
| POST | `/api/admin/extension-requests/{id}/reject` | `RejectRequest` | `ExtensionRequestView` |
| GET | `/api/admin/payout-requests` | `status?`, pagination | `PayoutRequestView[]` |
| POST | `/api/admin/payout-requests/{id}/process` | `{ "version": n }` | `PayoutRequestView` |
| POST | `/api/admin/payout-requests/{id}/complete` | `CompleteTransferRequest` | `PayoutRequestView` |
| POST | `/api/admin/payout-requests/{id}/reject` | `RejectRequest` | `PayoutRequestView` |
| PATCH | `/api/admin/users/{id}/status` | `ChangeUserStatusRequest` | `AdminUserView` |
| GET | `/api/admin/dashboard` | `from?`, `to?` | `AdminDashboardView` |
| GET | `/api/admin/audit-logs` | filters, pagination | `AuditLogView[]` |
| GET | `/api/admin/settings` | — | `PlatformSettingsView` |
| PUT | `/api/admin/settings` | `UpdatePlatformSettingsRequest` | `PlatformSettingsView` |

```json
// ApproveSubjectProposalRequest
{
  "resolution": "CREATE_NEW",
  "existingSubjectId": null,
  "newSubject": {
    "code": "PHY11",
    "name": "Vật lý 11",
    "slug": "vat-ly-11",
    "educationLevel": "GRADE_11",
    "description": "Chương trình Vật lý lớp 11"
  }
}
```

`resolution` nhận `LINK_EXISTING | CREATE_NEW`. Hai nhánh bắt buộc field tương ứng và tự tạo TeacherSubject.

```json
// ApproveRefundRequest
{
  "approvedSessions": 3,
  "adminNote": "Đủ điều kiện hoàn 3 buổi",
  "version": 1
}
```

Backend tính `refundAmountVnd = floor(approvedSessions × purchasePriceVnd / totalSessions)` theo đơn vị VND; Admin không truyền số tiền này trong request. Nếu cùng một StudentPackage được duyệt refund nhiều lần, phần dư do làm tròn được cộng vào lần duyệt cuối. `RefundRequestView` trả `refundAmountVnd` đã được Backend tính.

```json
// CompleteTransferRequest (multipart/form-data)
{
  "bankReference": "VCB202608190001",
  "transferredAt": "2026-08-19T14:30:00+07:00",
  "version": 2,
  "proof": "<JPG/PNG/PDF file>"
}
```

```json
// ChangeUserStatusRequest
{
  "status": "LOCKED",
  "reason": "Vi phạm chính sách nền tảng"
}
```

```json
// UpdatePlatformSettingsRequest
{
  "commissionRate": 5.00,
  "bayesianMinimumReviews": 10,
  "bookingReminderHours": 11,
  "bookingExpirationHours": 12
}
```

## 7. Communication và attachment API

Các endpoint REST dưới đây bổ sung phần còn thiếu trong danh sách mục 8 nhưng cần thiết cho màn hình chat, lịch sử tin nhắn và upload file.

| Method | Endpoint | Quyền | Request/Query | Response `data` |
|---|---|---|---|---|
| GET | `/api/conversations` | Student/Teacher | pagination | `ConversationView[]` |
| GET | `/api/conversations/{id}/messages` | Member | cursor/page, size | `MessageView[]` |
| POST | `/api/attachments` | Authenticated | multipart | `AttachmentView` (`201`) |
| GET | `/api/notifications` | Authenticated | `isRead?`, pagination | `NotificationView[]` |
| PATCH | `/api/notifications/{id}/read` | Owner | — | `NotificationView` |
| POST | `/api/notifications/read-all` | Authenticated | — | `{ "updatedCount": n }` |

Chat chỉ mở khi cặp Student–Teacher có StudentPackage, Trial Booking hoặc Booking hợp lệ. Biết `conversationId` không thay thế membership check.

### 7.1. WebSocket/STOMP

- Handshake: `/ws`
- Subscribe: `/user/queue/messages`, `/user/queue/notifications`, `/topic/conversations/{conversationId}`
- Send: `/app/chat.send`, `/app/chat.read`

```json
// /app/chat.send
{
  "clientMessageId": "97947f5c-3804-448a-bf35-a21102cb110d",
  "conversationId": "18dddb00-60a2-4330-b1fa-024245f844f6",
  "messageType": "TEXT",
  "content": "Em muốn đổi lịch học ngày mai.",
  "attachmentId": null
}
```

```json
// MessageView
{
  "id": "c65872d2-a63a-4a6f-8ddb-a89bc02af5da",
  "clientMessageId": "97947f5c-3804-448a-bf35-a21102cb110d",
  "conversationId": "18dddb00-60a2-4330-b1fa-024245f844f6",
  "senderId": "cfcc90fc-eed1-4647-9b71-8f3ad6343779",
  "messageType": "TEXT",
  "content": "Em muốn đổi lịch học ngày mai.",
  "attachment": null,
  "sentAt": "2026-08-19T08:00:00Z",
  "readAt": null
}
```

Server xác minh sender là thành viên Conversation. `clientMessageId` nên unique theo sender để chống gửi lặp khi reconnect.

## 8. payOS webhook

```http
POST /api/webhooks/payos
Content-Type: application/json
```

- Không dùng JWT; xác minh signature theo tài liệu payOS.
- Kiểm tra `orderCode`, số tiền chính xác, trạng thái invoice và provider reference.
- Webhook hợp lệ đã xử lý trả `200` và không tạo effect lần hai.
- Webhook sai signature trả lỗi, không ghi PaymentTransaction thành công và không kích hoạt package.
- Log `requestId`, `orderCode`, provider reference; không log secret/checksum key.

Response xác nhận tối giản theo yêu cầu provider; không bắt buộc dùng envelope nội bộ nếu payOS yêu cầu schema riêng.

## 9. DTO cốt lõi

### 9.1. StudentPackageDetail

```json
{
  "id": "43c6445f-2f69-4bd1-a108-17647845d66a",
  "teacher": { "id": "uuid", "fullName": "Trần Thu Hà" },
  "subject": { "id": "uuid", "name": "Toán 11" },
  "packageName": "Toán 11 — 10 buổi",
  "totalSessions": 10,
  "remainingSessions": 7,
  "reservedSessions": 1,
  "completedSessions": 2,
  "refundedSessions": 0,
  "purchasePriceVnd": 1000000,
  "startsAt": "2026-08-19T02:00:00Z",
  "expiresAt": "2026-11-17T02:00:00Z",
  "status": "ACTIVE",
  "lockedReason": null,
  "version": 4
}
```

### 9.2. BookingDetail

```json
{
  "id": "d5dfe201-e7c0-4540-9564-a52c82bf910c",
  "teacher": { "id": "uuid", "fullName": "Trần Thu Hà" },
  "student": { "id": "uuid", "fullName": "Nguyễn Minh An" },
  "studentPackageId": "43c6445f-2f69-4bd1-a108-17647845d66a",
  "subject": { "id": "uuid", "name": "Toán 11" },
  "startTime": "2026-08-20T12:00:00Z",
  "endTime": "2026-08-20T13:30:00Z",
  "deliveryMode": "ONLINE",
  "meetingLink": "https://meet.example/abc",
  "locationAddress": null,
  "status": "SCHEDULED",
  "trial": false,
  "outsideAvailabilityWarning": false,
  "sessionReport": null,
  "version": 1
}
```

### 9.3. ContentBlock

```json
[
  { "type": "TEXT", "content": "Nội dung..." },
  { "type": "IMAGE", "attachmentId": "uuid" },
  { "type": "FILE", "attachmentId": "uuid" },
  { "type": "LINK", "url": "https://example.com", "label": "Tài liệu" }
]
```

Backend validate field theo `type`; không chấp nhận attachment không thuộc người gọi hoặc sai attachable context.

## 10. Enum chính

| Nhóm | Giá trị |
|---|---|
| Role | `STUDENT`, `TEACHER`, `ADMIN` |
| User status | `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`, `DISABLED` |
| Teacher profile | `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED` |
| Package | `DRAFT`, `ACTIVE`, `INACTIVE` |
| Invoice | `PENDING`, `PAID`, `CANCELLED`, `EXPIRED` |
| StudentPackage | `PENDING_PAYMENT`, `ACTIVE`, `COMPLETED`, `REFUND_PENDING`, `REFUNDED`, `LOCKED_EXPIRED` |
| Booking | `SCHEDULED`, `COMPLETED`, `CANCELLED`, `EXPIRED` |
| Delivery | `ONLINE`, `OFFLINE` |
| Payout | `PENDING`, `PROCESSING`, `SUCCEEDED`, `REJECTED`, `FAILED` |
| Refund | `PENDING`, `APPROVED`, `PROCESSING`, `REFUNDED`, `REJECTED`, `FAILED` |
| Assignment | `DRAFT`, `PUBLISHED`, `CLOSED` |
| Submission | `DRAFT`, `SUBMITTED`, `GRADED` |

## 11. Contract decisions cần giữ ổn định

1. API JSON dùng camelCase; database dùng snake_case.
2. UUID là string; tiền VND là integer int64.
3. Không trả JPA Entity trực tiếp; mọi response qua DTO/envelope.
4. FE không suy luận payment success từ return URL.
5. FE dùng `error.code`, không dùng nội dung `message` để điều khiển luồng.
6. List lớn luôn phân trang; không có endpoint “get all” cho booking/message/ledger.
7. Mọi update/action trên entity có version phải gửi version hiện tại.
8. Các endpoint bổ sung ở Auth, trial, mục 4.3 và mục 7 là quyết định contract v1 để lấp khoảng trống chức năng trong đặc tả gốc.
9. Thông tin liên hệ phụ huynh chỉ là dữ liệu bị động của Student; không có role `PARENT`, đăng nhập phụ huynh hoặc SMS tự động trong MVP.
