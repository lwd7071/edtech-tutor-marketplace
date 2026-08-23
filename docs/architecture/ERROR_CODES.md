# Error Codes — Nền tảng Quản lý Gia sư & Lớp học trực tuyến 1-1

> Phiên bản: 1.0  
> FE phải xử lý theo `error.code`; `error.message` chỉ phục vụ hiển thị và có thể được bản địa hóa.

## 1. Cấu trúc lỗi chuẩn

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

Quy ước mã: `DOMAIN_REASON`, chữ in hoa và snake case. Không tạo mã chứa tên class, database constraint hoặc vendor exception.

## 2. HTTP mapping

| HTTP | Ý nghĩa | Ví dụ |
|---:|---|---|
| `400` | Request sai cú pháp, field validation hoặc transition không hợp lệ | `VALIDATION_ERROR`, `BOOKING_INVALID_STATE` |
| `401` | Chưa xác thực, token không hợp lệ/hết hạn | `AUTH_TOKEN_EXPIRED` |
| `403` | Sai role, ownership, account/profile không được phép | `FORBIDDEN_RESOURCE`, `TEACHER_NOT_APPROVED` |
| `404` | Không tìm thấy resource hoặc cần che giấu sự tồn tại | `RESOURCE_NOT_FOUND` |
| `409` | Duplicate, overlap, idempotency, concurrent update | `BOOKING_TIME_CONFLICT` |
| `422` | Business rule không thỏa mãn dù request đúng định dạng | `PACKAGE_NO_REMAINING_SESSION` |
| `429` | Vượt rate limit | `RATE_LIMIT_EXCEEDED` |
| `500` | Lỗi không dự kiến | `INTERNAL_SERVER_ERROR` |
| `502` | Upstream trả lỗi/response sai | `PAYMENT_PROVIDER_ERROR` |
| `503` | Upstream/hạ tầng tạm không sẵn sàng | `EXTERNAL_SERVICE_UNAVAILABLE` |

Không dùng `200` với `success=false`, ngoại trừ response webhook phải tuân theo contract riêng của provider. Webhook payOS gửi lặp hợp lệ trả `2xx` và không tạo effect lần hai.

## 3. Mã lỗi dùng chung

| Code | HTTP | Khi dùng | FE xử lý gợi ý |
|---|---:|---|---|
| `VALIDATION_ERROR` | 400 | Một hoặc nhiều field không hợp lệ; kèm `fieldErrors` | Hiển thị lỗi cạnh field |
| `MALFORMED_JSON` | 400 | JSON sai cú pháp/kiểu dữ liệu | Báo request không hợp lệ |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Content-Type không hỗ trợ | Báo định dạng không hỗ trợ |
| `RESOURCE_NOT_FOUND` | 404 | Resource không tồn tại hoặc đã soft-delete | Điều hướng 404 |
| `FORBIDDEN_RESOURCE` | 403 | Sai role/ownership; biết UUID không cấp quyền | Hiển thị không có quyền |
| `DUPLICATE_RESOURCE` | 409 | Vi phạm unique nhưng chưa có mã domain cụ thể | Giữ form, báo trùng |
| `CONCURRENT_MODIFICATION` | 409 | `version`/optimistic lock không khớp | Reload resource rồi cho thử lại |
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | Action bắt buộc thiếu `Idempotency-Key` | Tạo key và gửi lại |
| `IDEMPOTENCY_KEY_REUSED` | 409 | Cùng key nhưng payload khác | Tạo key mới hoặc dùng payload cũ |
| `RATE_LIMIT_EXCEEDED` | 429 | Vượt rate limit login/reset/chat/webhook | Tôn trọng `Retry-After` |
| `INTERNAL_SERVER_ERROR` | 500 | Lỗi hệ thống không dự kiến | Hiển thị lỗi chung và requestId |
| `EXTERNAL_SERVICE_UNAVAILABLE` | 503 | External integration/hạ tầng tạm ngừng | Cho phép thử lại có kiểm soát |

## 4. Auth và account

