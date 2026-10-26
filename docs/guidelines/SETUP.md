# Hướng dẫn Cài đặt & Phát triển Dự án EdTech Marketplace

Dự án này bao gồm 2 phần chính: **Backend (Spring Boot)** và **Frontend (React/Vite)**, cùng với **PostgreSQL** và **Redis** được chạy qua Docker.

Tài liệu này hướng dẫn bạn cách thiết lập môi trường để **chạy dự án** và **tham gia phát triển (code)**.

---

## 1. Yêu cầu hệ thống (Prerequisites)
Trước khi bắt đầu, đảm bảo máy tính của bạn đã cài đặt các phần mềm sau:
- **Java 21** (JDK 21)
- **Node.js** (Phiên bản 18.x trở lên)
- **Docker & Docker Compose** (Để chạy Database và Cache)
- **Git**
- **IDE khuyên dùng:**
  - Backend: **IntelliJ IDEA** (Ultimate hoặc Community)
  - Frontend: **Visual Studio Code (VS Code)**

---

## 2. Clone dự án
Mở terminal và chạy lệnh sau để tải source code về máy:
```bash
git clone https://github.com/lwd7071/edtech-tutor-marketplace.git
cd edtech-tutor-marketplace
```

---

## 3. Khởi động Database & Cache (Docker)
Dự án sử dụng `docker-compose.yml` có sẵn ở thư mục gốc để khởi tạo nhanh PostgreSQL và Redis.

Tại thư mục gốc của dự án, chạy lệnh:
```bash
docker compose up -d
```
*Lệnh này sẽ tải image và chạy ngầm 2 container (edtech_postgres và edtech_redis).*

Thông tin cấu hình mặc định (xem trong `docker-compose.yml`):
- **PostgreSQL:** `localhost:5432`, DB: `edtech_db`, User: `edtech_user`, Pass: `edtech_password`
- **Redis:** `localhost:6379`

---

## 4. Hướng dẫn thiết lập Backend (Spring Boot) để Code

### 4.1. Mở dự án trong IntelliJ IDEA
1. Mở IntelliJ IDEA, chọn **Open** và trỏ đến thư mục `backend` của dự án *(Lưu ý: Không mở thư mục gốc `edtech-tutor-marketplace`, hãy mở thẳng thư mục `backend` để IDE nhận diện đúng project Maven)*.
2. Đợi IntelliJ nhận diện dự án Maven và tải các thư viện (Sync dependencies).
3. Đảm bảo IntelliJ sử dụng **JDK 21** cho project:
   - Vào `File` -> `Project Structure` -> `Project`.
   - Mục `SDK`: Chọn bản JDK 21 bạn đã cài.
   - Mục `Language level`: Chọn `21`.

### 4.2. Cài đặt Plugin cần thiết
- **Lombok:** Dự án Spring Boot thường dùng Lombok để giảm code boilerplate (như Getter, Setter). 
  - Bạn cần cài plugin Lombok trong IntelliJ (`Settings` -> `Plugins` -> tìm `Lombok`). 
  - Đảm bảo bật **Enable annotation processing** trong `Settings` -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors`.

### 4.3. Chạy và Debug Backend
- File chạy chính của Backend nằm ở: `src/main/java/com/edtech/platform/ApiApplication.java`.
- Bấm nút **Run** (mũi tên xanh) hoặc **Debug** (hình con bọ) bên cạnh class này để khởi động.
- Backend sẽ chạy ở cổng **8080** (`http://localhost:8080`). 
- *Lưu ý:* Khi chạy lần đầu, Flyway sẽ tự động chạy các file migration (nếu có) để tạo bảng trong Database PostgreSQL.

Chạy nhanh bằng Maven từ thư mục `backend`:

```powershell
mvn spring-boot:run
```

Profile mặc định là `cloud`. `mvn spring-boot:run` tự đọc `.env.cloud` ở thư mục `backend` hoặc thư mục gốc và fail-fast khi thiếu secret; không cần export thủ công từng biến. Kiểm tra tên biến trước khi chạy mà không in giá trị:

```powershell
.\scripts\check-backend-config.ps1 -Profile cloud
```

