'use client';

import Link from 'next/link';
import { useAuthStore } from '@/features/auth';
import { Alert, Button, Skeleton } from 'antd';
import { useStudentDashboard } from '@/features/student-dashboard/hooks/useStudentDashboard';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';

export default function StudentDashboardPage() {
  const { user } = useAuthStore();
  const dashboard = useStudentDashboard();
  const summary = dashboard.data?.data;
  const loading = dashboard.isLoading;
  const value = (text: string) => loading ? 'Đang tải…' : text;
  const cards = [
    ['/student/bookings','Buổi sắp tới',summary?.nextBookingStartTime ? formatVietnamDateTime(summary.nextBookingStartTime) : 'Chưa có lịch sắp tới'],
    ['/student/packages','Lượt học còn lại',`${summary?.remainingSessions ?? 0} buổi`],
    ['/student/assignments','Bài cần làm',`${summary?.todoAssignments ?? 0} bài`],
    ['/student/notifications','Thông báo chưa đọc',`${summary?.unreadNotifications ?? 0} thông báo`],
    ['/student/requests','Yêu cầu đang chờ',`${summary?.pendingRequests ?? 0} yêu cầu`],
    ['/student/session-reports','Báo cáo buổi học','Xem nhận xét mới nhất'],
    ['/student/messages','Tin nhắn','Trao đổi với gia sư'],
  ];
  return <>
    {dashboard.isError && <Alert type="error" showIcon message="Chưa tải được tổng quan" action={<Button size="small" onClick={() => dashboard.refetch()}>Thử lại</Button>} style={{ marginBottom: 16 }} />}
    <header className="tm-page-heading"><h1>Chào {user?.fullName || 'bạn'}!</h1><p>Buổi học và việc cần làm tiếp theo của bạn ở đây.</p></header>
    <section className="tm-cta"><div><h2>{summary?.nextBookingStartTime ? 'Buổi học tiếp theo' : 'Bắt đầu với gia sư phù hợp'}</h2><p>{summary?.nextBookingStartTime ? formatVietnamDateTime(summary.nextBookingStartTime) : 'Chọn môn học và tìm người đồng hành theo mục tiêu của bạn.'}</p></div><Link href={summary?.nextBookingStartTime ? '/student/bookings' : '/subjects'} className="tm-button tm-button-secondary">{summary?.nextBookingStartTime ? 'Xem lịch học' : 'Tìm gia sư'} →</Link></section>
    {loading ? <Skeleton active paragraph={{ rows: 5 }} /> : !dashboard.isError && <div className="tm-dashboard-grid">{cards.map(([href,title,body]) => <Link className="tm-panel tm-dashboard-link" href={href} key={href}><h2>{title} →</h2><p>{value(body)}</p></Link>)}</div>}
  </>;
}