| Code | HTTP | Khi dùng |
|---|---:|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | Email/password sai; không tiết lộ field nào sai |
| `AUTH_TOKEN_MISSING` | 401 | Endpoint protected không có access token |
| `AUTH_TOKEN_INVALID` | 401 | Token sai chữ ký, malformed hoặc sai issuer/audience |
| `AUTH_TOKEN_EXPIRED` | 401 | Access token hết hạn |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | Refresh token sai, bị rotate hoặc không tồn tại |
| `AUTH_REFRESH_TOKEN_EXPIRED` | 401 | Refresh token hết hạn |
| `AUTH_REFRESH_TOKEN_REVOKED` | 401 | Refresh token đã bị thu hồi |
| `AUTH_EMAIL_ALREADY_EXISTS` | 409 | Email đăng ký đã tồn tại |
| `AUTH_EMAIL_NOT_VERIFIED` | 403 | Chức năng yêu cầu email đã xác minh |
| `AUTH_PASSWORD_RESET_TOKEN_INVALID` | 400 | Token reset sai/đã dùng |
| `AUTH_PASSWORD_RESET_TOKEN_EXPIRED` | 400 | Token reset hết hạn |
| `AUTH_OAUTH_ROLE_REQUIRED` | 422 | Google account mới chưa chọn Student/Teacher |
| `AUTH_OAUTH_LINK_NOT_ALLOWED` | 409 | Cố tự động link OAuth vào local account chưa xác minh |
| `ACCOUNT_LOCKED` | 403 | User ở trạng thái `LOCKED`; không login/refresh |
| `ACCOUNT_DISABLED` | 403 | User ở trạng thái `DISABLED` |
| `ACCOUNT_NOT_ACTIVE` | 403 | User chưa ở trạng thái `ACTIVE` |
| `ROLE_NOT_ALLOWED` | 403 | Role không được gọi endpoint |

## 5. Teacher, subject và availability

| Code | HTTP | Khi dùng |
|---|---:|---|
| `TEACHER_PROFILE_NOT_FOUND` | 404 | Tài khoản Teacher chưa có profile hợp lệ |
| `TEACHER_NOT_APPROVED` | 403 | Teacher chưa `APPROVED` nhưng gọi bán gói/booking/publication |
| `TEACHER_PROFILE_INVALID_STATE` | 400 | Transition profile không hợp lệ, ví dụ submit khi đã pending |
| `TEACHER_DOCUMENT_NOT_DELETABLE` | 422 | Xóa chứng chỉ đã duyệt/không còn được phép xóa |
| `SUBJECT_NOT_FOUND` | 404 | Subject không tồn tại/đã xóa |
| `SUBJECT_INACTIVE` | 422 | Subject đã ngừng hoạt động |
| `SUBJECT_NOT_ASSIGNED` | 422 | Teacher chưa có TeacherSubject active tương ứng |
| `SUBJECT_ALREADY_ASSIGNED` | 409 | Teacher gán lại môn đã có |
| `SUBJECT_CODE_ALREADY_EXISTS` | 409 | `subjects.code` trùng |
| `SUBJECT_SLUG_ALREADY_EXISTS` | 409 | `subjects.slug` trùng |
| `SUBJECT_PROPOSAL_INVALID_STATE` | 400 | Duyệt/từ chối proposal không còn PENDING |
| `AVAILABILITY_TIME_CONFLICT` | 409 | Hai khoảng active cùng teacher/ngày overlap |
| `AVAILABILITY_INVALID_RANGE` | 400 | `startTime >= endTime` hoặc thiếu bộ filter thời gian |

## 6. PricingPackage và StudentPackage

| Code | HTTP | Khi dùng |
|---|---:|---|
| `PRICING_PACKAGE_NOT_FOUND` | 404 | PricingPackage không tồn tại |
| `PACKAGE_NOT_ACTIVE` | 422 | Mua/đặt lịch với PricingPackage hoặc StudentPackage không ACTIVE |
| `PACKAGE_IMMUTABLE_AFTER_PURCHASE` | 422 | Sửa giá/số buổi/thời hạn/môn của gói đã được mua |
| `PACKAGE_EXPIRED` | 422 | StudentPackage hết hạn/`LOCKED_EXPIRED` |
| `PACKAGE_NO_REMAINING_SESSION` | 422 | `remainingSessions = 0` khi tạo Booking |
| `PACKAGE_HAS_SCHEDULED_BOOKING` | 422 | Refund khi còn Booking SCHEDULED |
| `PACKAGE_COUNTER_INVARIANT_VIOLATION` | 500 | Tổng counter không bằng total; lỗi integrity phía server |
| `PACKAGE_BOOKING_END_AFTER_EXPIRY` | 422 | `booking.endTime > studentPackage.expiresAt` |
| `PACKAGE_RELATION_MISMATCH` | 403 | Package không khớp Student/Teacher/Subject của request |
| `PACKAGE_REFUND_IN_PROGRESS` | 422 | Có RefundRequest đang xử lý/gói `REFUND_PENDING` |
| `PACKAGE_EXTENSION_NOT_ALLOWED` | 422 | Xin gia hạn khi gói không `LOCKED_EXPIRED` |
| `PACKAGE_EXTENSION_DATE_INVALID` | 422 | Ngày gia hạn mới không lớn hơn hiện tại |

## 7. Booking, trial, report và review

