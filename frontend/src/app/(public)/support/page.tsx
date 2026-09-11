import Link from 'next/link';

export default function SupportPage() {
  return <main className="tm-container tm-stack"><header className="tm-page-heading"><p className="tm-eyebrow">Trợ giúp</p><h1>Bạn cần hỗ trợ?</h1><p>Kiểm tra trạng thái ngay trong tài khoản trước khi liên hệ hỗ trợ.</p></header><section className="tm-panel tm-prose"><h2>Các mục thường dùng</h2><p><Link href="/student/requests">Yêu cầu học thử, hoàn tiền và gia hạn</Link></p><p><Link href="/student/notifications">Thông báo và kết quả xử lý</Link></p><p><Link href="/student/messages">Trao đổi với gia sư</Link></p><p>Nếu vấn đề chưa được giải quyết, hãy gửi mã giao dịch hoặc mã yêu cầu cho bộ phận vận hành Tutor Match.</p></section></main>;
}
