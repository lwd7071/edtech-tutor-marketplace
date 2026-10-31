# ADR 0018: Trợ lý hướng dẫn theo vai trò qua HTML session

**Status: Accepted — 2026-10-30**

## Bối cảnh

Trợ lý public ban đầu ép trả tiếng Việt, tìm nguồn bằng alias cố định và widget không nhận danh tính HTML session. Nguồn DB V43 chỉ có năm bài public nên không đủ hướng dẫn cho Student, Teacher và Admin. REST `/api/**` theo ADR 0016 chỉ xác thực bằng Bearer JWT.

## Quyết định

- Dùng playbook có phiên bản trong repo làm nguồn hướng dẫn; bài viết chỉ chứa quy trình và đường dẫn, không chứa dữ liệu tài khoản. Bảng V43 giữ nguyên nhưng không được đọc trong luồng runtime mới.
- Groq phân loại chủ đề, điều kiện tìm gia sư và ngôn ngữ; backend lọc kết quả theo role và allowlist. Câu trả lời chỉ dựa vào playbook hoặc public catalog. Link nguồn do server chọn.
- Widget gửi `POST /assistant/chat` trong HTML security chain với CSRF và session principal. REST `/api/public/assistant/chat` luôn ở phạm vi Guest. Không truyền JWT ra browser hoặc cho session cookie xác thực REST.
- Không đọc dữ liệu cá nhân, tài chính hay hàng đợi vận hành; không cho bot thực hiện command. Hội thoại chỉ ở browser và không persist tại backend.

## Hệ quả

Nội dung thay đổi cần review và deploy cùng code. Mỗi câu hỏi thường cần một lượt Groq để phân loại và một lượt để trả lời; cần đo chất lượng/độ trễ bằng smoke test provider thật trước khi công bố sẵn sàng production.
