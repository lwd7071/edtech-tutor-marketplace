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
    <div className="container mx-auto px-4 md:px-6 lg:px-8 py-8 max-w-[1320px]">
      <div className="flex flex-col gap-4 mb-8">
        <h1 className="text-h2 text-text-primary">Tìm kiếm Giáo viên</h1>
        <p className="text-text-secondary">Khám phá và kết nối với các giáo viên xuất sắc trên toàn quốc</p>
      </div>

      <TeacherSearchClient 
        initialFilters={params}
        initialTeachers={teachersResponse}
        subjects={subjectsResponse.data || []}
      />
    </div>
  );
}
