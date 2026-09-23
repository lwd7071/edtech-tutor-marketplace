'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Skeleton } from 'antd';
import { teacherApi } from '@/shared/api/teacher';
import { bookingApi } from '@/features/bookings/api/bookingApi';
import { bookingKeys } from '@/features/bookings/data/bookingKeys';
import { getUpcomingBookingFromList } from '@/features/bookings/hooks/useUpcomingBooking';
import { learningApi } from '@/features/learning/api/learningApi';
import { learningKeys } from '@/features/learning/data/learningKeys';
import { financeApi } from '@/features/finance/api/financeApi';
import { financeKeys } from '@/features/finance/data/financeKeys';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import { teacherDashboardKeys } from '../data/teacherDashboardKeys';

const profileLabels = {
  DRAFT: 'Hồ sơ chưa gửi xét duyệt', PENDING_APPROVAL: 'Hồ sơ đang chờ xét duyệt',
  APPROVED: 'Hồ sơ đã được duyệt', REJECTED: 'Hồ sơ cần chỉnh sửa',
};

export function TeacherOverview() {
  const profile = useQuery({ queryKey: teacherDashboardKeys.profile(), queryFn: teacherApi.getProfile });
  const bookings = useQuery({
    queryKey: bookingKeys.list('teacher', { status: 'SCHEDULED', page: 0, size: 12, sort: 'startTime,asc' }),
    queryFn: () => bookingApi.getBookings('teacher', { status: 'SCHEDULED', page: 0, size: 12, sort: 'startTime,asc' }),
  });
  const trials = useQuery({
    queryKey: bookingKeys.trialRequests('PENDING', 0, 3),
    queryFn: () => bookingApi.getTeacherTrialRequests('PENDING', 0, 3),
  });
  const assignments = useQuery({
    queryKey: learningKeys.assignments('teacher', 0),
    queryFn: () => learningApi.getTeacherAssignments(0, 8),
  });
  const wallet = useQuery({ queryKey: financeKeys.wallet(), queryFn: financeApi.getWallet });
  const nextBooking = getUpcomingBookingFromList(bookings.data?.data ?? []).upcomingBooking;
  const submittedAssignments = assignments.data?.data.filter(item => item.submittedCount > 0) ?? [];

  return <div className="tm-stack teacher-overview">
    <header className="tm-page-heading"><p className="tm-eyebrow">Không gian gia sư</p><h1>Việc cần theo dõi</h1><p>Lịch dạy, yêu cầu học thử và hồ sơ của bạn.</p></header>
    <section className="teacher-overview-section">
      <h2>Hồ sơ</h2>
      {profile.isLoading ? <Skeleton active paragraph={{ rows: 1 }} /> : profile.isError ?
        <Alert type="error" title="Chưa tải được hồ sơ" action={<Button onClick={() => profile.refetch()}>Thử lại</Button>} /> : <>
          <p>{profileLabels[profile.data?.approvalStatus ?? 'DRAFT']}</p>
          {profile.data?.approvalStatus === 'REJECTED' && profile.data.rejectionReason && <p>Lý do: {profile.data.rejectionReason}</p>}
          <Link href="/teacher/profile">{profile.data?.approvalStatus === 'APPROVED' ? 'Xem hồ sơ' : 'Tiếp tục hồ sơ'} →</Link>
        </>}
    </section>
    <div className="teacher-overview-columns">
      <section className="teacher-overview-section"><h2>Buổi học tiếp theo</h2>
        {bookings.isLoading ? <Skeleton active paragraph={{ rows: 2 }} /> : bookings.isError ?
          <Alert type="error" title="Chưa tải được lịch dạy" action={<Button onClick={() => bookings.refetch()}>Thử lại</Button>} /> : nextBooking ? <>
            <strong>{nextBooking.subject.name} · {nextBooking.student.fullName}</strong>
            <p>{formatVietnamDateTime(nextBooking.startTime)} · {nextBooking.deliveryMode === 'ONLINE' ? 'Online' : 'Trực tiếp'}</p>
            <Link href={`/teacher/bookings/${nextBooking.id}`}>Xem buổi học →</Link>
          </> : <p>Chưa có buổi học sắp tới. <Link href="/teacher/availability">Xem lịch rảnh →</Link></p>}
      </section>
      <section className="teacher-overview-section"><h2>Yêu cầu học thử</h2>
        {trials.isLoading ? <Skeleton active paragraph={{ rows: 2 }} /> : trials.isError ?
          <Alert type="error" title="Chưa tải được yêu cầu" action={<Button onClick={() => trials.refetch()}>Thử lại</Button>} /> : trials.data?.data.length ? <>
            <ul>{trials.data.data.map(item => <li key={item.id}>Thời gian đề xuất: {formatVietnamDateTime(item.preferredStartTime)}</li>)}</ul>
            <Link href="/teacher/trial-requests">Xem và phản hồi yêu cầu →</Link>
          </> : <p>Không có yêu cầu học thử đang chờ.</p>}
      </section>
    </div>
    <div className="teacher-overview-columns">
      <section className="teacher-overview-section"><h2>Bài tập có bài nộp</h2>
        {assignments.isLoading ? <Skeleton active paragraph={{ rows: 2 }} /> : assignments.isError ?
          <Alert type="error" title="Chưa tải được bài tập" action={<Button onClick={() => assignments.refetch()}>Thử lại</Button>} /> : submittedAssignments.length ? <>
            <ul>{submittedAssignments.slice(0, 3).map(item => <li key={item.id}><Link href={`/teacher/assignments/${item.id}`}>{item.title} · {item.submittedCount} bài nộp</Link></li>)}</ul>
            <Link href="/teacher/assignments">Xem tất cả bài tập →</Link>
          </> : <p>Chưa có bài nộp trong các bài tập gần đây.</p>}
      </section>
      <section className="teacher-overview-section"><h2>Thu nhập</h2>
        {wallet.isLoading ? <Skeleton active paragraph={{ rows: 2 }} /> : wallet.isError ?
          <Alert type="error" title="Chưa tải được ví" action={<Button onClick={() => wallet.refetch()}>Thử lại</Button>} /> : <>
            <p>Số dư khả dụng: <strong>{(wallet.data?.data.availableBalanceVnd ?? 0).toLocaleString('vi-VN')} ₫</strong></p>
            <p>Đang giữ quyết toán: {(wallet.data?.data.heldBalanceVnd ?? 0).toLocaleString('vi-VN')} ₫</p>
            <Link href="/teacher/wallet">Xem ví thu nhập →</Link>
          </>}
      </section>
    </div>
    <style>{`
      .teacher-overview-section{padding:24px 0;border-top:1px solid var(--color-border)}
      .teacher-overview-section h2{font-size:20px;margin:0 0 12px}
      .teacher-overview-section p{margin:8px 0;color:var(--color-text-secondary)}
      .teacher-overview-section ul{margin:8px 0 16px;padding-left:20px}
      .teacher-overview-section li{margin:8px 0}
      .teacher-overview-section a{color:var(--color-primary-700);font-weight:600}
      .teacher-overview-columns{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:32px}
      @media(max-width:767px){.teacher-overview-columns{grid-template-columns:1fr;gap:0}}
    `}</style>
  </div>;
}
