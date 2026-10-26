'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Alert, Button, Input, Pagination, Select, Skeleton, Tag } from 'antd';
import { bookingApi } from '../api/bookingApi';
import type { BookingStatus } from '../types';
import { bookingKeys } from '../data/bookingKeys';
import { formatVietnamDateTime, vietnamDateRange } from '@/shared/lib/vietnamTime';
import TeacherBookingWorkspace from './TeacherBookingWorkspace';

const settlementLabels: Record<string, string> = {
  AWAITING_CONFIRMATION: 'Chờ xác nhận', HELD: 'Đang giữ tiền', DISPUTE_PENDING: 'Chờ khiếu nại',
  REOPENED: 'Đã mở lại', AWAITING_ADMIN_DECISION: 'Chờ quản trị', RELEASED: 'Đã giải ngân', RETAINED: 'Đã giữ nền tảng',
};
const bookingLabels: Record<BookingStatus, string> = {
  SCHEDULED: 'Sắp diễn ra', COMPLETED: 'Đã học', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn',
};

export default function BookingWorkspace({ role }: { role: 'student' | 'teacher' }) {
  return role === 'teacher' ? <TeacherBookingWorkspace /> : <StudentBookingWorkspace />;
}

function StudentBookingWorkspace() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<BookingStatus>();
  const [date, setDate] = useState('');
  const range = date ? vietnamDateRange(date) : { from: undefined, to: undefined };
  const params = { page, size: 12, status, ...range, sort: 'startTime,asc' };
  const query = useQuery({ queryKey: bookingKeys.list('student', params), queryFn: () => bookingApi.getBookings('student', params) });

  return (
    <div className="tm-student-bookings">
      <header className="tm-student-bookings-heading">
        <h1>Lịch học</h1>
        <p>Các buổi học đã được tạo. Thời gian hiển thị theo giờ Việt Nam.</p>
      </header>

      <section className="tm-student-bookings-filters" aria-label="Lọc lịch học">
        <label>
          <span>Trạng thái buổi học</span>
          <Select className="tm-student-bookings-status-filter" aria-label="Trạng thái buổi học" allowClear placeholder="Tất cả trạng thái" value={status}
            onChange={value => { setStatus(value); setPage(0); }}
            options={Object.entries(bookingLabels).map(([value, label]) => ({ value, label }))} />
        </label>
        <label>
          <span>Ngày học theo giờ Việt Nam</span>
          <Input className="tm-student-bookings-date-filter" aria-label="Ngày học theo giờ Việt Nam" type="date" value={date}
            onChange={event => { setDate(event.target.value); setPage(0); }} />
        </label>
      </section>

      {query.isLoading ? (
        <section className="tm-student-bookings-state" role="status" aria-live="polite" aria-busy="true">
          <p>Đang tải lịch học</p>
          <Skeleton active title={false} paragraph={{ rows: 4 }} />
        </section>
      ) : query.isError ? (
        <section className="tm-student-bookings-state" aria-labelledby="tm-student-bookings-error-title">
          <div>
            <h2 id="tm-student-bookings-error-title">Chưa tải được lịch</h2>
            <p>Thử tải lại danh sách các buổi học.</p>
          </div>
          <Button className="tm-student-bookings-retry" onClick={() => query.refetch()}>Thử lại</Button>
        </section>
      ) : query.data?.data.length ? (
        <section className="tm-student-bookings-results" aria-labelledby="tm-student-bookings-results-title">
          <h2 id="tm-student-bookings-results-title">Các buổi học</h2>
          <ol>
            {query.data.data.map(booking => (
              <li key={booking.id}>
                <article className="tm-student-booking-row">
                  <div className="tm-student-booking-heading">
                    <div className="tm-student-booking-title">
                      <h3>{booking.subject.name}</h3>
                      <p>{booking.teacher.fullName}</p>
                    </div>
                    <div className="tm-student-booking-statuses">
                      <span className="tm-student-booking-kind">{booking.trial ? 'Buổi học thử' : 'Buổi học'}</span>
                      <Tag>{bookingLabels[booking.status]}</Tag>
                      {!booking.trial && booking.settlementStatus && (
                        <Tag color={booking.settlementStatus === 'RELEASED' ? 'green' : booking.settlementStatus === 'HELD' ? 'orange' : 'blue'}>
                          {settlementLabels[booking.settlementStatus]}
                        </Tag>
                      )}
                    </div>
                  </div>
                  <dl className="tm-student-booking-details">
                    <div>
                      <dt>Thời gian</dt>
                      <dd>{formatVietnamDateTime(booking.startTime)}</dd>
                    </div>
                    <div>
                      <dt>Hình thức</dt>
                      <dd>{booking.deliveryMode === 'ONLINE' ? 'Online' : 'Trực tiếp'}</dd>
                    </div>
                  </dl>
                  <Link className="tm-student-booking-detail-link" href={`/student/bookings/${booking.id}`}>
                    Xem chi tiết
                  </Link>
                </article>
              </li>
            ))}
          </ol>
          <Pagination className="tm-student-bookings-pagination" current={page + 1} pageSize={12} total={query.data.meta?.totalElements || 0}
            showSizeChanger={false} onChange={next => setPage(next - 1)} />
        </section>
      ) : (
        <section className="tm-student-bookings-empty" aria-labelledby="tm-student-bookings-empty-title">
          <h2 id="tm-student-bookings-empty-title">Chưa có buổi học với điều kiện đã chọn.</h2>
        </section>
      )}
    </div>
  );
}
