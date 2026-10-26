import { ActionLink, PageActions } from '@/shared/components/navigation/NavigationLinks';

const steps = [
  {
    title: 'Tìm người phù hợp',
    description: 'Lọc gia sư theo môn, hình thức, giá gói và giờ rảnh. Xem giới thiệu, gói học và đánh giá.',
  },
  {
    title: 'Chọn cách bắt đầu',
    description: 'Gửi yêu cầu học thử để gia sư phản hồi, hoặc chọn gói và thanh toán. Yêu cầu học thử chưa phải lịch học đã xác nhận.',
  },
  {
    title: 'Theo dõi việc học',
    description: 'Gia sư tạo buổi học sau khi thống nhất. Bạn xem lịch, nộp bài tập và theo dõi gói đã mua trong không gian học tập.',
  },
];

export default function Page() {
  return (
    <main className="tm-container tm-page tm-how-page">
      <header className="tm-how-heading">
        <h1 className="tm-how-title">Từ tìm gia sư đến buổi học đầu tiên</h1>
        <p className="tm-how-kicker">Hướng dẫn bắt đầu</p>
        <p className="tm-how-description">
          Chọn người hướng dẫn, thống nhất nhu cầu và theo dõi việc học trong một tài khoản.
        </p>
      </header>

      <section className="tm-how-steps-section" aria-label="Các bước học cùng gia sư">
        <ol className="tm-how-steps">
          {steps.map((step, index) => (
            <li className="tm-how-step" key={step.title}>
              <span className="tm-how-number" aria-hidden="true">0{index + 1}</span>
              <article className="tm-how-step-content">
                <h2>{step.title}</h2>
                <p>{step.description}</p>
              </article>
            </li>
          ))}
        </ol>
      </section>

      <nav className="tm-how-actions" aria-label="Bắt đầu tại tutormatch">
        <PageActions>
          <ActionLink href="/teachers">Tìm gia sư</ActionLink>
          <ActionLink href="/become-a-tutor" variant="secondary">Trở thành gia sư</ActionLink>
        </PageActions>
      </nav>
    </main>
  );
}