| Code | HTTP | Khi dùng |
|---|---:|---|
| `BOOKING_NOT_FOUND` | 404 | Booking không tồn tại hoặc không thuộc người gọi |
| `BOOKING_TIME_CONFLICT` | 409 | Overlap lịch Teacher hoặc Student theo `[start,end)` |
| `BOOKING_INVALID_TIME_RANGE` | 400 | `startTime >= endTime` |
| `BOOKING_IN_PAST` | 422 | Tạo Booking trong quá khứ |
| `BOOKING_INVALID_STATE` | 400 | Action không hợp lệ với trạng thái hiện tại |
| `BOOKING_REPORT_REQUIRED` | 400 | Complete thiếu SessionReport bắt buộc |
| `BOOKING_NOT_ENDED` | 422 | Complete trước `endTime` |
| `BOOKING_CANCEL_REASON_REQUIRED` | 400 | Cancel không có lý do |
| `BOOKING_STUDENT_CANCEL_NOT_ALLOWED` | 403 | Student gọi action hủy trực tiếp |
| `BOOKING_SETTLEMENT_ALREADY_PROCESSED` | 409 | Settlement bị gọi lặp ngoài luồng idempotent |
| `TRIAL_ALREADY_USED` | 422 | Cặp Student–Teacher đã có Trial SCHEDULED/COMPLETED |
| `TRIAL_REQUEST_ALREADY_PENDING` | 409 | Cặp Student–Teacher đã có yêu cầu học thử PENDING |
| `TRIAL_REQUEST_INVALID_STATE` | 400 | Accept/reject request không còn PENDING |
| `TRIAL_PACKAGE_NOT_ALLOWED` | 400 | Trial request/booking lại có StudentPackage |
| `SESSION_REPORT_ALREADY_EXISTS` | 409 | Booking đã có report |
| `REVIEW_NOT_ALLOWED` | 422 | Booking chưa completed hoặc người gọi không phải Student tham gia |
| `REVIEW_ALREADY_EXISTS` | 409 | Booking đã được đánh giá |
| `REVIEW_RATING_INVALID` | 400 | Rating ngoài 1–5 |

## 8. Payment và invoice

| Code | HTTP | Khi dùng |
|---|---:|---|
| `INVOICE_NOT_FOUND` | 404 | Invoice/orderCode không tồn tại |
| `INVOICE_INVALID_STATE` | 400 | Action không hợp lệ với trạng thái invoice |
| `INVOICE_EXPIRED` | 422 | Payment link/invoice đã hết hạn |
| `PAYMENT_AMOUNT_MISMATCH` | 422 | Số tiền provider khác chính xác `invoice.amountVnd` |
| `PAYMENT_SIGNATURE_INVALID` | 401 | Webhook signature không hợp lệ |
| `PAYMENT_ALREADY_PROCESSED` | 409* | Provider reference đã xử lý |
| `PAYMENT_LINK_CREATION_FAILED` | 502 | payOS không tạo được payment link |
| `PAYMENT_PROVIDER_ERROR` | 502 | payOS trả lỗi/response không hợp lệ |
| `PAYMENT_RECONCILIATION_FAILED` | 502 | Đối soát server-to-server thất bại |

`PAYMENT_ALREADY_PROCESSED`: webhook lặp hợp lệ không trả lỗi này ra provider mà trả `2xx`; mã này chỉ dùng cho action nội bộ/client không có semantics webhook idempotent.

## 9. Wallet, ledger và payout

| Code | HTTP | Khi dùng |
|---|---:|---|
| `WALLET_NOT_FOUND` | 404 | Wallet Teacher không tồn tại |
| `WALLET_BALANCE_INVARIANT_VIOLATION` | 500 | Balance âm/không khớp ledger do lỗi server |
| `LEDGER_DUPLICATE_ENTRY` | 409 | Trùng `idempotencyKey` ngoài replay hợp lệ |
| `BANK_ACCOUNT_NOT_FOUND` | 404 | Tài khoản ngân hàng không tồn tại/không thuộc Teacher |
| `BANK_ACCOUNT_NOT_VERIFIED` | 422 | Payout yêu cầu tài khoản đã verified nhưng chưa đạt |
| `BANK_ACCOUNT_IN_USE` | 409 | Xóa tài khoản đang được payout tham chiếu |
| `PAYOUT_INSUFFICIENT_BALANCE` | 422 | `amountVnd > availableBalanceVnd` |
| `PAYOUT_INVALID_AMOUNT` | 400 | Amount không dương/không đạt giới hạn cấu hình |
| `PAYOUT_INVALID_STATE` | 400 | Transition payout không hợp lệ |
| `PAYOUT_PROOF_REQUIRED` | 400 | Complete thiếu mã giao dịch/thời điểm/chứng từ |
| `PAYOUT_ALREADY_PENDING` | 422 | Có payout chưa kết thúc theo policy MVP |

