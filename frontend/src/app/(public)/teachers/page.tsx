import React from 'react';
import { getPublicTeachers, getPublicSubjects, TeacherSearchParams } from '@/shared/api/public';
import TeacherSearchClient from './TeacherSearchClient';

export default async function TeachersPage({
  searchParams,
}: {
  searchParams: { [key: string]: string | string[] | undefined };
}) {
  const page = searchParams.page ? parseInt(searchParams.page as string, 10) - 1 : 0;
  const size = searchParams.size ? parseInt(searchParams.size as string, 10) : 12;

  const params: TeacherSearchParams = {
    keyword: searchParams.keyword as string | undefined,
    subjectId: searchParams.subjectId as string | undefined,
    dayOfWeek: searchParams.dayOfWeek as string | undefined,
    startTime: searchParams.startTime as string | undefined,
    endTime: searchParams.endTime as string | undefined,
    minPrice: searchParams.minPrice ? Number(searchParams.minPrice) : undefined,
    maxPrice: searchParams.maxPrice ? Number(searchParams.maxPrice) : undefined,
    minRating: searchParams.minRating ? Number(searchParams.minRating) : undefined,
    deliveryMode: searchParams.deliveryMode as string | undefined,
    sort: searchParams.sort as string | undefined,
    page,
    size,
  };

  const [teachersResponse, subjectsResponse] = await Promise.all([
    getPublicTeachers(params).catch(() => ({ data: [], meta: { page: 0, totalElements: 0, size: 12 } })),
    getPublicSubjects({ size: 100 }).catch(() => ({ data: [], meta: {} }))
  ]);

  return (
    <div style={{ maxWidth: 'var(--size-container-wide)', margin: '0 auto', padding: 'var(--space-8) var(--space-4)' }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)', marginBottom: 'var(--space-8)' }}>
        <h1 style={{ fontSize: 'var(--text-h2)', color: 'var(--color-text-primary)', margin: 0 }}>Tìm kiếm Giáo viên</h1>
        <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>Khám phá và kết nối với các giáo viên xuất sắc trên toàn quốc</p>
      </div>

      <TeacherSearchClient 
        initialFilters={params}
        initialTeachers={teachersResponse}
        subjects={subjectsResponse.data || []}
      />
    </div>
  );
}
