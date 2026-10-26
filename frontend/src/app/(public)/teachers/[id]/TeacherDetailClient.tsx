'use client';

import { Alert, Button, Pagination, Spin } from 'antd';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import type { AvailabilityView, PageMeta, PricingPackageView, Review, TeacherPublicDetail } from '@/shared/api/public';
import { getTeacherPackages, getTeacherReviews } from '@/shared/api/public';
import { TeacherPackagesTab } from '@/features/marketplace/components/TeacherPackagesTab';
import TrialRequestForm from '@/features/marketplace/components/TrialRequestForm';
import { MessageTeacherButton } from '@/features/chat/components/MessageTeacherButton';
import { StarFilled } from '@ant-design/icons';

const days: Record<string, string> = {
  MONDAY: 'Thứ hai',
  TUESDAY: 'Thứ ba',
  WEDNESDAY: 'Thứ tư',
  THURSDAY: 'Thứ năm',
  FRIDAY: 'Thứ sáu',
  SATURDAY: 'Thứ bảy',
  SUNDAY: 'Chủ nhật',
};

type TeacherDetailClientProps = {
  teacher: TeacherPublicDetail;
  packages: { data: PricingPackageView[]; meta: Partial<PageMeta> };
  availability: AvailabilityView[];
  reviews: { data: Review[]; meta: Partial<PageMeta> };
  subjectOptions?: { id: string; name: string }[];
  errors?: string[];
};

