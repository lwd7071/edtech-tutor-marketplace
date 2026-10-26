'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Link from 'next/link';
import { Alert, Button, Input, Pagination, Select, Skeleton, Tag } from 'antd';
import { bookingApi } from '../api/bookingApi';
import type { BookingStatus } from '../types';
import { bookingKeys } from '../data/bookingKeys';
import { formatVietnamDateTime, vietnamDateRange } from '@/shared/lib/vietnamTime';
import styles from './TeacherBookingWorkspace.module.css';

const settlementLabels: Record<string, string> = {
  AWAITING_CONFIRMATION: 'Chờ xác nhận', HELD: 'Đang giữ tiền', DISPUTE_PENDING: 'Chờ khiếu nại',
  REOPENED: 'Đã mở lại', AWAITING_ADMIN_DECISION: 'Chờ quản trị', RELEASED: 'Đã giải ngân', RETAINED: 'Đã giữ nền tảng',
};
const bookingLabels: Record<BookingStatus, string> = {
  SCHEDULED: 'Sắp diễn ra', COMPLETED: 'Đã học', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn',
};

export default function TeacherBookingWorkspace() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<BookingStatus>();
  const [date, setDate] = useState('');
  const range = date ? vietnamDateRange(date) : { from: undefined, to: undefined };
  const params = { page, size: 12, status, ...range, sort: 'startTime,asc' };
  const query = useQuery({ queryKey: bookingKeys.list('teacher', params), queryFn: () => bookingApi.getBookings('teacher', params) });

  if (query.isLoading) {
    return (
      <main className={styles.page}>
        <header className={styles.header}>
          <h1 className={styles.title}>Lịch dạy</h1>
          <p className={styles.description}>Các buổi học đã được tạo. Thời gian hiển thị theo giờ Việt Nam.</p>
        </header>
        <div className={styles.loading} role="status" aria-live="polite" aria-busy="true">
          <p>Đang tải lịch dạy</p>
          <Skeleton active title={false} paragraph={{ rows: 4 }} />
        </div>
      </main>
    );
  }

  if (query.isError) {
    return (
      <main className={styles.page}>
        <header className={styles.header}>
          <h1 className={styles.title}>Lịch dạy</h1>
          <p className={styles.description}>Các buổi học đã được tạo. Thời gian hiển thị theo giờ Việt Nam.</p>
        </header>
        <Alert
          className={styles.error}
          type="error"
          title="Chưa tải được lịch"
          action={<Button onClick={() => query.refetch()}>Thử lại</Button>}
        />
      </main>
    );
  }

  const bookings = query.data?.data ?? [];

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>Lịch dạy</h1>
        <p className={styles.description}>Các buổi học đã được tạo. Thời gian hiển thị theo giờ Việt Nam.</p>
      </header>

      <section className={styles.toolbar} aria-label="Lọc lịch dạy">
        <label className={styles.filter}>
          <span>Trạng thái buổi học</span>
          <Select
            aria-label="Trạng thái buổi học"
            allowClear
            placeholder="Tất cả trạng thái"
            value={status}
            onChange={value => { setStatus(value); setPage(0); }}
            options={Object.entries(bookingLabels).map(([value, label]) => ({ value, label }))}
          />
        </label>
        <label className={styles.filter}>
          <span>Ngày học theo giờ Việt Nam</span>
          <Input
            aria-label="Ngày học theo giờ Việt Nam"
            type="date"
            value={date}
            onChange={event => { setDate(event.target.value); setPage(0); }}
          />
        </label>
        <Link className={styles.createLink} href="/teacher/students">Chọn học viên để tạo buổi</Link>
      </section>

      {bookings.length > 0 ? (
        <section className={styles.results} aria-labelledby="teacher-booking-list-title">
          <h2 id="teacher-booking-list-title">Các buổi dạy</h2>
          <ol className={styles.list}>
            {bookings.map(booking => (
              <li key={booking.id} className={styles.listItem}>
                <article className={styles.row}>
                  <div className={styles.identity}>
                    <h3>{booking.subject.name}</h3>
                    <p>{booking.student.fullName}</p>
                  </div>
                  <div className={styles.timing}>
                    <span className={styles.time}>{formatVietnamDateTime(booking.startTime)}</span>
                    <span>{booking.deliveryMode === 'ONLINE' ? 'Online' : 'Trực tiếp'}</span>
                  </div>
                  <div className={styles.statuses} aria-label="Trạng thái buổi học">
                    <span className={styles.kind}>{booking.trial ? 'Buổi học thử' : 'Buổi học'}</span>
                    <Tag>{bookingLabels[booking.status]}</Tag>
                    {!booking.trial && booking.settlementStatus && (
                      <Tag color={booking.settlementStatus === 'RELEASED' ? 'green' : booking.settlementStatus === 'HELD' ? 'orange' : 'blue'}>
                        {settlementLabels[booking.settlementStatus]}
                      </Tag>
                    )}
                  </div>
                  <Link className={styles.detailLink} href={`/teacher/bookings/${booking.id}`}>Xem chi tiết</Link>
                </article>
              </li>
            ))}
          </ol>
          <Pagination
            className={styles.pagination}
            current={page + 1}
            pageSize={12}
            total={query.data?.meta?.totalElements || 0}
            showSizeChanger={false}
            onChange={next => setPage(next - 1)}
          />
        </section>
      ) : (
        <section className={styles.empty} aria-labelledby="teacher-booking-empty-title">
          <h2 id="teacher-booking-empty-title">Chưa có buổi học với điều kiện đã chọn.</h2>
        </section>
      )}
    </main>
  );
}
