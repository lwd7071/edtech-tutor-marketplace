'use client';

import { useEffect, useState, type ReactNode } from 'react';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Form, Input, Modal, Skeleton, message } from 'antd';
import { bookingApi } from '../api/bookingApi';
import { bookingKeys } from '../data/bookingKeys';
import { SessionReportModal } from '../components/SessionReportModal';
import { CancelBookingModal } from '../components/CancelBookingModal';
import { ReviewBookingModal } from '../components/ReviewBookingModal';
import { BackLink } from '@/shared/components/navigation/NavigationLinks';
import { MessageTeacherButton } from '@/features/chat/components/MessageTeacherButton';
import { useConfirmBooking, useDisputeBooking } from '../hooks/useBookings';
import { formatVietnamDateTime, formatVietnamTime } from '@/shared/lib/vietnamTime';
import type { BookingDetail } from '../types';
import { TeacherBookingDetailView } from './TeacherBookingDetailView';

const bookingLabels = { SCHEDULED: 'Đã lên lịch', COMPLETED: 'Đã hoàn thành', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn' } as const;
const settlementLabels = { AWAITING_CONFIRMATION: 'Chờ xác nhận hai bên', HELD: 'Đang giữ tiền', DISPUTE_PENDING: 'Chờ xử lý khiếu nại', REOPENED: 'Đã mở lại xác nhận', AWAITING_ADMIN_DECISION: 'Chờ quyết định quản trị', RELEASED: 'Đã giải ngân', RETAINED: 'Đã giữ lại cho nền tảng' } as const;

export default function BookingDetailPage({ role }: { role: 'student' | 'teacher' }) {
  const { id } = useParams<{ id: string }>();
  const [action, setAction] = useState<string>();
  const [disputeOpen, setDisputeOpen] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);
  const confirmMutation = useConfirmBooking();
  const disputeMutation = useDisputeBooking();
  const booking = useQuery({ queryKey: bookingKeys.detail(role, id), queryFn: () => bookingApi.getBookingDetail(role, id) });
  const review = useQuery({
    queryKey: ['booking-review', id],
    queryFn: () => bookingApi.getReview(id),
    enabled: role === 'student' && booking.data?.data?.canReview === true,
  });
  if (booking.isLoading) return <div className="tm-booking-detail-loading" role="status" aria-label="Đang tải chi tiết buổi học"><Skeleton active paragraph={{ rows: 6 }} /></div>;
  if (booking.isError || !booking.data?.data) return <Alert type="error" title="Chưa đọc được buổi học" description="Buổi học có thể không tồn tại hoặc không thuộc tài khoản của bạn." action={<Button onClick={() => booking.refetch()}>Thử lại</Button>} />;

  const item = booking.data.data;
  const currentReview = review.data?.data;
  const close = () => { setAction(undefined); void booking.refetch(); void review.refetch(); };
  const canComplete = role === 'teacher' && (
    (item.status === 'SCHEDULED' && now >= new Date(item.endTime).getTime()) ||
    (!item.trial && item.settlementStatus === 'REOPENED' && item.canConfirm && !item.sessionReport)
  );
  const confirm = async () => { try { await confirmMutation.mutateAsync({ id: item.id, data: { version: item.version } }); message.success('Đã xác nhận buổi học.'); void booking.refetch(); } catch { message.error('Không thể xác nhận buổi học. Vui lòng tải lại và thử lại.'); } };

  const teacherActions = <>
    {canComplete && <Button type="primary" onClick={() => setAction('complete')}>{item.settlementStatus === 'REOPENED' ? 'Xác nhận lại và nộp báo cáo' : 'Hoàn thành và viết báo cáo'}</Button>}
    {role === 'teacher' && item.status === 'SCHEDULED' && <Button onClick={() => setAction('cancel')}>Hủy buổi học</Button>}
    {role === 'teacher' && item.canDispute && <Button onClick={() => setDisputeOpen(true)}>Khiếu nại khoản tiền đang giữ</Button>}
  </>;
  const studentActions = <>
    <MessageTeacherButton teacherId={item.teacher.id} />
    {item.canConfirm && <Button type="primary" onClick={confirm} loading={confirmMutation.isPending}>Xác nhận buổi học</Button>}
    {item.canReview && !review.isLoading && !currentReview && <Button onClick={() => setAction('review')}>Đánh giá buổi học</Button>}
  </>;

  return <div className={`tm-stack${role === 'student' ? ' tm-booking-detail' : ''}`}>
    <BackLink href={`/${role}/bookings`}>{role === 'teacher' ? 'Lịch dạy' : 'Lịch học'}</BackLink>
    {role === 'teacher' ? <TeacherBookingDetailView item={item} labels={bookingLabels} settlementLabels={settlementLabels} actions={teacherActions} /> : <StudentBookingDetailView item={item} currentReview={currentReview} actions={studentActions} />}
    {role === 'student' && item.sessionReport && <section className="tm-booking-report" aria-labelledby="session-report-title">
      <h2 id="session-report-title">Báo cáo buổi học</h2>
      <div className="tm-booking-report-grid">
        <div><h3>Nội dung buổi học</h3><p>{item.sessionReport.content}</p></div>
        <div><h3>Nhận xét của gia sư</h3><p>{item.sessionReport.feedback}</p></div>
        {item.sessionReport.followUpNote && <div><h3>Việc cần làm tiếp</h3><p>{item.sessionReport.followUpNote}</p></div>}
      </div>
    </section>}
    <SessionReportModal open={action === 'complete'} booking={item} onClose={close} />
    <CancelBookingModal open={action === 'cancel'} booking={item} onClose={close} />
    <ReviewBookingModal visible={action === 'review'} bookingId={item.id} teacherName={item.teacher.fullName} onCancel={close} onSuccess={close} />
    <Modal title="Khiếu nại khoản tiền đang giữ" open={disputeOpen} footer={null} onCancel={() => setDisputeOpen(false)} destroyOnHidden>
      <Form layout="vertical" onFinish={async values => { try { await disputeMutation.mutateAsync({ id: item.id, data: { version: item.version, reason: values.reason } }); message.success('Đã gửi khiếu nại.'); setDisputeOpen(false); void booking.refetch(); } catch { message.error('Không thể gửi khiếu nại.'); } }}>
        <Form.Item name="reason" label="Lý do" rules={[{ required: true, min: 10, message: 'Vui lòng nêu rõ lý do (ít nhất 10 ký tự).' }]}><Input.TextArea rows={4} maxLength={2000} /></Form.Item>
        <Button type="primary" htmlType="submit" loading={disputeMutation.isPending}>Gửi khiếu nại</Button>
      </Form>
    </Modal>
  </div>;
}

