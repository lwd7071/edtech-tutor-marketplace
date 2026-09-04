'use client';

import React, { useState } from 'react';
import { Typography, Space } from 'antd';
import { BookingStatus } from '../types';
import { useStudentBookings } from '../hooks/useBookings';
import { BookingCalendarView } from '../components/BookingCalendarView';

export const StudentBookingsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<BookingStatus | undefined>(undefined);

  const { data, isLoading } = useStudentBookings({
    status: statusFilter,
  });

  const bookings = data?.data || [];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6, 24px) var(--space-4, 16px)' }}>
      <Space orientation="vertical" size="small" style={{ width: '100%', marginBottom: 24 }}>
        <Typography.Title level={2} style={{ margin: 0 }}>
          Lịch học của tôi
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ margin: 0 }}>
          Theo dõi danh sách các buổi học gia sư 1-1, tham gia lớp học trực tuyến và xem báo cáo kết quả học tập.
        </Typography.Paragraph>
      </Space>

      <BookingCalendarView
        bookings={bookings}
        isLoading={isLoading}
        onStatusChange={(status) => setStatusFilter(status)}
      />
    </div>
  );
};
