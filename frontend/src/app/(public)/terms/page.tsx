import Link from 'next/link';

export default function TermsPage() {
  return (
    <main className="tm-container tm-page tm-legal-page tm-terms-page">
      <header className="tm-legal-heading">
        <p className="tm-eyebrow tm-legal-kicker">tutormatch</p>
        <h1 id="tm-terms-title" className="tm-legal-title">Điều khoản sử dụng</h1>
      </header>

      <article className="tm-legal-copy" aria-labelledby="tm-terms-title">
        <p>Khi sử dụng Tutor Match, bạn đồng ý cung cấp thông tin chính xác, bảo vệ tài khoản và sử dụng nền tảng cho mục đích học tập hợp pháp.</p>
        <p>Thông tin gói học, lịch học, hoàn tiền và gia hạn được xác nhận tại thời điểm giao dịch. Các bên cần trao đổi và thực hiện buổi học qua đúng tài khoản đã đăng ký.</p>
        <p>Điều khoản này là bản tối thiểu trong giai đoạn MVP và sẽ được cập nhật trước khi phát hành chính thức.</p>
      </article>

      <nav className="tm-legal-related" aria-label="Tài liệu liên quan">
        <Link href="/privacy">Chính sách riêng tư</Link>
      </nav>
    </main>
  );
}
