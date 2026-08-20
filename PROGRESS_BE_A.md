---
# Nhật ký triển khai Backend — Thành viên A

> File này là NHẬT KÝ, không phải đặc tả. Không được sửa các file 
> API_CONTRACT.md, CODING_CONVENTION.md, ERD.md, ERROR_CODES.md, PLANBE.md, 
> PLANFE.md, SETUP.md, SPEC.md để khớp với thay đổi ở đây — luôn làm ngược 
> lại: sửa code cho khớp các file đó.

## Quy tắc ghi log (đọc trước mỗi lần thực hiện task)

Sau MỖI task được giao (dù nhỏ hay lớn), agent phải thêm một entry mới vào 
cuối file này theo đúng format:

## [YYYY-MM-DD HH:mm] <Tên task ngắn gọn>
- Mục tiêu: <task được giao là gì>
- Căn cứ: <trích dẫn mục nào trong file .md nào làm cơ sở, ví dụ 
  "CODING_CONVENTION.md mục 3.1">
- File tạo mới/thay đổi/di chuyển: <liệt kê đầy đủ đường dẫn>
- Quyết định kỹ thuật đáng chú ý: <nếu có mâu thuẫn giữa các tài liệu hoặc 
  phải tự quyết định gì, ghi rõ lý do chọn phương án đó>
- Trạng thái: Done / Cần review / Blocked
- Việc còn thiếu / cần làm tiếp: <nếu có>

Không xoá hoặc sửa các entry cũ, chỉ append. Không tự tóm tắt lại lịch sử 
cũ để "gọn hơn".

---

## [2026-08-19 19:12] Sửa root package và tạo cây thư mục module
- Mục tiêu: Di chuyển root package từ `com.edtech.api` sang `com.edtech.platform` và khởi tạo cây thư mục cho các module do Thành viên A sở hữu.
- Căn cứ: CODING_CONVENTION.md mục 3.1
- File tạo mới/thay đổi/di chuyển: 
  - Di chuyển và sửa package: `backend/src/main/java/com/edtech/platform/ApiApplication.java`
  - Tạo mới các thư mục và `package-info.java` cho module `common` (config, exception, response, security, persistence, util) tại `backend/src/main/java/com/edtech/platform/common/` và `backend/src/test/java/com/edtech/platform/common/`.
  - Tạo mới các thư mục và `package-info.java` cho các module domain `auth`, `teacher`, `subject`, `catalog`, `learning`, `communication`, `ranking` (controller, dto/request, dto/response, service, domain, repository) cùng thư mục security riêng cho `auth` tại main và test.
  - Xoá thư mục cũ `backend/src/main/java/com/edtech/api`.
- Quyết định kỹ thuật đáng chú ý: Sử dụng script tự động sinh thư mục và `package-info.java` để đảm bảo Git có thể track được toàn bộ các directory rỗng chuẩn xác. Đã kiểm tra qua `pom.xml` và `application.yml` nhưng không phát hiện hard-code tên package cũ.
- Trạng thái: Done
- Việc còn thiếu / cần làm tiếp: Chưa có.

## [2026-08-19 19:24] Xây dựng package common cho Backend
- Mục tiêu: Thiết lập foundation cho persistence, response DTOs và global exception handling.
- Căn cứ: CODING_CONVENTION.md mục 3.4, 3.6, 6 và ERROR_CODES.md toàn bộ (đặc biệt mục 1, 2, 3-11, 12).
- File tạo mới/thay đổi/di chuyển:
  - `backend/src/main/java/com/edtech/platform/common/persistence/BaseEntity.java`
  - `backend/src/main/java/com/edtech/platform/common/response/PageMeta.java`
  - `backend/src/main/java/com/edtech/platform/common/response/ApiError.java`
  - `backend/src/main/java/com/edtech/platform/common/response/ApiResponse.java`
  - `backend/src/main/java/com/edtech/platform/common/response/ApiErrorResponse.java`
  - `backend/src/main/java/com/edtech/platform/common/exception/ErrorCode.java`
  - `backend/src/main/java/com/edtech/platform/common/exception/BusinessException.java`
  - `backend/src/main/java/com/edtech/platform/common/exception/ResourceNotFoundException.java`
  - `backend/src/main/java/com/edtech/platform/common/exception/ForbiddenResourceException.java`
  - `backend/src/main/java/com/edtech/platform/common/exception/DomainConcurrentModificationException.java`
  - `backend/src/main/java/com/edtech/platform/common/config/RequestIdFilter.java`
  - `backend/src/main/java/com/edtech/platform/common/exception/GlobalExceptionHandler.java`
- Quyết định kỹ thuật đáng chú ý: 
  1. Sử dụng `RequestIdFilter` (OncePerRequestFilter) và `MDC` để tự sinh `requestId` chuẩn ngẫu nhiên không dấu gạch ngang giống ULID.
  2. Đặt tên exception là `DomainConcurrentModificationException` thay vì `ConcurrentModificationException` để tránh xung đột với java.util.
  3. Dùng `@JsonInclude(JsonInclude.Include.NON_NULL)` cho field `meta` và `fieldErrors` để tuân thủ thiết kế null-safety trong response JSON.
- Trạng thái: Cần review
- Việc còn thiếu / cần làm tiếp: Không thể test `compile` qua lệnh do máy tính chưa cài đặt biến môi trường Maven (`mvn`), sẽ kiểm tra compile qua IDE.

