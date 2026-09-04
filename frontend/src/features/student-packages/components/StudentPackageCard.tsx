'use client';

import React from 'react';
import { Card, Tag, Typography, Button, Space } from 'antd';
import Link from 'next/link';
import { StudentPackageSummary, StudentPackageStatus } from '../types';
import { SessionCounter } from './SessionCounter';

interface StudentPackageCardProps {
  packageData: StudentPackageSummary;
}

export const getPackageStatusConfig = (status: StudentPackageStatus) => {
  switch (status) {
    case 'ACTIVE':
      return { color: 'success', label: 'Đang hoạt động' };
    case 'PENDING_PAYMENT':
      return { color: 'warning', label: 'Chờ thanh toán' };
    case 'COMPLETED':
      return { color: 'processing', label: 'Đã hoàn thành' };
    case 'LOCKED_EXPIRED':
      return { color: 'error', label: 'Hết hạn' };
    case 'REFUND_PENDING':
      return { color: 'warning', label: 'Đang xử lý hoàn tiền' };
    case 'REFUNDED':
      return { color: 'default', label: 'Đã hoàn tiền' };
    default:
      return { color: 'default', label: status };
  }
};

export const formatVnd = (amount: number): string => {
  return `${amount.toLocaleString('vi-VN')} ₫`;
};

export const formatDate = (dateStr?: string): string => {
  if (!dateStr) return '---';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
};

export const StudentPackageCard: React.FC<StudentPackageCardProps> = ({
  packageData,
}) => {
  const statusConfig = getPackageStatusConfig(packageData.status);

  return (
    <Card
      style={{
        width: '100%',
        borderRadius: 'var(--radius-lg, 12px)',
        border: '1px solid var(--color-border, #E7E3DC)',
      }}
      styles={{ body: { padding: 'var(--space-4, 16px)' } }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {packageData.packageName}
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Môn: <strong>{packageData.subject.name}</strong> · Gia sư: <strong>{packageData.teacher.fullName}</strong>
          </Typography.Text>
        </div>
        <Tag color={statusConfig.color} style={{ margin: 0, fontWeight: 500 }}>
          {statusConfig.label}
        </Tag>
      </div>

      <div style={{ margin: '14px 0' }}>
        <SessionCounter
          remainingSessions={packageData.remainingSessions}
          reservedSessions={packageData.reservedSessions}
          completedSessions={packageData.completedSessions}
          refundedSessions={packageData.refundedSessions}
          size="small"
        />
      </div>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginTop: 16,
          paddingTop: 12,
          borderTop: '1px solid var(--color-border, #E7E3DC)',
        }}
      >
        <div>
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary, #57534E)' }}>
            Hạn dùng: {formatDate(packageData.expiresAt)}
          </span>
          <div
            style={{
              fontSize: 16,
              fontWeight: 700,
              color: 'var(--color-primary-600, #0F766E)',
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {formatVnd(packageData.purchasePriceVnd)}
          </div>
        </div>

        <Link href={`/student/packages/${packageData.id}`}>
          <Button type="primary">Xem chi tiết</Button>
        </Link>
      </div>
    </Card>
  );
};
