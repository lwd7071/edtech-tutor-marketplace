'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Skeleton } from 'antd';
import { bookingApi } from '../api/bookingApi';
import { bookingKeys } from '../data/bookingKeys';
import { SessionReportModal } from '../components/SessionReportModal';
import { CancelBookingModal } from '../components/CancelBookingModal';
import { ReviewBookingModal } from '../components/ReviewBookingModal';
import { BackLink } from '@/shared/components/navigation/NavigationLinks';
import { MessageTeacherButton } from '@/features/chat/components/MessageTeacherButton';

export default function BookingDetailPage({ role }: { role: 'student' | 'teacher' }) {
  const { id } = useParams<{ id: string }>();
  const [action, setAction] = useState<string>();
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
  return <div className="tm-stack">
    <BackLink href={`/${role}/bookings`}>Lịch học</BackLink>
    <section className="tm-panel">
      <p className="tm-eyebrow">{item.trial ? 'Buổi học thử' : 'Buổi học'}</p><h1>{item.subject.name}</h1>
      <p>Gia sư: {item.teacher.fullName} · Học viên: {item.student.fullName}</p>
      <p>{new Date(item.startTime).toLocaleString('vi-VN')} – {new Date(item.endTime).toLocaleTimeString('vi-VN')}</p>
      <p>Trạng thái: {labels[item.status]}</p>
      {item.deliveryMode === 'ONLINE' && item.meetingLink && /^https?:\/\//.test(item.meetingLink) && item.status === 'SCHEDULED' && <a className="tm-button" href={item.meetingLink} target="_blank" rel="noopener noreferrer">Mở phòng học ↗</a>}
      {item.locationAddress && <p>Địa điểm: {item.locationAddress}</p>}
      {role === 'student' && <MessageTeacherButton teacherId={item.teacher.id} />}
      {role === 'teacher' && item.status === 'SCHEDULED' && <div className="tm-inline"><Button type="primary" onClick={() => setAction('complete')}>Hoàn thành và viết báo cáo</Button><Button onClick={() => setAction('cancel')}>Hủy buổi học</Button></div>}
      {role === 'student' && item.status === 'COMPLETED' && !review.isLoading && !currentReview && <Button onClick={() => setAction('review')}>Đánh giá buổi học</Button>}
      {currentReview && <p>Bạn đã đánh giá <strong>{currentReview.rating}/5</strong>{currentReview.comment ? ` · ${currentReview.comment}` : ''}</p>}
    </section>
    {item.sessionReport && <section className="tm-panel"><h2>Báo cáo buổi học</h2><p className="tm-prose">{item.sessionReport.content}</p><h3>Nhận xét</h3><p>{item.sessionReport.feedback}</p>{item.sessionReport.followUpNote && <p>Việc cần làm tiếp: {item.sessionReport.followUpNote}</p>}</section>}
    <SessionReportModal open={action === 'complete'} booking={item} onClose={close} />
    <CancelBookingModal open={action === 'cancel'} booking={item} onClose={close} />
    <ReviewBookingModal visible={action === 'review'} bookingId={item.id} teacherName={item.teacher.fullName} onCancel={close} onSuccess={close} />
  </div>;
}