## 10. Refund và extension

| Code | HTTP | Khi dùng |
|---|---:|---|
| `REFUND_NOT_FOUND` | 404 | RefundRequest không tồn tại |
| `REFUND_INVALID_STATE` | 400 | Transition refund không hợp lệ |
| `REFUND_AMOUNT_EXCEEDED` | 422 | Admin duyệt sessions/amount lớn hơn phần được hoàn |
| `REFUND_NO_REMAINING_SESSION` | 422 | Gói không còn lượt để hoàn |
| `REFUND_ALREADY_PENDING` | 409 | Package đã có refund đang xử lý |
| `REFUND_PROOF_REQUIRED` | 400 | Complete thiếu mã giao dịch/thời điểm/chứng từ |
| `REFUND_WALLET_INSUFFICIENT` | 422 | Thao tác hoàn làm Wallet âm/không thể đối soát |
| `EXTENSION_REQUEST_NOT_FOUND` | 404 | ExtensionRequest không tồn tại |
| `EXTENSION_REQUEST_ALREADY_PENDING` | 409 | Package đã có extension PENDING |
| `EXTENSION_INVALID_STATE` | 400 | Transition extension không hợp lệ |

## 11. Learning, attachment, chat và notification

| Code | HTTP | Khi dùng |
|---|---:|---|
| `ASSIGNMENT_NOT_FOUND` | 404 | Assignment không tồn tại/không thuộc quan hệ học tập |
| `ASSIGNMENT_INVALID_STATE` | 400 | Publish/submit/close sai trạng thái |
| `ASSIGNMENT_DUE_DATE_PASSED` | 422 | Nộp sau hạn khi policy không cho phép |
| `SUBMISSION_NOT_FOUND` | 404 | Submission không tồn tại |
| `SUBMISSION_ALREADY_GRADED` | 409 | Chấm lại ngoài flow được phép |
| `SUBMISSION_NOT_SUBMITTED` | 422 | Chấm submission còn DRAFT |
| `FILE_TYPE_NOT_ALLOWED` | 422 | Extension hoặc MIME thực tế không nằm allow-list |
| `FILE_TOO_LARGE` | 413 | File vượt giới hạn theo loại |
| `FILE_UPLOAD_FAILED` | 502 | Cloudinary upload thất bại |
| `ATTACHMENT_NOT_FOUND` | 404 | Attachment không tồn tại/không thuộc người gọi |
| `ATTACHMENT_CONTEXT_INVALID` | 422 | Gắn attachment sai loại/resource context |
| `CONVERSATION_NOT_FOUND` | 404 | Conversation không tồn tại hoặc người gọi không phải member |
| `CONVERSATION_NOT_ALLOWED` | 403 | Cặp Student–Teacher chưa có quan hệ hợp lệ để chat |
| `MESSAGE_TYPE_INVALID` | 400 | Payload không khớp `TEXT/IMAGE/FILE` |
| `MESSAGE_DUPLICATE` | 409 | Trùng `clientMessageId` ngoài replay hợp lệ |
| `NOTIFICATION_NOT_FOUND` | 404 | Notification không tồn tại/không thuộc người gọi |

## 12. Nguyên tắc implement Backend

1. Dùng một `ErrorCode` enum/registry chứa code, default message và HTTP status; không hard-code rải rác.
2. `@RestControllerAdvice` chuyển exception thành response envelope chuẩn.
3. Bean Validation gom toàn bộ lỗi field thành `fieldErrors`; tên field dùng camelCase như API.
4. Không chuyển mọi exception thành `400`; exception không được nhận diện phải là `500` và có `requestId`.
5. Constraint/lock exception phải map có chủ đích: booking exclusion → `BOOKING_TIME_CONFLICT`; optimistic lock → `CONCURRENT_MODIFICATION`.
6. `404` có thể dùng thay `403` cho IDOR nhạy cảm để không tiết lộ resource tồn tại.
7. Message trả client không chứa SQL, class name, stack trace, raw provider payload hoặc secret.

## 13. Nguyên tắc implement Frontend

1. Interceptor chỉ refresh một lần cho nhóm request gặp `AUTH_TOKEN_EXPIRED`; tránh refresh storm.
2. Refresh thất bại/refresh token bị revoke thì xóa session và điều hướng login.
3. `VALIDATION_ERROR` map theo `fieldErrors`; lỗi domain hiển thị thông báo form/toast phù hợp.
4. `CONCURRENT_MODIFICATION` yêu cầu reload dữ liệu thay vì tự động gửi lại mutation.
5. `429` tôn trọng header `Retry-After`.
6. `502/503` chỉ retry tự động với request idempotent hoặc có `Idempotency-Key`.
