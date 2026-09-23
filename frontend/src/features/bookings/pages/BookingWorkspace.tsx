'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Alert, Button, Select, Pagination, Skeleton, Input, Tag } from 'antd';
import { bookingApi } from '../api/bookingApi';
import type { BookingStatus } from '../types';
import { bookingKeys } from '../data/bookingKeys';
import { formatVietnamDateTime, vietnamDateRange } from '@/shared/lib/vietnamTime';

const settlementLabels: Record<string, string> = {
  AWAITING_CONFIRMATION: 'Chờ xác nhận', HELD: 'Đang giữ tiền', DISPUTE_PENDING: 'Chờ khiếu nại',
  REOPENED: 'Đã mở lại', AWAITING_ADMIN_DECISION: 'Chờ quản trị', RELEASED: 'Đã giải ngân', RETAINED: 'Đã giữ nền tảng',
};
const bookingLabels: Record<BookingStatus, string> = {
  SCHEDULED: 'Sắp diễn ra', COMPLETED: 'Đã học', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn',
};

export default function BookingWorkspace({ role }: { role: 'student' | 'teacher' }) {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<BookingStatus>();
  const [date, setDate] = useState('');
  const range = date ? vietnamDateRange(date) : { from: undefined, to: undefined };
  const params = { page, size: 12, status, ...range, sort: 'startTime,asc' };
  const query = useQuery({ queryKey: bookingKeys.list(role, params), queryFn: () => bookingApi.getBookings(role, params) });

  return <div className="tm-stack">
    <header className="tm-page-heading">
      <h1>{role === 'teacher' ? 'Lịch dạy' : 'Lịch học'}</h1>
      <p>Các buổi học đã được tạo. Thời gian hiển thị theo giờ Việt Nam.</p>
    </header>
    <div className="tm-toolbar">
      <div className="tm-inline">
        <Select aria-label="Trạng thái buổi học" allowClear placeholder="Tất cả trạng thái" value={status}
          onChange={value => { setStatus(value); setPage(0); }} style={{ width: 190 }}
          options={Object.entries(bookingLabels).map(([value, label]) => ({ value, label }))} />
        <Input aria-label="Ngày học theo giờ Việt Nam" type="date" value={date}
          onChange={event => { setDate(event.target.value); setPage(0); }} />
      </div>
      {role === 'teacher' && <Link className="tm-button" href="/teacher/students">Chọn học viên để tạo buổi</Link>}
    </div>
    {query.isLoading ? <Skeleton active /> : query.isError ?
      <Alert type="error" title="Chưa tải được lịch" action={<Button onClick={() => query.refetch()}>Thử lại</Button>} /> : <>
        {query.data?.data.length ? <div className="tm-package-grid">{query.data.data.map(booking =>
          <Link href={`/${role}/bookings/${booking.id}`} key={booking.id} className="tm-panel tm-dashboard-link">
            <p className="tm-eyebrow">{booking.trial ? 'Buổi học thử' : 'Buổi học'} · {bookingLabels[booking.status]}
              {!booking.trial && booking.settlementStatus && <Tag color={booking.settlementStatus === 'RELEASED' ? 'green' : booking.settlementStatus === 'HELD' ? 'orange' : 'blue'}>{settlementLabels[booking.settlementStatus]}</Tag>}
            </p>
            <h2>{booking.subject.name}</h2>
            <p>{role === 'teacher' ? booking.student.fullName : booking.teacher.fullName}</p>
            <strong>{formatVietnamDateTime(booking.startTime)}</strong>
            <p>{booking.deliveryMode === 'ONLINE' ? 'Online' : 'Trực tiếp'} · Xem chi tiết →</p>
          </Link>)}</div> : <section className="tm-panel">Chưa có buổi học với điều kiện đã chọn.</section>}
        <Pagination current={page + 1} pageSize={12} total={query.data?.meta?.totalElements || 0}
          showSizeChanger={false} onChange={next => setPage(next - 1)} />
      </>}
  </div>;
}
