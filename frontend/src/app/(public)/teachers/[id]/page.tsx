import React from 'react';
import { 
  getTeacherDetail, 
  getTeacherPackages, 
  getTeacherAvailability, 
  getTeacherReviews 
} from '@/shared/api/public';
import { TeacherProfileHeader } from '@/features/marketplace/components/TeacherProfileHeader';
import TeacherDetailClient from './TeacherDetailClient';

export default async function TeacherDetailPage({
  params,
  searchParams
}: {
  params: { id: string };
  searchParams: { [key: string]: string | string[] | undefined };
}) {
  const teacherId = params.id;
  const reviewsPage = searchParams.reviewsPage ? parseInt(searchParams.reviewsPage as string, 10) - 1 : 0;
  const packagesPage = searchParams.packagesPage ? parseInt(searchParams.packagesPage as string, 10) - 1 : 0;

  // Fetch all data in parallel
  const [
    teacher,
    packagesResponse,
    availability,
    reviewsResponse
  ] = await Promise.all([
    getTeacherDetail(teacherId).catch(() => null),
    getTeacherPackages(teacherId, packagesPage, 20).catch(() => ({ data: [], meta: { page: 0, totalElements: 0, size: 20 } })),
    getTeacherAvailability(teacherId).catch(() => []),
    getTeacherReviews(teacherId, reviewsPage, 10).catch(() => ({ data: [], meta: { page: 0, totalElements: 0, size: 10 } }))
  ]);

  if (!teacher) {
    return (
      <div className="container mx-auto px-4 py-16 text-center">
        <h1 className="text-h2 text-text-primary mb-4">Không tìm thấy giáo viên</h1>
        <p className="text-text-secondary">Giáo viên này không tồn tại hoặc đã bị khóa.</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 md:px-6 lg:px-8 py-8 max-w-[1320px]">
      <TeacherProfileHeader teacher={teacher} />
      
      <TeacherDetailClient 
        teacher={teacher}
        packages={packagesResponse}
        availability={availability}
        reviews={reviewsResponse}
      />
    </div>
  );
}