## [2026-08-20 07:54] Xây dựng skeleton Security & Domain Event cho Backend
- Mục tiêu: Thiết lập JWT (stateless), security config, authentication entry points, event interface và aspect chặn quyền bằng annotation.
- Căn cứ: CODING_CONVENTION.md toàn bộ, SPEC.md dòng ~178-188 và ~1246-1251 (tắt CSRF, không log JWT/password, hạn access 15 phút), ERROR_CODES.md mục 4 và 13 (phân biệt 3 mã lỗi JWT).
- File tạo mới/thay đổi/di chuyển:
  - Sửa `backend/pom.xml`: Thêm jjwt (0.12.5) và spring-boot-starter-aop.
  - Sửa `backend/src/main/resources/application.yml`: Khai báo jwt secret, expires, cors allowed-origins.
  - `backend/src/main/java/com/edtech/platform/common/event/DomainEvent.java`
  - `backend/src/main/java/com/edtech/platform/common/event/AbstractDomainEvent.java`
  - `backend/src/main/java/com/edtech/platform/common/security/AuthenticatedUser.java`
  - `backend/src/main/java/com/edtech/platform/common/security/SecurityUtils.java`
  - `backend/src/main/java/com/edtech/platform/common/security/RequireRole.java`
  - `backend/src/main/java/com/edtech/platform/common/security/RequireRoleAspect.java`
  - `backend/src/main/java/com/edtech/platform/common/security/RestAuthenticationEntryPoint.java`
  - `backend/src/main/java/com/edtech/platform/common/security/RestAccessDeniedHandler.java`
  - `backend/src/main/java/com/edtech/platform/common/security/JwtTokenProvider.java`
  - `backend/src/main/java/com/edtech/platform/common/security/JwtAuthenticationFilter.java`
  - `backend/src/main/java/com/edtech/platform/common/config/SecurityConfig.java`
- Quyết định kỹ thuật đáng chú ý: 
  1. JWT secret được cấu hình nạp từ biến môi trường `${APP_JWT_SECRET}` kèm fallback dùng riêng cho dev.
  2. Origins truy cập (CORS) đọc qua thuộc tính `${APP_CORS_ALLOWED_ORIGINS}`, `allowCredentials` là `false`.
  3. `RestAccessDeniedHandler` cố định map lỗi `FORBIDDEN_RESOURCE`. Lý do: chặn quyền bằng `@RequireRole` (AOP) trực tiếp ném `BusinessException` với mã `ROLE_NOT_ALLOWED` nên không cần phân nhánh ở handler này.
  4. Bổ sung `spring-boot-starter-aop` để kích hoạt proxy bắt buộc cho custom annotation `@RequireRole`.
- Trạng thái: Cần review
- Việc còn thiếu / cần làm tiếp: IDE tự động nhận diện và build project, chưa thể test compile bằng script do hệ thống không có sẵn `mvn` command.

## [2026-08-20 08:02] Triển khai Module Auth (Đăng ký, Đăng nhập, Refresh, Đăng xuất)
- Mục tiêu: Xây dựng các tính năng xác thực cơ bản cho hệ thống, đảm bảo an toàn token và chuẩn hóa cấu trúc JWT.
- Căn cứ: 
  - ERD.md bảng `users`, `refresh_tokens`.
  - API_CONTRACT.md mục 2 (Auth API).
  - ERROR_CODES.md mục 4 (Mã lỗi Auth và account).
  - SPEC.md mục 5 (Xác thực và tài khoản, 5.1, 5.2, 5.3).
- File tạo mới/thay đổi/di chuyển:
  - `backend/src/main/java/com/edtech/platform/auth/domain/Role.java`
  - `backend/src/main/java/com/edtech/platform/auth/domain/UserStatus.java`
  - `backend/src/main/java/com/edtech/platform/auth/domain/User.java`
  - `backend/src/main/java/com/edtech/platform/auth/domain/RefreshToken.java`
  - `backend/src/main/java/com/edtech/platform/auth/repository/UserRepository.java`
  - `backend/src/main/java/com/edtech/platform/auth/repository/RefreshTokenRepository.java`
  - `backend/src/main/java/com/edtech/platform/auth/dto/request/RegisterRequest.java`
  - `backend/src/main/java/com/edtech/platform/auth/dto/request/LoginRequest.java`
  - `backend/src/main/java/com/edtech/platform/auth/dto/request/RefreshRequest.java`
  - `backend/src/main/java/com/edtech/platform/auth/dto/response/AuthResult.java`
  - `backend/src/main/java/com/edtech/platform/common/config/PasswordEncoderConfig.java`
  - `backend/src/main/java/com/edtech/platform/auth/service/AuthService.java`
  - `backend/src/main/java/com/edtech/platform/auth/controller/AuthController.java`
  - (Sửa) `backend/src/main/java/com/edtech/platform/common/config/SecurityConfig.java`
- Quyết định kỹ thuật đáng chú ý: 
  1. Chốt dùng **Redis TTL-based token** cho luồng verify-email/reset-password (sẽ phát triển ở bước sau) do bảo mật hơn (xoá được ngay sau khi dùng).
  2. Cho phép **đăng nhập thành công khi PENDING_VERIFICATION** (tối ưu UX, đúng Contract), việc chặn các luồng cụ thể sẽ thực hiện ở tầng API nghiệp vụ.
  3. Refresh Token được **mã hoá SHA-256** trước khi lưu DB.
  4. Bắt buộc thiết lập quan hệ `RefreshToken` -> `User` bằng `@ManyToOne(fetch = FetchType.LAZY)`.
  5. Luôn chuẩn hoá **email lowercase** trước khi query/lưu DB.
  6. Ở SecurityConfig, chỉ cấu hình `permitAll()` cho 3 endpoint `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`. Endpoint `/logout` vẫn được giữ nguyên tính xác thực (`authenticated`). IP được trích xuất hoàn toàn từ Request Context.
- Trạng thái: Cần review
- Việc còn thiếu / cần làm tiếp: Tiếp tục biên dịch trên IDE. Kế hoạch verify-email và reset-password sẽ thực hiện sau.
