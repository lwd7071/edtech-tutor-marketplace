'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Form, Input, Modal, Skeleton, Tag, Typography, message } from 'antd';
import { bookingApi } from '../api/bookingApi';
import { bookingKeys } from '../data/bookingKeys';
import { SessionReportModal } from '../components/SessionReportModal';
import { CancelBookingModal } from '../components/CancelBookingModal';
import { ReviewBookingModal } from '../components/ReviewBookingModal';
import { BackLink } from '@/shared/components/navigation/NavigationLinks';
import { MessageTeacherButton } from '@/features/chat/components/MessageTeacherButton';
import { useConfirmBooking, useDisputeBooking } from '../hooks/useBookings';

export default function BookingDetailPage({ role }: { role: 'student' | 'teacher' }) {
  const { id } = useParams<{ id: string }>();
  const [action, setAction] = useState<string>();
  const [disputeOpen, setDisputeOpen] = useState(false);
  const confirmMutation = useConfirmBooking();
  const disputeMutation = useDisputeBooking();
  const booking = useQuery({ queryKey: bookingKeys.detail(role, id), queryFn: () => bookingApi.getBookingDetail(role, id) });
  const review = useQuery({
    queryKey: ['booking-review', id],
    queryFn: () => bookingApi.getReview(id),
    enabled: role === 'student' && booking.data?.data?.status === 'COMPLETED',
  });
  if (booking.isLoading) return <Skeleton active />;
  if (booking.isError || !booking.data?.data) return <Alert type="error" title="Chưa đọc được buổi học" description="Buổi học có thể không tồn tại hoặc không thuộc tài khoản của bạn." action={<Button onClick={() => booking.refetch()}>Thử lại</Button>} />;

  const item = booking.data.data;
  const currentReview = review.data?.data;
  const close = () => { setAction(undefined); void booking.refetch(); void review.refetch(); };
  const labels = { SCHEDULED: 'Đã lên lịch', COMPLETED: 'Đã hoàn thành', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn' };
  const settlementLabels = { AWAITING_CONFIRMATION: 'Chờ xác nhận hai bên', HELD: 'Đang giữ tiền', DISPUTE_PENDING: 'Chờ xử lý khiếu nại', REOPENED: 'Đã mở lại xác nhận', AWAITING_ADMIN_DECISION: 'Chờ quyết định quản trị', RELEASED: 'Đã giải ngân', RETAINED: 'Đã giữ lại cho nền tảng' } as const;
  const settlement = item.settlement;
  const confirm = async () => { try { await confirmMutation.mutateAsync({ id: item.id, data: { version: item.version } }); message.success('Đã xác nhận buổi học.'); void booking.refetch(); } catch { message.error('Không thể xác nhận buổi học. Vui lòng tải lại và thử lại.'); } };
  return <div className="tm-stack">
    <BackLink href={`/${role}/bookings`}>Lịch học</BackLink>
    <section className="tm-panel">
      <p className="tm-eyebrow">{item.trial ? 'Buổi học thử' : 'Buổi học'}</p><h1>{item.subject.name}</h1>
      <p>Gia sư: {item.teacher.fullName} · Học viên: {item.student.fullName}</p>
      <p>{new Date(item.startTime).toLocaleString('vi-VN')} – {new Date(item.endTime).toLocaleTimeString('vi-VN')}</p>
      <p>Trạng thái: {labels[item.status]}</p>
      {!item.trial && item.settlementStatus && <div style={{ margin: '12px 0' }}><Typography.Text>Quyết toán: <Tag color={item.settlementStatus === 'RELEASED' ? 'green' : item.settlementStatus === 'HELD' ? 'orange' : 'blue'}>{settlementLabels[item.settlementStatus]}</Tag></Typography.Text><br /><Typography.Text type="secondary">Hạn xác nhận: {settlement?.confirmationDeadline ? new Date(settlement.confirmationDeadline).toLocaleString('vi-VN') : '—'} · Gia sư: {settlement?.teacherConfirmedAt ? 'đã xác nhận' : 'chưa xác nhận'} · Học viên: {settlement?.studentConfirmedAt ? 'đã xác nhận' : 'chưa xác nhận'}</Typography.Text></div>}
      {item.deliveryMode === 'ONLINE' && item.meetingLink && /^https?:\/\//.test(item.meetingLink) && item.status === 'SCHEDULED' && <a className="tm-button" href={item.meetingLink} target="_blank" rel="noopener noreferrer">Mở phòng học ↗</a>}
      {item.locationAddress && <p>Địa điểm: {item.locationAddress}</p>}
      {role === 'student' && <MessageTeacherButton teacherId={item.teacher.id} />}
      {role === 'student' && item.canConfirm && <Button type="primary" onClick={confirm} loading={confirmMutation.isPending}>Xác nhận buổi học</Button>}
      {role === 'teacher' && (item.status === 'SCHEDULED' || (!item.trial && item.settlementStatus === 'REOPENED' && !item.sessionReport)) && <div className="tm-inline"><Button type="primary" onClick={() => setAction('complete')}>{item.settlementStatus === 'REOPENED' ? 'Xác nhận lại và nộp báo cáo' : 'Hoàn thành và viết báo cáo'}</Button>{item.status === 'SCHEDULED' && <Button onClick={() => setAction('cancel')}>Hủy buổi học</Button>}</div>}
      {role === 'teacher' && item.canDispute && <Button onClick={() => setDisputeOpen(true)}>Khiếu nại khoản tiền đang giữ</Button>}
      {role === 'student' && item.status === 'COMPLETED' && !review.isLoading && !currentReview && <Button onClick={() => setAction('review')}>Đánh giá buổi học</Button>}
      {currentReview && <p>Bạn đã đánh giá <strong>{currentReview.rating}/5</strong>{currentReview.comment ? ` · ${currentReview.comment}` : ''}</p>}
    </section>
    {item.sessionReport && <section className="tm-panel"><h2>Báo cáo buổi học</h2><p className="tm-prose">{item.sessionReport.content}</p><h3>Nhận xét</h3><p>{item.sessionReport.feedback}</p>{item.sessionReport.followUpNote && <p>Việc cần làm tiếp: {item.sessionReport.followUpNote}</p>}</section>}
    <SessionReportModal open={action === 'complete'} booking={item} onClose={close} />
    <CancelBookingModal open={action === 'cancel'} booking={item} onClose={close} />
    <ReviewBookingModal visible={action === 'review'} bookingId={item.id} teacherName={item.teacher.fullName} onCancel={close} onSuccess={close} />
    <Modal title="Khiếu nại khoản tiền đang giữ" open={disputeOpen} footer={null} onCancel={() => setDisputeOpen(false)} destroyOnHidden><Form layout="vertical" onFinish={async values => { try { await disputeMutation.mutateAsync({ id: item.id, data: { version: item.version, reason: values.reason } }); message.success('Đã gửi khiếu nại.'); setDisputeOpen(false); void booking.refetch(); } catch { message.error('Không thể gửi khiếu nại.'); } }}><Form.Item name="reason" label="Lý do" rules={[{ required: true, min: 10, message: 'Vui lòng nêu rõ lý do (ít nhất 10 ký tự).' }]}><Input.TextArea rows={4} maxLength={2000} /></Form.Item><Button type="primary" htmlType="submit" loading={disputeMutation.isPending}>Gửi khiếu nại</Button></Form></Modal>
  </div>;
}
