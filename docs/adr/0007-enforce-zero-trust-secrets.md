# 7. Enforce Zero-Trust Secrets

Date: 2026-09-12

## Status

Accepted

## Context

Trong quá trình khởi tạo dự án, các file cấu hình như `application.yml` thường được set sẵn các giá trị mặc định cho những thông số nhạy cảm (như `APP_JWT_SECRET` hay `EDTECH_ACCOUNT_ENCRYPTION_KEY`). Việc này giúp các nhà phát triển dễ dàng khởi chạy môi trường local mà không cần thiết lập biến môi trường thủ công.

Tuy nhiên, việc cung cấp giá trị dự phòng (fallback) cho secret mang lại rủi ro rất lớn ở môi trường Cloud/Production:
- Nếu một DevOps engineer quên cung cấp cấu hình biến môi trường, ứng dụng vẫn khởi động thành công mà không báo lỗi, nhưng nó sẽ sử dụng secret mặc định (thường là mã cố định, dễ bị dò rỉ hoặc đoán ra).
- Kẻ tấn công có thể dễ dàng giải mã được JWT tokens hoặc dữ liệu nhạy cảm nếu chúng biết được secret mặc định này nằm trong mã nguồn.

## Decision

Chúng tôi quyết định áp dụng nguyên tắc **Zero-Trust Secrets** (Không tin cậy mã bí mật mặc định):
1. **Không hardcode mặc định cho secret:** Xóa bỏ toàn bộ fallback cho các key và secret trong `application.yml` và `application-cloud.yml` (chỉ sử dụng syntax `${VAR_NAME}` thay vì `${VAR_NAME:fallback}`).
2. **Fail-Fast (Chết sớm):** Ứng dụng phải văng exception (crash) ngay lúc khởi động nếu biến môi trường bị thiếu, không được phép im lặng sử dụng giá trị yếu.
3. **Môi trường Local:** Đối với môi trường phát triển (`application-local.yml`), chúng tôi có thể giữ các chuỗi nội bộ để giảm ma sát khi dev, nhưng chúng phải được cấu hình tách biệt hoàn toàn và không được kế thừa lên production profile.

## Consequences

- Các nhà phát triển và đội DevOps bắt buộc phải cung cấp đầy đủ các biến môi trường nhạy cảm trong file `.env.cloud` hoặc qua hệ thống quản lý secret (như AWS Secrets Manager) khi deploy.
- Tăng độ an toàn và tính minh bạch cho quy trình vận hành. Ứng dụng sẽ báo ngay cấu hình thiếu sót qua log.

Cloudinary tuân thủ cùng quy tắc: base/cloud chỉ dùng `${CLOUDINARY_URL}`; giá trị giả chỉ được phép ở profile local/test.
