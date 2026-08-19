# Coding Convention — Nền tảng Quản lý Gia sư & Lớp học trực tuyến 1-1

> Áp dụng cho Backend Java/Spring Boot, Frontend React/TypeScript, database migration và Git workflow.  
> Mục tiêu: hai thành viên có thể phát triển song song mà code, DTO và cách xử lý lỗi vẫn đồng nhất.

## 1. Nguyên tắc chung

- Ngôn ngữ code, identifier, branch và commit: tiếng Anh. Tài liệu/ngôn ngữ hiển thị có thể dùng tiếng Việt.
- Ưu tiên code rõ nghĩa hơn comment. Comment giải thích **vì sao**, không lặp lại code đang làm gì.
- Không commit secret, API key, checksum key, token, tài khoản demo hoặc `.env` thật.
- Thời gian lưu UTC; API dùng ISO-8601 có offset; UI hiển thị `Asia/Ho_Chi_Minh`.
- Tiền VND dùng `long`/`Long` ở Java và `number` nguyên ở TypeScript; không dùng `float`/`double` cho tiền.
- Public ID dùng UUID; không lộ database sequence.
- API JSON camelCase; database snake_case; Java/TypeScript không dùng tên cột database làm field nếu không cần.
- Enum truyền/lưu bằng tên string, không dùng ordinal.
- Không trả JPA Entity trực tiếp qua API.

## 2. Cấu trúc repository đề xuất

```text
root/
├─ backend/
│  ├─ src/main/java/com/edtech/platform/
│  ├─ src/main/resources/db/migration/
│  └─ src/test/java/com/edtech/platform/
├─ frontend/
│  └─ src/
├─ docker-compose.yml
├─ ERD.md
├─ API_CONTRACT.md
├─ ERROR_CODES.md
└─ CODING_CONVENTION.md
```

Nếu dùng monorepo, mỗi PR liên quan contract phải cập nhật tài liệu ở root cùng thay đổi code.

## 3. Backend Java/Spring Boot

### 3.1. Package theo module nghiệp vụ

Root package: `com.edtech.platform`.

```text
com.edtech.platform
├─ auth
│  ├─ controller
│  ├─ dto
│  │  ├─ request
│  │  └─ response
│  ├─ service
│  ├─ domain
│  ├─ repository
│  └─ security
├─ booking
│  ├─ controller
│  ├─ dto
│  ├─ service
│  ├─ domain
│  └─ repository
├─ finance
├─ payment
├─ teacher
├─ subject
├─ learning
├─ communication
├─ ranking
├─ admin
├─ scheduler
└─ common
   ├─ config
   ├─ exception
   ├─ response
   ├─ security
   ├─ persistence
   └─ util
```

- Tổ chức theo domain/module trước, layer sau; không đặt toàn bộ controller/service/repository của hệ thống vào package chung.
- Module giao tiếp qua public service/facade hoặc event rõ ràng; không truy cập repository của module khác tùy tiện.
- `common` chỉ chứa thành phần thật sự dùng chung; không biến thành nơi chứa mọi thứ.

### 3.2. Luồng xử lý chuẩn

```text
HTTP Request
  → Security Filter / Authentication
  → Controller
  → Bean Validation
  → Application Service (@Transactional boundary)
  → Domain rule / Repository / Integration Port
  → Response DTO + ApiResponse
```

- Controller: parse request, annotation bảo mật, gọi service, chọn HTTP status. Không chứa business rule hay truy vấn repository.
- Validation: kiểm tra cấu trúc bằng Bean Validation; business rule/ownership/state kiểm tra trong Service.
- Service: orchestration, transaction, ownership, state transition, lock và idempotency.
- Domain: entity/value object và hành vi giữ invariant cục bộ.
- Repository: persistence/query; không quyết định authorization.
- Integration adapter: payOS/Cloudinary/email/Redis; domain phụ thuộc interface/port thay vì SDK vendor.

