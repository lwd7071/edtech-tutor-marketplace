import Link from 'next/link';

const privacySections = [
  {
    title: 'Thông tin tài khoản và học tập',
    content: 'Tutor Match lưu thông tin tài khoản, hồ sơ, giao dịch và hoạt động học tập cần thiết để cung cấp dịch vụ.',
  },
  {
    title: 'Thông tin liên hệ phụ huynh',
    content: 'Thông tin liên hệ phụ huynh là tùy chọn và chỉ được dùng để gửi email khi học viên bật tính năng thông báo. Bạn có thể cập nhật hoặc xóa thông tin này trong hồ sơ.',
  },
  {
    title: 'Dữ liệu xác thực và thanh toán',
    content: 'Dữ liệu xác thực và thanh toán được giới hạn theo vai trò; thông tin nhạy cảm không được hiển thị trong hồ sơ công khai.',
  },
];

export default function PrivacyPage() {
  return (
    <main className="tm-container tm-page tm-legal-page tm-privacy-page">
      <header className="tm-legal-heading">
        <p className="tm-eyebrow tm-legal-kicker">tutormatch</p>
        <h1 id="tm-privacy-title" className="tm-legal-title">Chính sách riêng tư</h1>
      </header>

      <article className="tm-privacy-copy" aria-labelledby="tm-privacy-title">
        {privacySections.map(section => (
          <section className="tm-privacy-section" key={section.title}>
            <h2>{section.title}</h2>
            <p>{section.content}</p>
          </section>
        ))}
      </article>

      <nav className="tm-legal-related" aria-label="Tài liệu liên quan">
        <Link href="/terms">Điều khoản sử dụng</Link>
      </nav>
    </main>
  );
}
