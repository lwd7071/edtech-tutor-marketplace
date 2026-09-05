'use client';

import React from 'react';
import { Tag } from 'antd';
import { BookingStatus } from '../types';

interface BookingStatusTagProps {
  status: BookingStatus;
}

export const getBookingStatusConfig = (status: BookingStatus) => {
  switch (status) {
    case 'SCHEDULED':
      return { color: 'processing', label: 'Đã lên lịch' };
    case 'COMPLETED':
      return { color: 'success', label: 'Hoàn thành' };
    case 'CANCELLED':
      return { color: 'default', label: 'Đã hủy' };
    case 'EXPIRED':
      return { color: 'error', label: 'Quá hạn xác nhận' };
    default:
      return { color: 'default', label: status };
  }
};

export const BookingStatusTag: React.FC<BookingStatusTagProps> = ({ status }) => {
  const config = getBookingStatusConfig(status);
  return (
    <Tag color={config.color} style={{ fontWeight: 500, margin: 0 }}>
      {config.label}
    </Tag>
  );
};
