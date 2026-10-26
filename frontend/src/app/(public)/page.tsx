import Link from 'next/link';
import { getPublicSubjectsServer, getPublicTeachersServer } from '@/shared/api/public.server';
import HeroSearch from '@/features/marketplace/components/HeroSearch';
import SubjectGrid from '@/features/marketplace/components/SubjectGrid';
import TeacherGrid from '@/features/marketplace/components/TeacherGrid';
import PublicDataRecovery from '@/features/marketplace/components/PublicDataRecovery';
import { InlineActionLink } from '@/shared/components/navigation/NavigationLinks';
import Image from 'next/image';
import HomeIcon from '@/features/marketplace/components/HomeIcon';

export const revalidate = 30;

export default async function LandingPage() {
  const [subjects, teachers] = await Promise.allSettled([
    getPublicSubjectsServer({ page: 0, size: 9 }),
    getPublicTeachersServer({ sort: 'rating_desc', page: 0, size: 6 }),
  ]);
  const hasPublicDataError = subjects.status === 'rejected' || teachers.status === 'rejected';
  const featuredTeachers = teachers.status === 'fulfilled' ? teachers.value.data : [];
  const learningSteps = [
    { key: 'choose', step: '01', icon: 'search' as const, title: 'Chọn gia sư', description: 'So sánh môn dạy, kinh nghiệm và gói học trong hồ sơ.' },
    { key: 'connect', step: '02', icon: 'message' as const, title: 'Trao đổi nhu cầu', description: 'Gửi yêu cầu học thử để gia sư phản hồi hoặc chọn mua gói học.' },
    { key: 'learn', step: '03', icon: 'video' as const, title: 'Bắt đầu học', description: 'Sau khi thống nhất, gia sư tạo buổi học. Theo dõi lịch và bài tập trong tài khoản.' },
  ];

  return (
    <main className="tm-home-page">
      <PublicDataRecovery hasError={hasPublicDataError} />
      <section className="tm-hero tm-home-hero" aria-labelledby="home-title">
        <header className="tm-container tm-home-hero-container">
          <div className="tm-home-hero-layout">
            <header className="tm-home-hero-copy">
              <div className="tm-home-hero-message">
                <h1 id="home-title">Học đúng người.<br /><strong>Tiến đúng mục tiêu.</strong></h1>
                <p className="tm-lead tm-home-hero-lead">Kết nối với gia sư phù hợp để học hiệu quả hơn, chủ động hơn và gần hơn với mục tiêu của bạn.</p>
              </div>
              <div className="tm-home-hero-action"><HeroSearch /></div>
            </header>
            <figure className="tm-home-hero-art">
              <Image src="/images/tutor-hero-concept.png" alt="Học viên viết bài trong buổi học trực tuyến cùng gia sư" width={1536} height={1024} priority sizes="(max-width: 760px) 100vw, 52vw" className="tm-hero-illustration" />
            </figure>
          </div>
        </header>
      </section>

      <div className="tm-container tm-home-content">
        <section className="tm-section tm-home-subjects" aria-labelledby="home-subjects-title">
          <header className="tm-home-section-head">
            <h2 id="home-subjects-title" className="tm-home-subject-title">Khám phá các môn học phổ biến</h2>
            <InlineActionLink href="/subjects" className="tm-home-subject-action">Tất cả môn học</InlineActionLink>
          </header>
          <div className="tm-home-subject-list">
            <SubjectGrid subjects={subjects.status === 'fulfilled' ? subjects.value.data : []} isError={subjects.status === 'rejected'} />
          </div>
        </section>

        <section className="tm-home-guidance" aria-label="Bắt đầu học cùng gia sư">
          <section className="tm-home-how" aria-labelledby="home-how-title">
            <h2 id="home-how-title">Cách hoạt động</h2>
            <ol className="tm-home-steps" aria-label="Các bước học cùng gia sư">
              {learningSteps.map(step => (
                <li className="tm-home-step" key={step.key}>
                  <span className="tm-home-step-number" aria-hidden="true">{step.step}</span>
                  <div className="tm-home-step-content">
                    <h3 className="tm-home-step-title">{step.title}</h3>
                    <p className="tm-home-step-description">{step.description}</p>
                  </div>
                </li>
              ))}
            </ol>
          </section>

          <aside className="tm-home-tutor-invite" aria-labelledby="home-tutor-title">
            <div>
              <h2 id="home-tutor-title" className="tm-home-tutor-title">Trở thành gia sư</h2>
              <p className="tm-home-tutor-description">Chia sẻ kiến thức, đồng hành cùng học viên và tạo ra giá trị thật.</p>
              <Link href="/become-a-tutor" className="tm-button tm-home-tutor-cta">Đăng ký làm gia sư</Link>
            </div>
            <Image src="/images/tutor-teacher-illustration.png" alt="" width={1456} height={1024} className="tm-home-tutor-art" />
          </aside>
        </section>

        <section className="tm-home-story" aria-labelledby="home-story-title">
          <header className="tm-home-story-heading">
            <h2 id="home-story-title">Một người đồng hành phù hợp tạo nên khác biệt.</h2>
          </header>
          <div className="tm-home-story-body">
            <div className="tm-home-story-copy">
            <p className="tm-home-story-intro">Tìm người đồng hành theo môn học, mục tiêu và lịch học của bạn. Mỗi hồ sơ giúp bạn hiểu rõ kinh nghiệm, cách dạy và gói học trước khi bắt đầu.</p>
            <ul className="tm-intro-points tm-home-story-points">
              <li className="tm-home-story-point"><HomeIcon name="check" /><span>Chọn gia sư theo môn và mục tiêu học tập</span></li>
              <li className="tm-home-story-point"><HomeIcon name="check" /><span>Trao đổi trước để thống nhất cách học</span></li>
              <li className="tm-home-story-point"><HomeIcon name="check" /><span>Quản lý lịch và bài tập trong tài khoản</span></li>
            </ul>
            </div>
            <figure className="tm-home-story-art">
              <Image src="/images/tutor-learning-illustration.png" alt="Học viên trao đổi bài cùng gia sư trong lớp học trực tuyến" width={1672} height={941} className="tm-intro-illustration" />
            </figure>
          </div>
        </section>

        <section className="tm-section tm-home-teachers" aria-labelledby="home-teachers-title">
          <div className="tm-home-teachers-introduction">
            <header className="tm-home-teachers-copy">
              <h2 id="home-teachers-title" className="tm-home-teachers-title">Gặp những gia sư được học viên tin chọn.</h2>
              <p className="tm-home-teachers-description">Khám phá hồ sơ thật, kinh nghiệm, môn dạy và gói học đang được cung cấp.</p>
            </header>
            <div className="tm-home-teachers-action">
              <InlineActionLink href="/teachers" className="tm-home-teachers-action-link">Tất cả gia sư</InlineActionLink>
            </div>
          </div>
          <TeacherGrid teachers={featuredTeachers} isError={teachers.status === 'rejected'} />
        </section>
      </div>
    </main>
  );
}
