import Link from 'next/link';
import { getPublicSubjectsServer, getPublicTeachersServer } from '@/shared/api/public.server';
import HeroSearch from '@/features/marketplace/components/HeroSearch';
import SubjectGrid from '@/features/marketplace/components/SubjectGrid';
import TeacherGrid from '@/features/marketplace/components/TeacherGrid';
import PublicDataRecovery from '@/features/marketplace/components/PublicDataRecovery';
import { InlineActionLink } from '@/shared/components/navigation/NavigationLinks';
import ScrollReveal from '@/features/marketplace/components/ScrollReveal';
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
    <>
      <PublicDataRecovery hasError={hasPublicDataError} />
      <section className="tm-hero tm-home-hero">
        <div className="tm-container">
          <div className="tm-hero-grid">
            <div className="tm-hero-copy">
              <p className="tm-hero-kicker">Gia sư 1 kèm 1 trực tuyến</p>
              <h1>Học đúng người.<br /><span>Tiến đúng mục tiêu.</span></h1>
              <p className="tm-lead">Kết nối với gia sư phù hợp để học hiệu quả hơn, chủ động hơn và gần hơn với mục tiêu của bạn.</p>
              <HeroSearch />
            </div>
            <div className="tm-hero-visual">
              <Image src="/images/tutor-hero-concept.png" alt="Học viên viết bài trong buổi học trực tuyến cùng gia sư" width={1536} height={1024} priority className="tm-hero-illustration" />
            </div>
          </div>
        </div>
      </section>

      <div className="tm-container">
        <ScrollReveal className="tm-section tm-home-subjects" aria-labelledby="home-subjects-title">
          <div className="tm-section-head">
            <div><h2 id="home-subjects-title">Khám phá các môn học phổ biến</h2></div>
            <InlineActionLink href="/subjects">Tất cả môn học</InlineActionLink>
          </div>
          <SubjectGrid subjects={subjects.status === 'fulfilled' ? subjects.value.data : []} isError={subjects.status === 'rejected'} />
        </ScrollReveal>

        <div className="tm-home-action-row">
          <ScrollReveal className="tm-section tm-how-section tm-home-how" aria-labelledby="home-how-title">
            <div className="tm-section-head"><h2 id="home-how-title">Cách hoạt động</h2></div>
            <div className="tm-steps">
            {learningSteps.map(step => (
              <article className="tm-step" key={step.key}>
                <div className="tm-how-step-top">
                  <span className="tm-how-step-badge">{step.step}</span>
                  <span className="tm-how-step-art"><HomeIcon name={step.icon} /></span>
                </div>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </article>
            ))}
            </div>
          </ScrollReveal>
          <ScrollReveal className="tm-tutor-invite tm-home-invite" aria-labelledby="home-tutor-title">
          <div className="tm-tutor-invite-copy">
            <h2 id="home-tutor-title">Trở thành gia sư</h2>
            <p>Chia sẻ kiến thức, đồng hành cùng học viên và tạo ra giá trị thật.</p>
            <Link href="/become-a-tutor" className="tm-button">Đăng ký làm gia sư <span aria-hidden="true">→</span></Link>
          </div>
          <Image src="/images/tutor-teacher-illustration.png" alt="Gia sư hướng dẫn học viên trong lớp trực tuyến" width={1456} height={1024} className="tm-tutor-invite-art" />
          </ScrollReveal>
        </div>

        <ScrollReveal className="tm-intro tm-section" aria-labelledby="home-intro-title">
          <div className="tm-intro-copy">
            <h2 id="home-intro-title">Một người đồng hành phù hợp tạo nên khác biệt.</h2>
            <p>Tìm người đồng hành theo môn học, mục tiêu và lịch học của bạn. Mỗi hồ sơ giúp bạn hiểu rõ kinh nghiệm, cách dạy và gói học trước khi bắt đầu.</p>
            <ul className="tm-intro-points">
              <li><HomeIcon name="check" /><span>Chọn gia sư theo môn và mục tiêu học tập</span></li>
              <li><HomeIcon name="check" /><span>Trao đổi trước để thống nhất cách học</span></li>
              <li><HomeIcon name="check" /><span>Quản lý lịch và bài tập trong tài khoản</span></li>
            </ul>
          </div>
          <div className="tm-intro-art"><Image src="/images/tutor-learning-illustration.png" alt="Học viên trao đổi bài cùng gia sư trong lớp học trực tuyến" width={1672} height={941} className="tm-intro-illustration" /></div>
        </ScrollReveal>
        <ScrollReveal className="tm-section" aria-labelledby="home-teachers-title">
          <div className="tm-section-head"><div><h2 id="home-teachers-title">Gặp những gia sư được học viên tin chọn.</h2><p>Khám phá hồ sơ thật, kinh nghiệm, môn dạy và gói học đang được cung cấp.</p></div><InlineActionLink href="/teachers">Tất cả gia sư</InlineActionLink></div>
          <TeacherGrid teachers={featuredTeachers} isError={teachers.status === 'rejected'} />
        </ScrollReveal>
      </div>
    </>
  );
}
