'use client';

import Link from 'next/link';
import Image from 'next/image';
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
    <header className="tm-page-heading teacher-overview-hero">
      <div><h1>Hôm nay bạn cần làm gì?</h1><p>Hoàn thành việc cần làm để kết nối với nhiều học viên hơn.</p></div>
      <Image src="/images/tutor-teacher-illustration.png" alt="" width={1456} height={1024} className="teacher-overview-hero-art" />
    </header>
    <section className="teacher-overview-section teacher-overview-readiness">
      <h2>Hồ sơ giảng dạy</h2>
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
    <section className="teacher-overview-section teacher-overview-schedule"><div className="teacher-overview-section-head"><h2>Lịch dạy của bạn</h2><Link href="/teacher/bookings">Xem lịch đầy đủ →</Link></div>
      {bookings.isLoading ? <Skeleton active paragraph={{ rows: 2 }} /> : bookings.isError ?
        <Alert type="error" title="Chưa tải được lịch dạy" action={<Button onClick={() => bookings.refetch()}>Thử lại</Button>} /> : bookings.data?.data.length ?
        <ul className="teacher-overview-schedule-list">{bookings.data.data.slice(0, 5).map(item => <li key={item.id}><span>{formatVietnamDateTime(item.startTime)}</span><strong>{item.subject.name}</strong><span>{item.student.fullName}</span><Link href={`/teacher/bookings/${item.id}`}>Xem buổi học</Link></li>)}</ul> :
        <p>Chưa có buổi học sắp tới. <Link href="/teacher/availability">Tạo khung giờ rảnh →</Link></p>}
    </section>
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
      .teacher-overview-hero{position:relative;display:flex;align-items:center;min-height:166px;overflow:hidden;padding:24px 34px;background:linear-gradient(110deg,#fffefa 0%,#fffefa 63%,#ffdc50 63%);border-radius:12px}
      .teacher-overview-hero>div{position:relative;z-index:1;max-width:66%}.teacher-overview-hero h1{font-size:clamp(30px,3.2vw,44px)!important;line-height:1.16;margin:0 0 10px}.teacher-overview-hero p{margin:0;color:var(--color-text-secondary)}
      .teacher-overview-hero-art{position:absolute;right:0;bottom:-35px;width:38%;height:210px;object-fit:contain;object-position:bottom}
      .teacher-overview-section{min-width:0;padding:22px;border:1px solid var(--color-border);border-radius:12px;background:var(--color-surface);box-shadow:none}
      .teacher-overview-readiness{background:#fff1b2;border-color:#e6c55d;padding:26px 28px}
      .teacher-overview-section h2{font-size:20px;margin:0 0 12px}
      .teacher-overview-section p{margin:8px 0;color:var(--color-text-secondary)}
      .teacher-overview-section ul{margin:8px 0 16px;padding-left:20px}
      .teacher-overview-section li{margin:8px 0}
      .teacher-overview-section-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.teacher-overview-schedule-list{display:grid;gap:0!important;padding:0!important;list-style:none}.teacher-overview-schedule-list li{display:grid;grid-template-columns:minmax(170px,1.1fr) minmax(90px,.7fr) minmax(100px,1fr) auto;gap:12px;align-items:center;padding:10px 0;border-top:1px solid var(--color-border);margin:0}.teacher-overview-schedule-list li span{color:var(--color-text-secondary)}
      .teacher-overview-section a{color:var(--color-text-primary);font-weight:700;text-decoration:underline;text-decoration-color:#d8ad00;text-underline-offset:4px}
      .teacher-overview-columns{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}
      @media(max-width:767px){.teacher-overview{padding-bottom:calc(100px + env(safe-area-inset-bottom, 0px))}.teacher-overview-columns{grid-template-columns:1fr;gap:12px}.teacher-overview-section{padding:18px}.teacher-overview-hero{min-height:150px;padding:20px}.teacher-overview-hero>div{max-width:78%}.teacher-overview-hero h1{font-size:27px!important}.teacher-overview-hero-art{width:40%;height:140px;bottom:-20px;right:-30px}.teacher-overview-schedule-list li{grid-template-columns:1fr auto;gap:3px 10px}.teacher-overview-schedule-list li span:nth-of-type(2){grid-column:1}}
    `}</style>
  </div>;
}
