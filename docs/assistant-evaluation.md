# Bộ câu hỏi nghiệm thu trợ lý

Bộ này dùng cho smoke test Groq thật và browser QA. Câu hỏi không chứa dữ liệu cá nhân. Mỗi dòng cần kiểm tra ngôn ngữ trả lời, nội dung đúng nguồn và link thuộc role; không chấm pass từ mock Groq.

| Role | Câu hỏi | Kỳ vọng |
|---|---|---|
| Guest | How do I create an account? | Tiếng Anh; hướng dẫn đăng ký; `/auth/register` |
| Guest | Tôi quên mật khẩu thì làm sao? | Tiếng Việt; `/auth/forgot-password` |
| Guest | Find a high school math tutor | Tiếng Anh; chỉ gia sư công khai phù hợp hoặc `/teachers` nếu không có |
| Guest | Thủ đô Pháp là gì? | Ngoài phạm vi; chỉ hướng kênh hỗ trợ |
| Student | Mình mới mua gói, giờ đặt buổi ở đâu? | `/student/bookings`; phân biệt yêu cầu với buổi đã xác nhận |
| Student | Can I ask for a refund? | Tiếng Anh; `/student/requests`; không phỏng đoán tình trạng yêu cầu |
| Student | Giúp mình | Hỏi lại một câu cụ thể |
| Teacher | Hồ sơ bị chờ duyệt, phải làm gì? | `/teacher/profile`; không khẳng định hồ sơ hiện tại đã được duyệt |
| Teacher | How do I create a teaching package? | Tiếng Anh; `/teacher/packages` |
| Teacher | Rút tiền ở đâu? | `/teacher/payouts`; không đọc số dư |
| Admin | How do I review refunds? | Tiếng Anh; `/admin/refunds`; không nêu dữ liệu hàng đợi |
| Admin | Quyết toán buổi học đang tranh chấp xử lý ở đâu? | `/admin/booking-settlements`; không quyết định thay admin |

Kiểm tra chuyển ngôn ngữ trong cùng hội thoại: hỏi “Đặt lịch thế nào?”, tiếp theo “Explain that in English”, rồi “Nói lại bằng tiếng Việt”. Kiểm tra phiên hết hạn trở về Guest; request tự khai `role=ADMIN` không mở nguồn Admin; nội dung yêu cầu bot bỏ qua quy tắc không làm lộ playbook sai role.

Baseline code trước thay đổi: prompt ép mọi câu trả lời tiếng Việt; bước tìm nguồn chỉ nhận alias/cụm từ cố định; widget gọi API public nên mọi role chỉ có nguồn Guest. So sánh chất lượng thực tế của phiên mới cần Groq thật và browser QA, chưa thể suy ra từ unit test.