Cloudinary dùng ba biến `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`; ứng dụng tự ghép Cloudinary URL. `EDTECH_ACCOUNT_ENCRYPTION_KEY` phải có đúng 32 ký tự và phải giữ ổn định sau khi đã mã hóa dữ liệu tài khoản. Không tự tạo lại hoặc đổi khóa trên database đang có dữ liệu mã hóa.

Khi chủ động chạy hạ tầng local, đặt `SPRING_PROFILES_ACTIVE=local`. Local dùng PostgreSQL/Redis trên máy, mail logging, payment disabled và credential giả chỉ dành cho các adapter chưa được gọi.

Google OAuth dùng `GOOGLE_OAUTH_REDIRECT_URI` cho callback Backend và `APP_OAUTH2_FRONTEND_CALLBACK_URI` cho callback FE. Profile cloud không khởi động nếu thiếu `GOOGLE_CLIENT_ID` hoặc `GOOGLE_CLIENT_SECRET`.

Để chạy toàn bộ stack local bằng container:

```powershell
docker compose up -d --build backend
docker inspect --format '{{.State.Health.Status}}' edtech_backend
```

Health endpoint chuẩn là `http://localhost:8080/actuator/health`.

### 4.4. Deploy Backend lên Render

Repository có `render.yaml` tại thư mục gốc để cố định các thiết lập deploy: nhánh `main`, root directory `backend`, Docker runtime và health check `/actuator/health`. Docker image giới hạn heap JVM ở 256 MiB và giảm connection pool mặc định xuống 5 để còn đủ native memory trên Render Free 512 MiB.

Với service đã tạo thủ công trước đây, vào **Settings** và đối chiếu:

- Root Directory: `backend`
- Runtime: Docker; Dockerfile Path: `./Dockerfile`
- Health Check Path: `/actuator/health`
- Branch: `main`; Auto-Deploy bật cho commit

Giữ toàn bộ secret hiện có trong **Environment** của Render; không chép giá trị secret vào `render.yaml`. Sau khi push, deploy thành công chỉ khi `/actuator/health` trả `2xx` và Metrics không vượt giới hạn RAM. Nếu service vẫn vượt 512 MiB trong tải thật, chuyển sang plan có RAM lớn hơn thay vì tăng `-Xmx` trên Free.

### 4.5. Chạy production bằng Docker qua Cloudflare Tunnel

Compose riêng `docker-compose.tunnel.yml` chạy frontend, backend, Redis và connector `cloudflared`. Backend dùng profile `cloud`, lấy thông tin kết nối và secret từ `.env.cloud` ở thư mục gốc; không đưa file này vào image. Dữ liệu Redis được giữ trong volume `tunnel_redis_data`.

1. Cài Docker Desktop và bật Linux containers.
2. Nếu chưa có file token cục bộ, tạo từ mẫu:

   ```powershell
   Copy-Item .env.tunnel.example .env.tunnel
   ```

   Mở `.env.tunnel` và dán token tunnel `edtech` vào `CF_TUNNEL_TOKEN`. Không gửi hoặc commit token này.
3. Trong Cloudflare Dashboard, sửa route `tutormatch.id.vn` trỏ tới `http://frontend:3000` và thêm `api.tutormatch.id.vn` trỏ tới `http://backend:8080`. Hai hostname cần nằm trong cùng tunnel `edtech`; không giữ route trùng hostname ở phía trên.
   Kiểm tra mục **DNS → Records** của zone `tutormatch.id.vn`: cả hostname gốc (`@`) và `api` phải phân giải được. Nếu Cloudflare quản lý DNS toàn phần mà bản ghi chưa được tạo khi thêm route, thêm hai CNAME được proxy tới cùng `<Tunnel-ID>.cfargotunnel.com` của tunnel `edtech`. Lấy Tunnel ID trong Dashboard, không dùng token làm DNS target. Kiểm tra phân giải DNS trước khi thử HTTPS; thay đổi route nội bộ không sửa được hostname chưa phân giải.
