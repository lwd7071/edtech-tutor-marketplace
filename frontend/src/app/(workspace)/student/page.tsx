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
  const nextBookingStartTime = summary?.nextBookingStartTime;
  const hasNextBooking = Boolean(nextBookingStartTime);
  const metrics = summary ? [
    { href: '/student/packages', label: 'Lượt học còn lại', value: `${summary.remainingSessions} buổi` },
    { href: '/student/assignments', label: 'Bài cần làm', value: `${summary.todoAssignments} bài` },
    { href: '/student/notifications', label: 'Thông báo chưa đọc', value: `${summary.unreadNotifications} thông báo` },
    { href: '/student/requests', label: 'Yêu cầu đang chờ', value: `${summary.pendingRequests} yêu cầu` },
  ] : [];

  return (
    <div className="tm-student-dashboard">
      <header className="tm-student-dashboard-heading">
        <h1>Chào {user?.fullName || 'bạn'}!</h1>
        <p>Buổi học và việc cần làm tiếp theo của bạn ở đây.</p>
      </header>

      {dashboard.isError && (
        <Alert
          className="tm-student-dashboard-error"
          type="error"
          showIcon
          message="Chưa tải được tổng quan"
          action={<Button size="small" onClick={() => dashboard.refetch()}>Thử lại</Button>}
        />
      )}

      {loading && (
        <section className="tm-student-dashboard-loading" aria-busy="true" aria-live="polite">
          <p role="status">Đang tải tổng quan việc học…</p>
          <Skeleton className="tm-student-dashboard-skeleton" active paragraph={{ rows: 3 }} />
        </section>
      )}

      {!loading && !dashboard.isError && summary && (
        <>
          <section className="tm-student-next" aria-labelledby="student-next-title">
            <div className="tm-student-next-copy">
              <h2 id="student-next-title">{hasNextBooking ? 'Buổi học tiếp theo' : 'Bắt đầu với gia sư phù hợp'}</h2>
              <p>{nextBookingStartTime
                ? formatVietnamDateTime(nextBookingStartTime)
                : 'Chọn môn học và tìm người đồng hành theo mục tiêu của bạn.'}</p>
            </div>
            <Link
              className="tm-student-next-action"
              href={hasNextBooking ? '/student/bookings' : '/subjects'}
            >
              {hasNextBooking ? 'Xem lịch học' : 'Tìm gia sư'}
            </Link>
          </section>

          <section className="tm-student-metrics" aria-labelledby="student-metrics-title">
            <h2 id="student-metrics-title">Việc học của bạn</h2>
            <ul>
              {metrics.map((metric) => (
                <li key={metric.href}>
                  <Link href={metric.href}>
                    <span>{metric.label}</span>
                    <strong>{metric.value}</strong>
                  </Link>
                </li>
              ))}
            </ul>
          </section>

          <nav className="tm-student-shortcuts" aria-label="Lối tắt học tập">
            <Link href="/student/session-reports">
              <span>Báo cáo buổi học</span>
              <span className="tm-student-shortcut-detail">Xem nhận xét mới nhất</span>
            </Link>
            <Link href="/student/messages">
              <span>Tin nhắn</span>
              <span className="tm-student-shortcut-detail">Trao đổi với gia sư</span>
            </Link>
          </nav>
        </>
      )}
    </div>
  );
}