### 3.3. Quy tắc đặt tên Java

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Package | lowercase | `com.edtech.platform.booking.service` |
| Class/interface/enum | PascalCase | `BookingService`, `BookingStatus` |
| Method/field | camelCase, động từ rõ nghĩa | `completeBooking`, `studentPackageId` |
| Constant | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Entity | danh từ số ít | `StudentPackage` |
| Repository | `<Entity>Repository` | `BookingRepository` |
| Service | `<Domain>Service` hoặc use-case rõ nghĩa | `BookingService`, `PaymentWebhookService` |
| Controller | `<Audience><Resource>Controller` | `TeacherBookingController` |
| Request DTO | `<Action><Resource>Request` | `CreateBookingRequest` |
| Response DTO | `<Resource>Response/View/Summary` | `BookingDetailResponse` |
| Exception | `<Reason>Exception` nếu cần type riêng | `ConcurrentModificationException` |
| Mapper | `<Entity>Mapper` | `BookingMapper` |
| Test | `<Class>Test`, integration `<Feature>IT` | `BookingServiceTest`, `BookingOverlapIT` |

Không dùng tên mơ hồ: `Utils`, `Helper`, `Manager`, `Data`, `Info`, `handle()` nếu có thể đặt tên cụ thể hơn.

### 3.4. Entity và persistence

```java
@MappedSuperclass
@Getter
public abstract class BaseEntity {
    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;
}
```

- Entity nghiệp vụ kế thừa BaseEntity; ledger/payment transaction/audit log không soft delete.
- Collection mặc định `LAZY`. Không sửa thành `EAGER` để chữa lỗi serialization; dùng fetch join/entity graph/projection có chủ đích.
- Không dùng Lombok `@Data` trên Entity; tránh generated `equals/hashCode/toString` đi qua lazy relation.
- `equals/hashCode` không dựa trên association mutable.
- FK field đặt tên `teacher`, `studentPackage` trong Entity; cột là `teacher_id`, `student_package_id`.
- Field enum dùng `@Enumerated(EnumType.STRING)`.
- Money VND dùng `long`; commission rate dùng `BigDecimal` với scale rõ ràng.
- Entity concurrent dùng `@Version private long version`.
- Không gọi external API trong khi giữ database lock lâu hơn cần thiết. Tạo payOS link và persist state theo flow có recovery/idempotency rõ ràng.

### 3.5. DTO và mapping

- Request/response là class/record riêng; không reuse Entity.
- Request DTO dùng Bean Validation:

```java
public record CreateBookingRequest(
    @NotNull UUID studentPackageId,
    @NotNull Instant startTime,
    @NotNull Instant endTime,
    @NotNull DeliveryMode deliveryMode,
    @Size(max = 500) String meetingLink,
    @Size(max = 1000) String locationAddress
) {}
```

- Validation liên field (`startTime < endTime`) đặt ở custom class-level validator hoặc Service; tránh controller `if` rải rác.
- Response mapper không trigger N+1. Query phải fetch đủ dữ liệu cần cho DTO.
- Field nhạy cảm không có trong public response type ngay từ thiết kế, không dựa vào `@JsonIgnore` tạm thời.
- Update DTO không dùng `Map<String,Object>`; định nghĩa field rõ ràng và gửi `version` cho entity có optimistic lock.

### 3.6. API response và exception

Các DTO chung:

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    PageMeta meta,
    Instant timestamp
) {}

