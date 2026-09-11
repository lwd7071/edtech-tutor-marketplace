'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Empty, Pagination, Skeleton } from 'antd';
import { bookingApi } from '@/features/bookings/api/bookingApi';

export default function StudentSessionReportsPage() {
  const [page, setPage] = useState(0);
  const query = useQuery({ queryKey: ['student-session-reports', page], queryFn: () => bookingApi.getStudentSessionReports(page, 10) });
  if (query.isLoading) return <Skeleton active />;
  if (query.isError) return <Alert type="error" title="Chưa tải được báo cáo buổi học" action={<Button onClick={() => query.refetch()}>Thử lại</Button>} />;
  const reports = query.data?.data ?? [];
  return <div className="tm-stack">
    <header className="tm-page-heading"><p className="tm-eyebrow">Theo dõi tiến độ</p><h1>Báo cáo buổi học</h1><p>Nhận xét và việc cần làm tiếp do gia sư ghi sau mỗi buổi.</p></header>
    {reports.length === 0 ? <Empty description="Chưa có báo cáo buổi học." /> : reports.map(report => <article className="tm-panel" key={report.id}>
      <p className="tm-eyebrow">{new Date(report.startTime).toLocaleString('vi-VN')}</p>
      <h2>{report.content}</h2><p>{report.feedback}</p>
      {report.followUpNote && <p><strong>Việc cần làm tiếp:</strong> {report.followUpNote}</p>}
      {report.recordLink && /^https?:\/\//.test(report.recordLink) && <a href={report.recordLink} target="_blank" rel="noopener noreferrer">Xem bản ghi ↗</a>}
      <p><Link href={`/student/bookings/${report.bookingId}`}>Mở chi tiết buổi học →</Link></p>
    </article>)}
    {(query.data?.meta?.totalPages ?? 0) > 1 && <Pagination current={page + 1} total={query.data!.meta!.totalElements} pageSize={query.data!.meta!.size} onChange={next => setPage(next - 1)} showSizeChanger={false} />}
  </div>;
}
