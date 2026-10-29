# Tài liệu chuyển UI sang Thymeleaf

## Mục đích

Bộ này là đặc tả xây dựng lại giao diện Tutor Match bằng Spring MVC + Thymeleaf, thay frontend Next.js. Đây là tài liệu thiết kế/triển khai; hợp đồng nghiệp vụ và API vẫn nằm ở tài liệu nguồn được liên kết bên dưới.

## Nguồn chuẩn

- [Mô hình nghiệp vụ](../../CONTEXT.md)
- [Spec frontend hiện hành](../architecture/FRONTEND_SPEC.md)
- [API contract hiện hành](../architecture/API_CONTRACT.md)
- [Mã lỗi](../architecture/ERROR_CODES.md)
- [ADR 0014: session browser và refresh race](../adr/0014-browser-session-identity-and-refresh-race.md)
- [ADR 0016: auth server-rendered](../adr/0016-server-rendered-authentication.md) — Accepted; production vẫn còn gate về session-store/topology.
- [ADR 0015: hệ thiết kế Tutor Match](../adr/0015-tutor-match-visual-system.md)
- [ADR 0012: quyền riêng tư residence/credential](../adr/0012-residence-and-credential-privacy.md)
- [ADR 0009: finance proof và audit](../adr/0009-server-owned-finance-proof-and-audit.md)

## Đọc theo thứ tự

1. [Phạm vi sản phẩm](PRODUCT_SCOPE.md)
2. [Route và trang](ROUTES_AND_PAGES.md)
3. [Hệ thiết kế](DESIGN_SYSTEM.md)
4. [Tương tác và accessibility](INTERACTIONS_AND_ACCESSIBILITY.md)
5. [Tích hợp API](API_INTEGRATION.md)
6. [Đăng nhập và bảo mật](AUTH_AND_SECURITY.md)
7. [Kiểm thử và nghiệm thu](TEST_AND_ACCEPTANCE.md)
8. [Tiến độ route](PROGRESS.md)
9. [Chuẩn bị trước triển khai](PREPARATION.md)

Không sao chép schema/DTO, error-code catalog hoặc invariant nghiệp vụ vào bộ này. Luôn mở tài liệu nguồn khi cần chi tiết. `PROGRESS.md` chỉ theo dõi route; trạng thái dự án tổng thể nằm ở [docs/STATUS.md](../STATUS.md).

## Trạng thái

Đây là contract triển khai, không phải tuyên bố hoàn tất. Chuyển đổi Thymeleaf đang được thực hiện theo từng route; SSR đã có ở nhiều nhóm public, student, teacher và admin, nhưng từng route chỉ được xem là hoàn tất sau khi có bằng chứng nghiệm thu tương ứng trong [PROGRESS.md](PROGRESS.md) và [docs/STATUS.md](../STATUS.md). REST API vẫn giữ contract JSON; giao diện dùng HTML handlers và session theo các tài liệu kiến trúc liên quan.

## Gate trước khi mở rộng toàn bộ route

- Dùng ADR 0016 đã chấp nhận cho authentication/session/CSRF/OAuth trên UI server-rendered; xem [AUTH_AND_SECURITY.md](AUTH_AND_SECURITY.md) để biết gate còn lại.
- Xác minh command tạo booking do Student và end-to-end trial flow; không dựng form trước khi command/ownership rõ.
- Thymeleaf starter, public/protected route security và template foundation đã được thêm. Tiếp tục theo dõi route parity tại [PROGRESS.md](PROGRESS.md).
- Chuyển route theo [PROGRESS.md](PROGRESS.md), giữ route parity với frontend nguồn cho tới khi có quyết định loại bỏ được duyệt.

## Kiến trúc tích hợp trang

Thymeleaf web controllers gọi application services/facades trực tiếp, không HTTP-loopback tới `/api/**`. JSON REST tiếp tục phục vụ API clients hiện tại. Form HTML bind riêng, validate server-side, gọi cùng command service rồi PRG. Không đưa entity/repository cross-module vào controller hoặc template.