public record ApiErrorResponse(
    boolean success,
    ApiError error,
    Instant timestamp,
    String requestId
) {}
```

- Dùng factory `ApiResponse.ok(data)`, `ApiResponse.created(data)`, `ApiResponse.page(data, meta)`.
- `@RestControllerAdvice` là nơi duy nhất map exception → `ErrorCode` → status/envelope.
- Error registry tuân theo `ERROR_CODES.md`; không trả raw `exception.getMessage()` cho client.
- Controller trả `ResponseEntity<ApiResponse<...>>` khi cần status/header; giữ một phong cách nhất quán.

### 3.7. Transaction, lock và state transition

- `@Transactional` đặt ở public application service method, không đặt ở Controller.
- Read-only query dùng `@Transactional(readOnly = true)` khi phù hợp.
- Booking lock theo đúng thứ tự toàn hệ thống: Teacher/User → Student/User → StudentPackage → conflict query → counters → insert Booking.
- Finance lock: StudentPackage → Wallet → idempotency check → LedgerEntry → balance.
- Không thay đổi thứ tự lock ở service khác; tránh deadlock.
- State transition được thể hiện bằng method có nghĩa (`booking.complete(report)`), không set status tùy ý từ controller.
- Complete Booking + SessionReport + counters + wallet/ledger nằm trong một transaction.
- Webhook và scheduled job phải idempotent; unique idempotency key là lớp bảo vệ bắt buộc.

### 3.8. Repository và query

- Method đơn giản dùng derived query có tên dễ đọc; query phức tạp dùng JPQL/native SQL/Specification có test.
- Method lock ghi rõ semantics, ví dụ `findByIdForUpdate` với `PESSIMISTIC_WRITE`.
- Không load list không giới hạn cho Message, Ledger, Booking.
- Public search phải dùng projection/pagination; không filter trên collection đã load trong memory.
- Tránh N+1 bằng query test/log ở môi trường dev.
- Native query PostgreSQL (GiST, `tstzrange`, JSONB) đặt ở repository chuyên biệt và có integration test Testcontainers.

### 3.9. Integration adapter

```text
payment/service → PaymentGateway (port)
payment/integration/payos → PayOsPaymentGateway (adapter)
```

- Cấu hình qua `@ConfigurationProperties`, không gọi `System.getenv()` rải rác.
- External call có connect/read timeout, retry giới hạn và circuit breaker.
- Chỉ retry operation an toàn/idempotent.
- Log provider reference/request ID, không log secret hoặc raw tài liệu nhạy cảm.

### 3.10. Logging và audit

- Dùng SLF4J parameterized logging: `log.info("Booking completed bookingId={}", bookingId)`.
- Không nối chuỗi tốn chi phí trong log.
- Log có `requestId/correlationId`; trả `requestId` trong error response.
- Không log password, JWT, refresh token, OAuth token, account number đầy đủ, payOS checksum key hoặc raw webhook chứa dữ liệu nhạy cảm.
- AuditLog là dữ liệu nghiệp vụ riêng, không thay bằng application log.

## 4. Frontend React/TypeScript

### 4.1. Cấu trúc theo feature

```text
frontend/src/
├─ app/
│  ├─ router/
│  ├─ providers/
│  └─ config/
├─ features/
│  ├─ auth/
│  ├─ marketplace/
│  ├─ teacher-profile/
│  ├─ packages/
│  ├─ bookings/
│  ├─ learning/
│  ├─ chat/
│  ├─ finance/
│  └─ admin/
├─ shared/
│  ├─ api/
│  ├─ components/
│  ├─ hooks/
│  ├─ lib/
│  ├─ types/
│  └─ constants/
└─ main.tsx
```

Mỗi feature có thể chứa `api/`, `components/`, `hooks/`, `pages/`, `schemas/`, `types/`. Không import internals sâu của feature khác; export qua `index.ts` có kiểm soát.

### 4.2. Quy tắc đặt tên Frontend

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Component/page | PascalCase file và symbol | `BookingCalendar.tsx` |
| Hook | `use` + Pascal/camel | `useCreateBooking.ts` |
| API module | kebab-case hoặc camelCase thống nhất | `booking-api.ts` |
| Utility | camelCase | `formatVnd.ts` |
| Type/interface | PascalCase | `BookingDetail` |
| Constant | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| Query key | factory theo feature | `bookingKeys.list(filters)` |
| Test | cùng tên `.test.ts(x)` | `BookingCalendar.test.tsx` |

- Component boolean prop dùng tiền tố `is/has/can/should`.
- Event handler nội bộ dùng `handleSubmit`; prop callback dùng `onSubmit`.
- Không prefix interface bằng `I` (`Booking`, không phải `IBooking`).

### 4.3. TypeScript

- Bật `strict`, `noUncheckedIndexedAccess` nếu khả thi; không dùng `any`. Dữ liệu chưa biết dùng `unknown` và parse/validate.
- Type API bám `API_CONTRACT.md`; tốt nhất generate từ OpenAPI khi có.
- Enum API ưu tiên string union để bundle nhẹ và nhận biết contract:

```ts
export type BookingStatus =
  | 'SCHEDULED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'EXPIRED';
