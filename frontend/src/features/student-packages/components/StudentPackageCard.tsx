'use client';

import React from 'react';
import Link from 'next/link';
import { Tag } from 'antd';
import { StudentPackageSummary, StudentPackageStatus } from '../types';
import { formatVietnamDate } from '@/shared/lib/vietnamTime';

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

export const formatVnd = (amount: number): string => `${amount.toLocaleString('vi-VN')} ₫`;

export const formatDate = (dateStr?: string): string => {
  if (!dateStr) return '---';
  return formatVietnamDate(dateStr);
};

export const StudentPackageCard: React.FC<StudentPackageCardProps> = ({ packageData }) => {
  const statusConfig = getPackageStatusConfig(packageData.status);
  const sessionCounts = [
    { label: 'Còn lại', value: packageData.remainingSessions },
    { label: 'Đang giữ', value: packageData.reservedSessions },
    { label: 'Đã học', value: packageData.completedSessions },
    { label: 'Đã hoàn', value: packageData.refundedSessions },
  ];

  return (
    <article className="tm-student-package-item">
      <header className="tm-student-package-item-heading">
        <div className="tm-student-package-identity">
          <h2>{packageData.packageName}</h2>
          <p><span>{packageData.subject.name}</span><span className="tm-student-package-separator" aria-hidden="true">·</span><span>Gia sư {packageData.teacher.fullName}</span></p>
        </div>
        <Tag color={statusConfig.color}>{statusConfig.label}</Tag>
      </header>

      <dl className="tm-student-package-sessions" aria-label="Phân bổ số buổi học">
        {sessionCounts.map(({ label, value }) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>

      <footer className="tm-student-package-item-footer">
        <dl className="tm-student-package-terms">
          <div>
            <dt>Hạn dùng</dt>
            <dd>{formatDate(packageData.expiresAt)}</dd>
          </div>
          <div>
            <dt>Đã thanh toán</dt>
            <dd>{formatVnd(packageData.purchasePriceVnd)}</dd>
          </div>
        </dl>
        <Link className="tm-student-package-detail-link" href={`/student/packages/${packageData.id}`}>
          Xem chi tiết
        </Link>
      </footer>
    </article>
  );
};
