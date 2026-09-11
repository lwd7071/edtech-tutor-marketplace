'use client';

import Link from 'next/link';
import { useQueries } from '@tanstack/react-query';
import { useAuthStore } from '@/features/auth';
import { bookingApi } from '@/features/bookings/api/bookingApi';
import { studentPackageApi } from '@/features/student-packages/api/studentPackageApi';
import { learningApi } from '@/features/learning/api/learningApi';
import { notificationApi } from '@/features/notifications/api/notificationApi';
import { financeApi } from '@/features/finance/api/financeApi';

export default function StudentDashboardPage() {
  const { user } = useAuthStore();
  const results = useQueries({ queries: [
    { queryKey:['dashboard','booking'], queryFn:() => bookingApi.getStudentBookings({ status:'SCHEDULED', from:new Date().toISOString(), page:0, size:1, sort:'startTime,asc' }) },
    { queryKey:['dashboard','packages'], queryFn:() => studentPackageApi.getStudentPackages('ACTIVE',0,100) },
    { queryKey:['dashboard','assignments'], queryFn:() => learningApi.getStudentAssignments(0,1,'TODO') },
    { queryKey:['dashboard','notifications'], queryFn:() => notificationApi.getNotifications(false,0,1) },
    { queryKey:['dashboard','trials'], queryFn:() => bookingApi.getStudentTrialRequests('PENDING',0,1) },
    { queryKey:['dashboard','refunds'], queryFn:() => financeApi.getStudentRefunds(0,100) },
    { queryKey:['dashboard','extensions'], queryFn:() => financeApi.getStudentExtensions(0,100) },
  ] });
  const [bookingQuery, packagesQuery, assignmentsQuery, notificationsQuery, trialsQuery, refundsQuery, extensionsQuery] = results;
  const booking = bookingQuery.data;
  const packages = packagesQuery.data;
  const assignments = assignmentsQuery.data;
  const notifications = notificationsQuery.data;
  const trials = trialsQuery.data;
  const refunds = refundsQuery.data;
  const extensions = extensionsQuery.data;
  const remaining = packages?.data?.reduce((sum, item) => sum + item.remainingSessions, 0) ?? 0;
  const pendingFinance = (refunds?.data ?? []).filter(item => item.status === 'PENDING').length + (extensions?.data ?? []).filter(item => item.status === 'PENDING').length;
  const cards = [
    ['/student/bookings','Buổi sắp tới',booking?.data?.[0] ? new Date(booking.data[0].startTime).toLocaleString('vi-VN') : 'Chưa có lịch sắp tới'],
    ['/student/packages','Lượt học còn lại',`${remaining} buổi`],
    ['/student/assignments','Bài cần làm',`${assignments?.meta?.totalElements ?? 0} bài`],
    ['/student/notifications','Thông báo chưa đọc',`${notifications?.meta?.totalElements ?? 0} thông báo`],
    ['/student/requests','Yêu cầu đang chờ',`${(trials?.meta?.totalElements ?? 0) + pendingFinance} yêu cầu`],
    ['/student/session-reports','Báo cáo buổi học','Xem nhận xét mới nhất'],
    ['/student/messages','Tin nhắn','Trao đổi với gia sư'],
  ];
  return <>
    <header className="tm-page-heading"><p className="tm-eyebrow">Không gian học tập</p><h1>Chào {user?.fullName || 'bạn'}!</h1><p>Các con số dưới đây được cập nhật từ hoạt động học của bạn.</p></header>
    <section className="tm-cta"><div><h2>Bước tiến tiếp theo bắt đầu từ bạn</h2><p>Khám phá gia sư theo môn học và nhịp học phù hợp.</p></div><Link href="/teachers" className="tm-button tm-button-secondary">Tìm gia sư →</Link></section>
    <div className="tm-dashboard-grid">{cards.map(([href,title,body]) => <Link className="tm-panel tm-dashboard-link" href={href} key={href}><h2>{title} →</h2><p>{body}</p></Link>)}</div>
  </>;
}
