'use client';

import React from 'react';
import { Tabs } from 'antd';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { TeacherPublicDetail, PricingPackageView, AvailabilityView, Review, PageMeta } from '@/shared/api/public';
import { TeacherPackagesTab } from '@/features/marketplace/components/TeacherPackagesTab';
import { TeacherReviewsTab } from '@/features/marketplace/components/TeacherReviewsTab';
import WeeklyScheduleGrid from '@/shared/components/data-display/WeeklyScheduleGrid';
import { FileTextOutlined, BookOutlined, CalendarOutlined, StarOutlined } from '@ant-design/icons';

interface TeacherDetailClientProps {
  teacher: TeacherPublicDetail;
  packages: { data: PricingPackageView[]; meta: any };
  availability: AvailabilityView[];
  reviews: { data: Review[]; meta: any };
}

export default function TeacherDetailClient({
  teacher,
  packages,
  availability,
  reviews
}: TeacherDetailClientProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const handleReviewsPageChange = (page: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('reviewsPage', (page + 1).toString());
    router.push(`${pathname}?${params.toString()}`);
  };

  const handlePackagesPageChange = (page: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('packagesPage', (page + 1).toString());
    router.push(`${pathname}?${params.toString()}`);
  };

  const items = [
    {
      key: 'bio',
      label: <span style={{ fontSize: 'var(--text-body-lg)', fontWeight: 600 }}><FileTextOutlined /> Giới thiệu</span>,
      children: (
        <div style={{ backgroundColor: 'var(--color-surface)', padding: 'var(--space-6)', borderRadius: 'var(--radius-xl)', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)' }}>
          <h2 style={{ fontSize: 'var(--text-h4)', marginBottom: 'var(--space-4)' }}>Về giảng viên</h2>
          <p style={{ color: 'var(--color-text-primary)', whiteSpace: 'pre-line', fontSize: 'var(--text-body-lg)' }}>{teacher.bio || 'Chưa có thông tin giới thiệu.'}</p>
        </div>
      )
    },
    {
      key: 'packages',
      label: <span style={{ fontSize: 'var(--text-body-lg)', fontWeight: 600 }}><BookOutlined /> Gói học ({packages.meta?.totalElements || 0})</span>,
      children: (
        <TeacherPackagesTab packages={packages.data} />
      )
    },
    {
      key: 'availability',
      label: <span style={{ fontSize: 'var(--text-body-lg)', fontWeight: 600 }}><CalendarOutlined /> Lịch rảnh</span>,
      children: (
        <div style={{ backgroundColor: 'var(--color-surface)', padding: 'var(--space-6)', borderRadius: 'var(--radius-xl)', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)' }}>
          <WeeklyScheduleGrid 
            mode="readonly"
            availableSlots={availability.map(a => {
              const dayMap: Record<string, number> = {
                'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6, 'SUNDAY': 7
              };
              return {
                dayOfWeek: dayMap[a.dayOfWeek] || 1,
                startTime: a.startTime.substring(0, 5),
                endTime: a.endTime.substring(0, 5)
              };
            })}
          />
        </div>
      )
    },
    {
      key: 'reviews',
      label: <span style={{ fontSize: 'var(--text-body-lg)', fontWeight: 600 }}><StarOutlined /> Đánh giá ({reviews.meta?.totalElements || 0})</span>,
      children: (
        <TeacherReviewsTab 
          reviews={reviews.data} 
          meta={reviews.meta} 
          onPageChange={handleReviewsPageChange}
        />
      )
    }
  ];

  return (
    <div style={{ marginTop: 'var(--space-8)' }}>
      <Tabs 
        defaultActiveKey="bio" 
        items={items} 
        className="custom-tabs"
        size="large"
      />
    </div>
  );
}
