import React from 'react';
import { getPublicSubjects, getPublicTeachers } from '@/shared/api/public';
import HeroSearch from '@/features/marketplace/components/HeroSearch';
import SubjectGrid from '@/features/marketplace/components/SubjectGrid';
import TeacherGrid from '@/features/marketplace/components/TeacherGrid';
import TrustSection from '@/features/marketplace/components/TrustSection';
import CTASection from '@/features/marketplace/components/CTASection';
import { Typography } from 'antd';

const { Title, Paragraph } = Typography;

export default async function LandingPage() {
  let topSubjects: any[] = [];
  let topTeachers: any[] = [];
  
  // Parallel data fetching for subjects and teachers
  try {
    const [subjectsRes, teachersRes] = await Promise.all([
      getPublicSubjects({ page: 0, size: 8 }),
      getPublicTeachers({ sort: 'rating_desc', page: 0, size: 6 }),
    ]);
    topSubjects = subjectsRes.data;
    topTeachers = teachersRes.data;
  } catch (error) {
    console.error('Failed to fetch landing page data', error);
  }

  return (
    <div style={{ backgroundColor: 'var(--color-background, #FBFAF8)' }}>
      {/* Hero Section */}
      <section style={{ 
        padding: 'var(--space-20, 80px) 24px', 
        textAlign: 'center',
        backgroundColor: 'var(--color-surface, #FFFFFF)',
        borderBottom: '1px solid var(--color-border, #E7E3DC)'
      }}>
        <Title style={{ fontSize: 'var(--text-display, 48px)', marginBottom: 'var(--space-4, 16px)' }}>
          Học tập dễ dàng cùng chuyên gia
        </Title>
        <Paragraph type="secondary" style={{ fontSize: '18px', maxWidth: '600px', margin: '0 auto var(--space-8, 32px)' }}>
          Tìm kiếm gia sư phù hợp nhất để đạt được mục tiêu học tập của bạn. Hàng ngàn giáo viên xuất sắc đã sẵn sàng.
        </Paragraph>
        <HeroSearch />
      </section>

      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '0 16px' }}>
        {/* Featured Subjects Section */}
        <section style={{ margin: 'var(--space-16, 64px) 0' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-8, 32px)' }}>
            <Title level={2} style={{ margin: 0 }}>Môn học nổi bật</Title>
            <a href="/subjects" style={{ color: 'var(--color-primary-600, #0D9488)', fontWeight: 600 }}>Xem tất cả</a>
          </div>
          <SubjectGrid subjects={topSubjects} />
        </section>

        {/* Featured Teachers Section */}
        <section style={{ margin: 'var(--space-16, 64px) 0' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-8, 32px)' }}>
            <Title level={2} style={{ margin: 0 }}>Giáo viên nổi bật</Title>
            <a href="/teachers" style={{ color: 'var(--color-primary-600, #0D9488)', fontWeight: 600 }}>Xem tất cả</a>
          </div>
          <TeacherGrid teachers={topTeachers} />
        </section>

        {/* Trust Section */}
        <TrustSection />

        {/* CTA Section */}
        <CTASection />
      </div>
    </div>
  );
}
