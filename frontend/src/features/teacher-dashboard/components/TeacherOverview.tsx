'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
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

  return <main className="tm-stack tm-teacher-overview">
    <header className="tm-page-heading tm-teacher-overview__heading">
      <div>
        <h1>Hôm nay bạn cần làm gì?</h1>
        <p>Hoàn thành việc cần làm để kết nối với nhiều học viên hơn.</p>
      </div>
    </header>

    <section className="tm-teacher-overview__profile" aria-labelledby="teacher-overview-profile-title">
      <div className="tm-teacher-overview__section-title">
        <h2 id="teacher-overview-profile-title">Hồ sơ giảng dạy</h2>
      </div>
      {profile.isLoading ? <div className="tm-teacher-overview__loading" role="status">Đang tải trạng thái hồ sơ…</div> : profile.isError ?
        <div className="tm-teacher-overview__error" role="alert"><p>Chưa tải được hồ sơ</p><button type="button" onClick={() => profile.refetch()}>Thử lại</button></div> : <div className="tm-teacher-overview__profile-status">
          <div>
            <p className="tm-teacher-overview__status-label">{profileLabels[profile.data?.approvalStatus ?? 'DRAFT']}</p>
            {profile.data?.approvalStatus === 'REJECTED' && profile.data.rejectionReason && <p className="tm-teacher-overview__rejection">Lý do: {profile.data.rejectionReason}</p>}
          </div>
          <Link className="tm-teacher-overview__primary-link" href="/teacher/profile">
            {profile.data?.approvalStatus === 'APPROVED' ? 'Xem hồ sơ' : 'Tiếp tục hồ sơ'}
          </Link>
        </div>}
    </section>

    <div className="tm-teacher-overview__priority-grid">
      <section className="tm-teacher-overview__section" aria-labelledby="teacher-overview-next-title">
        <h2 id="teacher-overview-next-title">Buổi học tiếp theo</h2>
        {bookings.isLoading ? <div className="tm-teacher-overview__loading" role="status">Đang tải lịch dạy…</div> : bookings.isError ?
          <div className="tm-teacher-overview__error" role="alert"><p>Chưa tải được lịch dạy</p><button type="button" onClick={() => bookings.refetch()}>Thử lại</button></div> : nextBooking ? <div className="tm-teacher-overview__next-booking">
            <p className="tm-teacher-overview__lead">{nextBooking.subject.name}<span aria-hidden="true"> · </span>{nextBooking.student.fullName}</p>
            <p className="tm-teacher-overview__meta"><time dateTime={nextBooking.startTime}>{formatVietnamDateTime(nextBooking.startTime)}</time><span aria-hidden="true"> · </span>{nextBooking.deliveryMode === 'ONLINE' ? 'Online' : 'Trực tiếp'}</p>
            <Link href={`/teacher/bookings/${nextBooking.id}`}>Mở buổi học</Link>
          </div> : <div className="tm-teacher-overview__empty"><p>Chưa có buổi học sắp tới.</p><Link href="/teacher/availability">Xem lịch rảnh</Link></div>}
      </section>

      <section className="tm-teacher-overview__section" aria-labelledby="teacher-overview-trials-title">
        <div className="tm-teacher-overview__section-title">
          <h2 id="teacher-overview-trials-title">Yêu cầu học thử</h2>
          {!trials.isLoading && !trials.isError && trials.data?.data.length ? <Link href="/teacher/trial-requests">Xem và phản hồi yêu cầu</Link> : null}
        </div>
        {trials.isLoading ? <div className="tm-teacher-overview__loading" role="status">Đang tải yêu cầu học thử…</div> : trials.isError ?
          <div className="tm-teacher-overview__error" role="alert"><p>Chưa tải được yêu cầu</p><button type="button" onClick={() => trials.refetch()}>Thử lại</button></div> : trials.data?.data.length ? <ul className="tm-teacher-overview__trial-list">
            {trials.data.data.map(item => <li key={item.id}><span>Thời gian đề xuất</span><time dateTime={item.preferredStartTime}>{formatVietnamDateTime(item.preferredStartTime)}</time></li>)}
          </ul> : <div className="tm-teacher-overview__empty"><p>Không có yêu cầu học thử đang chờ.</p><Link href="/teacher/trial-requests">Xem yêu cầu học thử</Link></div>}
      </section>
    </div>

    <section className="tm-teacher-overview__schedule" aria-labelledby="teacher-overview-schedule-title">
      <div className="tm-teacher-overview__section-title">
        <h2 id="teacher-overview-schedule-title">Lịch dạy của bạn</h2>
        <Link href="/teacher/bookings">Xem lịch đầy đủ</Link>
      </div>
      {bookings.isLoading ? <div className="tm-teacher-overview__loading" role="status">Đang tải lịch dạy…</div> : bookings.isError ?
        <div className="tm-teacher-overview__error" role="alert"><p>Chưa tải được lịch dạy</p><button type="button" onClick={() => bookings.refetch()}>Thử lại</button></div> : bookings.data?.data.length ?
        <ol className="tm-teacher-overview__schedule-list">{bookings.data.data.slice(0, 5).map(item => <li key={item.id}>
          <time dateTime={item.startTime}>{formatVietnamDateTime(item.startTime)}</time>
          <strong>{item.subject.name}</strong>
          <span>{item.student.fullName}</span>
          <Link href={`/teacher/bookings/${item.id}`}>Mở buổi học</Link>
        </li>)}</ol> : <div className="tm-teacher-overview__empty"><p>Chưa có buổi học sắp tới.</p><Link href="/teacher/availability">Tạo khung giờ rảnh</Link></div>}
    </section>

    <div className="tm-teacher-overview__support-grid">
      <section className="tm-teacher-overview__section" aria-labelledby="teacher-overview-submissions-title">
        <div className="tm-teacher-overview__section-title">
          <h2 id="teacher-overview-submissions-title">Bài tập có bài nộp</h2>
          <Link href="/teacher/assignments">Tất cả bài tập</Link>
        </div>
        {assignments.isLoading ? <div className="tm-teacher-overview__loading" role="status">Đang tải bài tập…</div> : assignments.isError ?
          <div className="tm-teacher-overview__error" role="alert"><p>Chưa tải được bài tập</p><button type="button" onClick={() => assignments.refetch()}>Thử lại</button></div> : submittedAssignments.length ? <ul className="tm-teacher-overview__assignment-list">
            {submittedAssignments.slice(0, 3).map(item => <li key={item.id}>
              <Link href={`/teacher/assignments/${item.id}`}>{item.title}</Link>
              <span>{item.submittedCount} bài nộp</span>
            </li>)}
          </ul> : <div className="tm-teacher-overview__empty"><p>Chưa có bài nộp trong các bài tập gần đây.</p></div>}
      </section>

      <section className="tm-teacher-overview__section" aria-labelledby="teacher-overview-wallet-title">
        <div className="tm-teacher-overview__section-title">
          <h2 id="teacher-overview-wallet-title">Thu nhập</h2>
          <Link href="/teacher/wallet">Xem ví thu nhập</Link>
        </div>
        {wallet.isLoading ? <div className="tm-teacher-overview__loading" role="status">Đang tải số dư…</div> : wallet.isError ?
          <div className="tm-teacher-overview__error" role="alert"><p>Chưa tải được ví</p><button type="button" onClick={() => wallet.refetch()}>Thử lại</button></div> : <dl className="tm-teacher-overview__balances">
            <div><dt>Số dư khả dụng</dt><dd>{(wallet.data?.data.availableBalanceVnd ?? 0).toLocaleString('vi-VN')} ₫</dd></div>
            <div><dt>Đang giữ quyết toán</dt><dd>{(wallet.data?.data.heldBalanceVnd ?? 0).toLocaleString('vi-VN')} ₫</dd></div>
          </dl>}
      </section>
    </div>
  </main>;
}