function StudentBookingDetailView({ item, currentReview, actions }: { item: BookingDetail; currentReview: { rating: number; comment?: string | null } | null | undefined; actions: ReactNode }) {
  const meetingAvailable = item.deliveryMode === 'ONLINE' && Boolean(item.meetingLink && /^https?:\/\//.test(item.meetingLink)) && item.status === 'SCHEDULED';
  const settlement = item.settlement;
  return <>
    <header className="tm-booking-detail-heading">
      <div><div className="tm-booking-detail-kind">{item.trial ? 'Buổi học thử' : 'Buổi học'}</div><h1>{item.subject.name}</h1></div>
      <div className="tm-booking-status" role="status" aria-label={`Trạng thái: ${bookingLabels[item.status]}`}>{bookingLabels[item.status]}</div>
    </header>
    <time className="tm-booking-time" dateTime={item.startTime}>{formatVietnamDateTime(item.startTime)} <span aria-hidden="true">–</span> {formatVietnamTime(item.endTime)}</time>
    <section className="tm-booking-facts" aria-labelledby="booking-facts-title">
      <h2 id="booking-facts-title">Thông tin buổi học</h2>
      <dl>
        <div><dt>Gia sư</dt><dd>{item.teacher.fullName}</dd></div>
        <div><dt>Học viên</dt><dd>{item.student.fullName}</dd></div>
        <div><dt>Thời gian</dt><dd>{formatVietnamDateTime(item.startTime)} – {formatVietnamTime(item.endTime)}</dd></div>
        <div><dt>Hình thức</dt><dd>{item.deliveryMode === 'ONLINE' ? 'Trực tuyến' : 'Trực tiếp'}</dd></div>
        {item.locationAddress && <div><dt>Địa điểm</dt><dd>{item.locationAddress}</dd></div>}
        {meetingAvailable && <div><dt>Phòng học</dt><dd><a href={item.meetingLink!} target="_blank" rel="noopener noreferrer">Mở phòng học</a></dd></div>}
        {item.status === 'CANCELLED' && item.cancelReason && <div className="tm-booking-cancel-reason"><dt>Lý do hủy</dt><dd>{item.cancelReason}</dd></div>}
      </dl>
      {item.outsideAvailabilityWarning && <Alert className="tm-booking-warning" type="warning" showIcon title="Buổi học nằm ngoài lịch rảnh đã công bố" />}
    </section>
    {!item.trial && item.settlementStatus && <section className="tm-booking-settlement" aria-labelledby="settlement-title">
      <h2 id="settlement-title">Quyết toán buổi học</h2>
      <div className="tm-booking-settlement-status" role="status">{settlementLabels[item.settlementStatus]}</div>
      <dl>
        {settlement?.netAmountVnd != null && <div><dt>Số tiền gia sư nhận</dt><dd>{new Intl.NumberFormat('vi-VN').format(settlement.netAmountVnd)} ₫</dd></div>}
        <div><dt>Hạn xác nhận</dt><dd>{settlement?.confirmationDeadline ? formatVietnamDateTime(settlement.confirmationDeadline) : '—'}</dd></div>
        <div><dt>Gia sư</dt><dd>{settlement?.teacherConfirmedAt ? 'Đã xác nhận' : 'Chưa xác nhận'}</dd></div>
        <div><dt>Học viên</dt><dd>{settlement?.studentConfirmedAt ? 'Đã xác nhận' : 'Chưa xác nhận'}</dd></div>
      </dl>
    </section>}
    <section className="tm-booking-actions" aria-label="Thao tác buổi học">{actions}</section>
    {currentReview && <div className="tm-booking-existing-review">Bạn đã đánh giá <strong>{currentReview.rating}/5</strong>{currentReview.comment ? ` · ${currentReview.comment}` : ''}</div>}
  </>;
}
