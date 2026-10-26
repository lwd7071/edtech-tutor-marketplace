'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Pagination, Skeleton } from 'antd';
import { bookingApi } from '@/features/bookings/api/bookingApi';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';

const PAGE_SIZE = 10;

export default function StudentSessionReportsPage() {
  const [page, setPage] = useState(0);
  const query = useQuery({ queryKey: ['student-session-reports', page], queryFn: () => bookingApi.getStudentSessionReports(page, PAGE_SIZE) });

  return (
    <main className="tm-stack tm-session-reports" aria-labelledby="student-session-reports-title">
      <header className="tm-page-heading tm-session-reports__heading">
        <h1 id="student-session-reports-title" className="tm-session-reports__title">Báo cáo buổi học</h1>
        <p>Nhận xét và việc cần làm tiếp do gia sư ghi sau mỗi buổi.</p>
      </header>

      {query.isLoading && <div className="tm-session-reports__loading" role="status" aria-label="Đang tải báo cáo buổi học"><Skeleton active paragraph={{ rows: 4 }} /></div>}
      {query.isError && <Alert className="tm-session-reports__error" type="error" showIcon message="Chưa tải được báo cáo buổi học" action={<Button onClick={() => void query.refetch()}>Thử lại</Button>} />}
      {!query.isLoading && !query.isError && (query.data?.data ?? []).length === 0 && (
        <div className="tm-session-reports__empty">
          <h2>Chưa có báo cáo buổi học</h2>
          <p>Nhận xét của gia sư sẽ xuất hiện sau khi buổi học được ghi nhận.</p>
        </div>
      )}

      {!query.isLoading && !query.isError && (query.data?.data ?? []).length > 0 && (
        <>
          <ol className="tm-session-reports__list" aria-label="Danh sách báo cáo buổi học">
            {(query.data?.data ?? []).map(report => (
              <li key={report.id}>
              <article className="tm-session-reports__item">
                <header className="tm-session-reports__item-heading">
                  <h2><time dateTime={report.startTime}>{formatVietnamDateTime(report.startTime)}</time></h2>
                </header>
                <div className="tm-session-reports__content">
                  <section>
                    <h3>Nội dung buổi học</h3>
                    <p className="tm-session-reports__copy">{report.content}</p>
                  </section>
                  <section>
                    <h3>Nhận xét của gia sư</h3>
                    <p className="tm-session-reports__copy">{report.feedback}</p>
                  </section>
                  {report.followUpNote && <section className="tm-session-reports__follow-up">
                    <h3>Việc cần làm tiếp</h3>
                    <p className="tm-session-reports__copy">{report.followUpNote}</p>
                  </section>}
                </div>
                <footer className="tm-session-reports__actions">
                  {report.recordLink && /^https?:\/\//.test(report.recordLink) && <a href={report.recordLink} target="_blank" rel="noopener noreferrer">Xem bản ghi</a>}
                  <Link href={`/student/bookings/${report.bookingId}`}>Mở chi tiết buổi học</Link>
                </footer>
              </article>
              </li>
            ))}
          </ol>
          {(query.data?.meta?.totalPages ?? 0) > 1 && <Pagination className="tm-session-reports__pagination" current={page + 1} total={query.data?.meta?.totalElements} pageSize={query.data?.meta?.size || PAGE_SIZE} onChange={next => setPage(next - 1)} showSizeChanger={false} />}
        </>
      )}
    </main>
  );
}
