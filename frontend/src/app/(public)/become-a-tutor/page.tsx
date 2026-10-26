import { ActionLink, PageActions } from '@/shared/components/navigation/NavigationLinks';

const onboardingSteps = [
  {
    title: 'Chuẩn bị hồ sơ',
    description: 'Giới thiệu bản thân, môn giảng dạy, kinh nghiệm và tài liệu xác minh.',
  },
  {
    title: 'Gửi xét duyệt',
    description: 'Hoàn thiện và gửi hồ sơ. Theo dõi trạng thái; bổ sung thông tin nếu nhận phản hồi.',
  },
  {
    title: 'Bắt đầu giảng dạy',
    description: 'Sau khi đáp ứng điều kiện, tạo gói học, cập nhật lịch rảnh và phản hồi yêu cầu học thử.',
  },
];

export default function Page() {
  return (
    <main className="tm-container tm-page tm-tutor-join">
      <header className="tm-tutor-join-heading">
        <p className="tm-eyebrow tm-tutor-join-kicker">Dành cho người dạy</p>
        <h1 className="tm-tutor-join-title">Kiến thức của bạn, bước tiến của học viên.</h1>
        <p className="tm-tutor-join-intro">
          Tạo hồ sơ, giới thiệu kinh nghiệm và tổ chức việc dạy học 1–1.
        </p>
      </header>

      <section className="tm-tutor-join-process" aria-labelledby="tm-tutor-join-process-title">
        <h2 id="tm-tutor-join-process-title">Bắt đầu theo từng bước</h2>
        <ol className="tm-tutor-join-steps">
          {onboardingSteps.map((step, index) => (
            <li className="tm-tutor-join-step" key={step.title}>
              <span className="tm-tutor-join-number" aria-hidden="true">0{index + 1}</span>
              <div className="tm-tutor-join-step-copy">
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <nav className="tm-tutor-join-actions" aria-label="Bắt đầu dạy cùng tutormatch">
        <PageActions>
          <ActionLink href="/auth/register?role=TEACHER">Đăng ký gia sư</ActionLink>
          <ActionLink href="/teacher/profile" variant="secondary">Tiếp tục hồ sơ</ActionLink>
        </PageActions>
      </nav>
    </main>
  );
}