```

- Không tự tính lại số tiền, commission hoặc counter nghiệp vụ ở FE; hiển thị dữ liệu server trả.
- UUID là `string`; timestamp là `string` tại API boundary, chuyển sang date object ở presentation/helper.

### 4.4. API client và TanStack Query

- Một Axios instance cấu hình base URL, auth header và normalize error.
- Refresh token phải single-flight: nhiều request 401 chỉ kích hoạt một lần refresh.
- Mutation tài chính tạo `Idempotency-Key`; không auto-retry mutation không idempotent.
- Query key chứa toàn bộ filter ảnh hưởng response.
- Sau mutation invalidate theo resource hẹp nhất; không `invalidateQueries()` toàn app.
- Component không gọi Axios trực tiếp; dùng feature API function/hook.
- API error có type `ApiError` và switch theo `error.code`.

### 4.5. Form, validation và UI state

- React Hook Form quản lý form; schema/client validation phải tương thích backend, nhưng backend vẫn là nguồn xác thực cuối.
- Server `fieldErrors` map về `setError(field, ...)`.
- Phân biệt loading ban đầu, background refetch, mutation pending, empty state và error state.
- Disable nút submit khi mutation đang chạy; không dùng disable như lớp chống double-submit duy nhất.
- Tiền hiển thị bằng `Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })`.
- Timestamp hiển thị rõ timezone, không cắt/ghép ISO string thủ công.

### 4.6. Routing và security UX

- Có `PublicRoute`, `AuthenticatedRoute`, `RoleGuard`, `TeacherApprovalGuard`.
- Guard chỉ phục vụ UX; Backend vẫn enforce mọi permission.
- Token hết hạn và refresh thất bại: xóa session, giữ safe return URL nếu phù hợp, điều hướng login.
- Không render dữ liệu nhạy cảm từ cache sau logout; clear QueryClient/session state.

### 4.7. WebSocket

- Một connection manager duy nhất, không mở connection theo từng component.
- Reconnect có exponential backoff và resubscribe.
- `clientMessageId` chống gửi trùng khi reconnect.
- REST load lịch sử; WebSocket nhận delta realtime.
- Unsubscribe khi không còn consumer; kiểm tra conversation membership vẫn thuộc Backend.

## 5. Database và Flyway

- File: `V<version>__<description>.sql`, ví dụ `V3__create_booking_tables.sql`.
- Migration đã chạy ở môi trường dùng chung không được sửa; tạo migration mới.
- DDL dùng snake_case, tên constraint/index rõ nghĩa:

```text
pk_bookings
fk_bookings_student_package
uk_teacher_subjects_teacher_subject
ck_bookings_time_range
idx_bookings_teacher_start_time
ex_bookings_teacher_time_overlap
```

- Mọi FK quan trọng chỉ rõ `ON DELETE` (`RESTRICT` là mặc định an toàn cho tài chính/lịch sử).
- Hibernate `ddl-auto=validate`; schema thay đổi qua Flyway.
- Migration liên quan PostgreSQL-specific feature phải được test bằng PostgreSQL Testcontainers, không thay bằng H2.

## 6. Formatting và static analysis

### Backend

- Java 21; dùng formatter cố định (Spotless với Palantir Java Format hoặc Google Java Format), chọn một và pin version.
- Build phải chạy compile, unit test, integration test phù hợp và static analysis.
- Không wildcard import.
- Constructor injection; không field injection `@Autowired`.
- Ưu tiên immutable DTO/value object và `final` khi giúp rõ intent.

### Frontend

- ESLint + TypeScript ESLint + React Hooks; Prettier pin version.
- Một style quote/semicolon/trailing comma do formatter quyết định, không tranh luận thủ công trong review.
- Không disable lint toàn file nếu có thể giới hạn một dòng và giải thích lý do.

## 7. Test convention

### Backend

- Unit test: business rule/state machine/rounding/permission, không cần Spring context.
- Repository/integration: `@DataJpaTest` hoặc Spring test với PostgreSQL Testcontainers.
- External adapter: fixture payOS, mock Cloudinary/email; không gọi production service.
- Tên test mô tả hành vi:

```java
@Test
void createBooking_whenTeacherTimeOverlaps_shouldThrowConflict() {}
```

- Test bắt buộc cho counter invariant, overlap, webhook idempotency, wallet settlement, payout reserve/release, refund rollback.

### Frontend

- Test hành vi người dùng bằng React Testing Library; tránh test implementation detail.
- Mock API ở network boundary (MSW) thay vì mock sâu hook/component.
- Có test route guard, error-code mapping, refresh single-flight và critical checkout/booking flows.

## 8. Git workflow

### 8.1. Branch

Nhánh ổn định:

- `main`: phiên bản demo/release chạy được.
- `develop`: tích hợp cho sprint nếu nhóm thực sự cần; nhóm nhỏ có thể trunk-based trực tiếp qua PR vào `main`.

Nhánh công việc:

```text
feature/EDU-123-booking-conflict-check
fix/EDU-204-webhook-idempotency
docs/EDU-015-api-contract
refactor/EDU-310-wallet-ledger
test/EDU-322-payout-integration
chore/EDU-001-docker-compose
```

- Lowercase kebab-case, có ticket nếu dùng task tracker.
- Một branch giải quyết một mục tiêu; không trộn refactor không liên quan.
- Branch sống ngắn; rebase/update thường xuyên theo quy ước nhóm.

### 8.2. Commit message

Dùng Conventional Commits:

```text
<type>(<scope>): <imperative summary>

