import React from 'react';
import { Tag } from 'antd';

export type StatusTagDomain = 'TeacherProfile' | 'Invoice' | 'StudentPackage' | 'Booking' | 'Payout';

export interface StatusTagProps {
  domain: StatusTagDomain;
  status: string;
}

const statusConfig: Record<StatusTagDomain, Record<string, { color: string; label: string }>> = {
  TeacherProfile: {
    DRAFT: { color: 'default', label: 'Bản nháp' },
    PENDING_APPROVAL: { color: 'warning', label: 'Chờ duyệt' },
    APPROVED: { color: 'success', label: 'Đã duyệt' },
    REJECTED: { color: 'error', label: 'Từ chối' },
  },
  Invoice: {
    PENDING: { color: 'warning', label: 'Đang xử lý' },
    PAID: { color: 'success', label: 'Đã thanh toán' },
    CANCELLED: { color: 'default', label: 'Đã hủy' },
    EXPIRED: { color: 'error', label: 'Hết hạn' },
  },
  StudentPackage: {
    PENDING_PAYMENT: { color: 'warning', label: 'Chờ thanh toán' },
    ACTIVE: { color: 'success', label: 'Đang kích hoạt' },
    COMPLETED: { color: 'processing', label: 'Đã hoàn thành' }, // mapping info to processing in antd
    LOCKED_EXPIRED: { color: 'error', label: 'Bị khóa/Hết hạn' },
    REFUND_PENDING: { color: 'warning', label: 'Chờ hoàn tiền' },
    REFUNDED: { color: 'default', label: 'Đã hoàn tiền' },
  },
  Booking: {
    SCHEDULED: { color: 'processing', label: 'Đã lên lịch' },
    COMPLETED: { color: 'success', label: 'Đã hoàn thành' },
    CANCELLED: { color: 'default', label: 'Đã hủy' },
    EXPIRED: { color: 'error', label: 'Hết hạn' },
  },
  Payout: {
    PENDING: { color: 'warning', label: 'Đang xử lý' },
    PROCESSING: { color: 'processing', label: 'Đang thực hiện' },
    SUCCEEDED: { color: 'success', label: 'Thành công' },
    REJECTED: { color: 'default', label: 'Từ chối' },
    FAILED: { color: 'error', label: 'Thất bại' },
  },
};

export const StatusTag: React.FC<StatusTagProps> = ({ domain, status }) => {
  const config = statusConfig[domain]?.[status] || { color: 'default', label: status };

  return (
    <Tag color={config.color} style={{ borderRadius: 'var(--radius-xs)' }}>
      {config.label}
    </Tag>
  );
};