export default function TeacherDetailClient({
  teacher,
  packages,
  availability,
  reviews,
  subjectOptions = [],
  errors = [],
}: TeacherDetailClientProps) {
  const router = useRouter();
  const pathname = usePathname();
  const search = useSearchParams();
  const packagePage = Math.max(0, Number(search.get('packagesPage') || 0));
  const reviewPage = Math.max(0, Number(search.get('reviewsPage') || 0));

  const packageInitial = {
    success: true,
    message: null,
    data: packages.data,
    errors: null,
    meta: {
      page: 0,
      size: packages.meta.size || 6,
      totalElements: packages.meta.totalElements || packages.data.length,
      totalPages: packages.meta.totalPages || 1,
      hasNext: false,
      hasPrevious: false,
    },
  };
  const reviewInitial = {
    success: true,
    message: null,
    data: reviews.data,
    errors: null,
    meta: {
      page: 0,
      size: reviews.meta.size || 10,
      totalElements: reviews.meta.totalElements || reviews.data.length,
      totalPages: reviews.meta.totalPages || 1,
      hasNext: false,
      hasPrevious: false,
    },
  };

  const packageQuery = useQuery({
    queryKey: ['public-teacher-packages', teacher.id, packagePage],
    queryFn: () => getTeacherPackages(teacher.id, packagePage, 6),
    initialData: packageInitial,
    enabled: packagePage > 0,
    staleTime: 300_000,
  });
  const reviewQuery = useQuery({
    queryKey: ['public-teacher-reviews', teacher.id, reviewPage],
    queryFn: () => getTeacherReviews(teacher.id, reviewPage, 10),
    initialData: reviewInitial,
    enabled: reviewPage > 0,
    staleTime: 60_000,
  });

  const packageData = packageQuery.data || packageInitial;
  const reviewData = reviewQuery.data || reviewInitial;
  const changePage = (key: string, page: number, anchor: string) => {
    const query = new URLSearchParams(search.toString());
    query.set(key, String(page));
    router.push(`${pathname}?${query.toString()}#${anchor}`, { scroll: false });
  };
  const sectionError = (key: string) => errors.includes(key) ? (
    <Alert
      type="error"
      title="Chưa tải được dữ liệu"
      action={<Button onClick={() => router.refresh()}>Thử lại</Button>}
    />
  ) : null;

  return (
    <>
      <nav className="tm-detail-toc" aria-label="Các phần hồ sơ">
        <ul className="tm-detail-toc-list">
          <li><a className="tm-detail-toc-link" href="#about">Giới thiệu</a></li>
          <li><a className="tm-detail-toc-link" href="#packages">Gói học</a></li>
          <li><a className="tm-detail-toc-link" href="#availability">Lịch rảnh</a></li>
          <li><a className="tm-detail-toc-link" href="#reviews">Đánh giá</a></li>
        </ul>
      </nav>

      <div className="tm-detail-layout">
        <aside id="trial" className="tm-detail-cta tm-section-anchor" aria-labelledby="trial-title">
          <div className="tm-detail-cta-actions">
            <h2 id="trial-title">Bắt đầu với gia sư</h2>
            <a className="tm-button tm-detail-package-link" href="#packages">Xem các gói học</a>
            <MessageTeacherButton teacherId={teacher.id} block />
          </div>
          <section className="tm-trial-section" aria-labelledby="trial-request-title">
            <h3 id="trial-request-title">Yêu cầu học thử</h3>
            <TrialRequestForm teacherId={teacher.id} subjects={subjectOptions} />
          </section>
        </aside>

        <main className="tm-detail-content" aria-label="Nội dung hồ sơ gia sư">
          <section id="about" className="tm-detail-section tm-section-anchor" aria-labelledby="about-title">
            <header className="tm-detail-section-heading">
              <h2 className="tm-detail-section-title" id="about-title">Giới thiệu</h2>
            </header>
            <p className="tm-detail-prose">{teacher.bio || 'Gia sư chưa cập nhật giới thiệu.'}</p>
            {teacher.introductionVideoUrl && /^https?:\/\//.test(teacher.introductionVideoUrl) && (
              <a className="tm-detail-text-link" href={teacher.introductionVideoUrl} target="_blank" rel="noopener noreferrer">
                Xem video giới thiệu
              </a>
            )}
          </section>

          <section id="packages" className="tm-detail-section tm-section-anchor" aria-labelledby="packages-title">
            <header className="tm-detail-section-heading">
              <div>
                <h2 className="tm-detail-section-title" id="packages-title">Gói học</h2>
                <p>Thông tin và điều khoản của các gói đang mở bán.</p>
              </div>
            </header>
            {sectionError('packages') || (
              <>
                <TeacherPackagesTab packages={packageData.data} />
                {packageQuery.isFetching && packagePage > 0 && (
                  <div className="tm-detail-loading" role="status" aria-label="Đang tải gói học">
                    <Spin size="small" />
                  </div>
                )}
                {(packageData.meta.totalPages || 0) > 1 && (
                  <nav className="tm-detail-pagination" aria-label="Trang gói học">
                    <Pagination
                      current={(packageData.meta.page || 0) + 1}
                      pageSize={packageData.meta.size}
                      total={packageData.meta.totalElements}
                      showSizeChanger={false}
                      onChange={page => changePage('packagesPage', page, 'packages')}
                    />
                  </nav>
                )}
              </>
            )}
          </section>

          <section id="availability" className="tm-detail-section tm-section-anchor" aria-labelledby="availability-title">
            <header className="tm-detail-section-heading">
              <div>
                <h2 className="tm-detail-section-title" id="availability-title">Lịch rảnh</h2>
                <p>Khung giờ gia sư có thể nhận dạy; buổi học cụ thể cần được thống nhất.</p>
              </div>
            </header>
            {sectionError('availability') || (
              availability.length > 0 ? (
                <ul className="tm-availability-list">
                  {availability.map((slot, index) => (
                    <li key={slot.id || index}>
                      <strong>{days[slot.dayOfWeek] || slot.dayOfWeek}</strong>
                      <time>{slot.startTime.slice(0, 5)} – {slot.endTime.slice(0, 5)}</time>
                    </li>
                  ))}
                </ul>
              ) : <p className="tm-detail-empty">Gia sư chưa cập nhật lịch rảnh.</p>
            )}
          </section>

          <section id="reviews" className="tm-detail-section tm-section-anchor" aria-labelledby="reviews-title">
            <header className="tm-detail-section-heading">
              <div>
                <h2 className="tm-detail-section-title" id="reviews-title">Đánh giá từ học viên</h2>
                {reviewData.meta.totalElements !== undefined && reviewData.meta.totalElements > 0 && (
                  <p>{reviewData.meta.totalElements} đánh giá</p>
                )}
              </div>
            </header>
            {sectionError('reviews') || (
              <>
                {reviewData.data.length > 0 ? (
                  <ol className="tm-review-list">
                    {reviewData.data.map(review => (
                      <li className="tm-review-entry" key={review.id}>
                        <div className="tm-review-heading">
                          <strong>{review.reviewerName}</strong>
                          <time dateTime={review.createdAt}>{new Date(review.createdAt).toLocaleDateString('vi-VN')}</time>
                        </div>
                        <p className="tm-review-rating" aria-label={`Đánh giá ${review.rating} trên 5`}>
                          <StarFilled aria-hidden="true" /> {review.rating} / 5
                        </p>
                        {review.comment && <p className="tm-review-comment">{review.comment}</p>}
                      </li>
                    ))}
                  </ol>
                ) : <p className="tm-detail-empty">Chưa có đánh giá.</p>}
                {(reviewData.meta.totalPages || 0) > 1 && (
                  <nav className="tm-detail-pagination" aria-label="Trang đánh giá">
                    <Pagination
                      current={(reviewData.meta.page || 0) + 1}
                      pageSize={reviewData.meta.size}
                      total={reviewData.meta.totalElements}
                      showSizeChanger={false}
                      onChange={page => changePage('reviewsPage', page, 'reviews')}
                    />
                  </nav>
                )}
              </>
            )}
          </section>
        </main>
      </div>
    </>
  );
}