[optional body]
[optional footer]
```

Ví dụ:

```text
feat(booking): prevent overlapping student schedules
fix(payment): make payOS webhook idempotent
docs(api): define payout request payload
test(finance): cover wallet reserve and release
```

Type cho phép: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `revert`.

- Summary tiếng Anh, imperative, không dấu chấm cuối, nên dưới 72 ký tự.
- Commit phải build được ở mức hợp lý; không dùng `wip` trên branch được merge.
- Breaking contract ghi `!` và footer `BREAKING CHANGE:`; chỉ thực hiện sau khi FE/BE cùng xác nhận.

### 8.3. Pull request

PR phải có:

- Mục tiêu và phạm vi.
- Contract/schema thay đổi gì.
- Cách test và kết quả.
- Screenshot/video nếu thay UI.
- Migration/rollback note nếu đổi database.
- Security/concurrency/idempotency impact nếu liên quan.

Không merge khi CI fail hoặc API behavior khác `API_CONTRACT.md`.

## 9. Definition of Done cho một endpoint

- Endpoint và DTO khớp API contract.
- Authentication, role và ownership được test.
- Bean Validation và business validation đầy đủ.
- Error code có trong registry/`ERROR_CODES.md`.
- Transaction/lock/idempotency được xem xét.
- Không lộ Entity hoặc dữ liệu nhạy cảm.
- Query phân trang và không có N+1 đáng kể.
- Unit/integration/security test tương xứng rủi ro.
- OpenAPI/Markdown contract được cập nhật cùng PR.
- Log/audit đúng yêu cầu, không chứa secret.

## 10. Các điều cấm quan trọng

- Không dùng `double`/`float` cho tiền.
- Không tin return URL payOS để xác nhận thanh toán.
- Không chỉ kiểm tra role ở Controller rồi bỏ qua ownership.
- Không thay đổi Wallet mà thiếu LedgerEntry cùng transaction.
- Không update/delete ledger lịch sử.
- Không tạo Booking bằng check-then-insert ngoài transaction/constraint.
- Không catch `Exception` rồi luôn trả `400`.
- Không trả stack trace hoặc raw vendor payload cho client.
- Không log JWT, password, refresh token, API key, checksum key hoặc account number đầy đủ.
- Không sửa migration đã được áp dụng ở môi trường dùng chung.
