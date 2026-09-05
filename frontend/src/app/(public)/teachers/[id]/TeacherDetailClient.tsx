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
      label: <span className="text-base font-semibold"><FileTextOutlined /> Giới thiệu</span>,
      children: (
        <div className="bg-surface p-6 rounded-xl border border-border shadow-sm">
          <h2 className="text-h4 mb-4">Về giảng viên</h2>
          <p className="text-text-primary whitespace-pre-line text-base">{teacher.bio || 'Chưa có thông tin giới thiệu.'}</p>
        </div>
      )
    },
    {
      key: 'packages',
      label: <span className="text-base font-semibold"><BookOutlined /> Gói học ({packages.meta?.totalElements || 0})</span>,
      children: (
        <TeacherPackagesTab packages={packages.data} />
      )
    },
    {
      key: 'availability',
      label: <span className="text-base font-semibold"><CalendarOutlined /> Lịch rảnh</span>,
      children: (
        <div className="bg-surface p-6 rounded-xl border border-border shadow-sm">
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
      label: <span className="text-base font-semibold"><StarOutlined /> Đánh giá ({reviews.meta?.totalElements || 0})</span>,
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
    <div className="mt-8">
      <Tabs 
        defaultActiveKey="bio" 
        items={items} 
        className="custom-tabs"
        size="large"
      />
    </div>
  );
}
