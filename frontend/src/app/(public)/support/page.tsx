import Link from 'next/link';

const helpTopics = [
  { href: '/student/requests', label: 'Yêu cầu học thử, hoàn tiền và gia hạn' },
  { href: '/student/notifications', label: 'Thông báo và kết quả xử lý' },
  { href: '/student/messages', label: 'Trao đổi với gia sư' },
];

export default function SupportPage() {
  return (
    <main className="tm-container tm-page tm-support-page">
      <header className="tm-support-heading">
        <p className="tm-eyebrow tm-support-kicker">Trợ giúp</p>
        <h1 className="tm-support-title">Bạn cần hỗ trợ?</h1>
        <p className="tm-support-intro">Kiểm tra trạng thái ngay trong tài khoản trước khi liên hệ hỗ trợ.</p>
      </header>

      <section className="tm-support-topics" aria-labelledby="tm-support-topics-title">
        <div className="tm-support-section-heading">
          <h2 id="tm-support-topics-title">Các mục thường dùng</h2>
          <p>Chọn nội dung cần tra cứu trong tài khoản của bạn.</p>
        </div>
        <ul className="tm-support-topic-list">
          {helpTopics.map(topic => (
            <li className="tm-support-topic" key={topic.href}>
              <Link className="tm-support-topic-link" href={topic.href}>{topic.label}</Link>
            </li>
          ))}
        </ul>
        <p className="tm-support-note">
          Nếu vấn đề chưa được giải quyết, hãy gửi mã giao dịch hoặc mã yêu cầu cho bộ phận vận hành Tutor Match.
        </p>
      </section>
    </main>
  );
}
