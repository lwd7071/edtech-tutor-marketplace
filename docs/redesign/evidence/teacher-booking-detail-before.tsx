'use client';

import type { ReactNode } from 'react';
import { Alert, Tag, Typography } from 'antd';
import type { BookingDetail } from '../types';
import { formatVietnamDateTime, formatVietnamTime } from '@/shared/lib/vietnamTime';

export function TeacherBookingDetailView({ item, labels, settlementLabels, actions }: {
  item: BookingDetail;
  labels: Record<BookingDetail['status'], string>;
  settlementLabels: Record<NonNullable<BookingDetail['settlementStatus']>, string>;
  actions: ReactNode;
}) {
  return <section className="tm-panel">
    <p className="tm-eyebrow">{item.trial ? 'Buổi học thử' : 'Buổi học'}</p><h1>{item.subject.name}</h1>
    <p>Gia sư: {item.teacher.fullName} · Học viên: {item.student.fullName}</p>
    <p>{formatVietnamDateTime(item.startTime)} – {formatVietnamTime(item.endTime)}</p>
    <p>Trạng thái: {labels[item.status]}</p>
    {item.outsideAvailabilityWarning && <Alert type="warning" showIcon title="Buổi học nằm ngoài lịch rảnh đã công bố" />}
    {!item.trial && item.settlementStatus && <div style={{ margin: '12px 0' }}><Typography.Text>Quyết toán: <Tag color={item.settlementStatus === 'RELEASED' ? 'green' : item.settlementStatus === 'HELD' ? 'orange' : 'blue'}>{settlementLabels[item.settlementStatus]}</Tag></Typography.Text><br /><Typography.Text type="secondary">Hạn xác nhận: {item.settlement?.confirmationDeadline ? formatVietnamDateTime(item.settlement.confirmationDeadline) : '—'} · Gia sư: {item.settlement?.teacherConfirmedAt ? 'đã xác nhận' : 'chưa xác nhận'} · Học viên: {item.settlement?.studentConfirmedAt ? 'đã xác nhận' : 'chưa xác nhận'}</Typography.Text></div>}
    {item.deliveryMode === 'ONLINE' && item.meetingLink && /^https?:\/\//.test(item.meetingLink) && item.status === 'SCHEDULED' && <a className="tm-button" href={item.meetingLink} target="_blank" rel="noopener noreferrer">Mở phòng học ↗</a>}
    {item.locationAddress && <p>Địa điểm: {item.locationAddress}</p>}
    {actions}
    {item.cancelReason && item.status === 'CANCELLED' && <p style={{ whiteSpace: 'pre-wrap' }}>Lý do hủy: {item.cancelReason}</p>}
  </section>;
}