4. Thêm callback OAuth Google `https://api.tutormatch.id.vn/login/oauth2/code/google` vào **Authorized redirect URIs** của OAuth client Web application và bấm Save. Client ID của OAuth client này phải trùng với `GOOGLE_CLIENT_ID` đang dùng trong `.env.cloud`; nếu đổi Client ID/Secret cục bộ, tạo lại container backend để nhận biến môi trường mới. Đăng nhập Google phải được kiểm tra runtime riêng sau khi thay đổi có hiệu lực.
   Luồng OAuth đi trực tiếp qua Spring backend và Google, rồi backend chuyển về `https://tutormatch.id.vn/oauth2/callback`. Supabase chỉ cung cấp PostgreSQL cho dữ liệu tài khoản trong cấu hình này; không cần thêm redirect URL vào Supabase Auth.
5. Kiểm tra và chạy stack:

   ```powershell
   docker compose --env-file .env.cloud --env-file .env.tunnel -f docker-compose.tunnel.yml config -q
   docker compose --env-file .env.cloud --env-file .env.tunnel -f docker-compose.tunnel.yml up -d --build
   docker compose --env-file .env.cloud --env-file .env.tunnel -f docker-compose.tunnel.yml ps
   ```

   Không dùng `docker compose config` không có `-q`, vì đầu ra có thể chứa giá trị môi trường.
6. Dừng stack mà vẫn giữ dữ liệu Redis:

   ```powershell
   docker compose --env-file .env.cloud --env-file .env.tunnel -f docker-compose.tunnel.yml down
   ```

Frontend chỉ bind vào `127.0.0.1:3000`; backend bind vào `127.0.0.1:18080` trên host để tránh xung đột với Tomcat đang dùng cổng 8080. Bên trong Docker, backend vẫn nghe cổng 8080 và connector truy cập `http://backend:8080` qua mạng Docker. `/health` công khai để health check; Actuator health không trả chi tiết và Prometheus yêu cầu tài khoản Admin. Khi đổi hostname, đồng bộ lại biến callback/CORS trong Compose và cấu hình OAuth bên Google.

---

## 5. Hướng dẫn thiết lập Frontend (React + Vite) để Code

### 5.1. Mở dự án trong VS Code
1. Mở VS Code.
2. Chọn `File` -> `Open Folder` và trỏ đến thư mục `frontend` của dự án *(tương tự như Backend, hãy mở đúng thư mục frontend để tránh lỗi cấu hình IDE)*.

### 5.2. Cài đặt các thư viện (Dependencies)
Mở Terminal tích hợp trong VS Code (phím tắt `` Ctrl + ` ``) và chạy lệnh:
```bash
npm install
```

### 5.3. Cài đặt Extension (Plugin) cần thiết
Để đảm bảo format code đồng nhất trong team, hãy cài các extension sau cho VS Code:
- **ESLint** (`dbaeumer.vscode-eslint`): Để bắt lỗi cú pháp và chuẩn code (conventions).
- **Prettier - Code formatter** (`esbenp.prettier-vscode`): Để tự động định dạng code cho đẹp.
- *Tip:* Bạn nên bật tính năng tự động Format khi lưu trong VS Code (Mở `Settings` -> tìm `Format On Save` và tích chọn).

### 5.4. Chạy và Debug Frontend
Khởi động môi trường phát triển Vite bằng lệnh Terminal trong VS Code:
```bash
npm run dev
```
- Frontend sẽ chạy trên trình duyệt, thường ở địa chỉ `http://localhost:5173`.
- Bất kỳ thay đổi nào trong thư mục `src/` sẽ tự động tải lại (HMR - Hot Module Replacement) và hiển thị ngay trên trình duyệt mà không cần F5.

---

## 6. Xử lý sự cố thường gặp (Troubleshooting)
- **Lỗi cổng (Port already in use):**
  - Nếu báo lỗi cổng `5432` hoặc `6379` khi chạy Docker, hãy chắc chắn máy bạn chưa chạy sẵn PostgreSQL/Redis nào khác.
  - Nếu báo lỗi cổng `8080` ở Backend, kiểm tra xem có ứng dụng web nào khác đang dùng port này không.
- **Lỗi Flyway migration (Backend):**
  - Không sửa migration đã áp dụng, không chạy `flyway repair` và không xóa database để bỏ qua checksum mismatch.
  - Đối chiếu migration với lịch sử Git, sửa nguyên nhân và tạo migration mới cho thay đổi schema tiếp theo.
- **Không Import được class trong IntelliJ:**
  - Chạy lại lệnh cập nhật Maven (Chuột phải vào `pom.xml` -> `Maven` -> `Reload project`).
