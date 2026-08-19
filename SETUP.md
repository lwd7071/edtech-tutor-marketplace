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
docker-compose up -d
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
- File chạy chính của Backend nằm ở: `src/main/java/com/edtech/api/ApiApplication.java`.
- Bấm nút **Run** (mũi tên xanh) hoặc **Debug** (hình con bọ) bên cạnh class này để khởi động.
- Backend sẽ chạy ở cổng **8080** (`http://localhost:8080`). 
- *Lưu ý:* Khi chạy lần đầu, Flyway sẽ tự động chạy các file migration (nếu có) để tạo bảng trong Database PostgreSQL.

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
  - Nếu thay đổi cấu trúc bảng, đôi khi Flyway sẽ báo lỗi sai Checksum. Trong môi trường dev, bạn có thể xóa toàn bộ database (xóa volume trong docker) và chạy lại. 
  - Lệnh xóa data cũ của Docker: `docker-compose down -v`
- **Không Import được class trong IntelliJ:**
  - Chạy lại lệnh cập nhật Maven (Chuột phải vào `pom.xml` -> `Maven` -> `Reload project`).
