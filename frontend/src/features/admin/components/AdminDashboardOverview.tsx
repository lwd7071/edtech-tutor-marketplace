'use client';

import React from 'react';
import Link from 'next/link';
import { Skeleton } from 'antd';
import { AdminDashboardView } from '../types';
import styles from './AdminDashboardOverview.module.css';

interface AdminDashboardOverviewProps {
  stats?: AdminDashboardView | null;
  loading?: boolean;
}

const formatVnd = (amount: number) => `${amount.toLocaleString('vi-VN')} ₫`;

const shortcuts = [
  { href: '/admin/teachers', label: 'Duyệt hồ sơ gia sư', group: 'Hồ sơ' },
  { href: '/admin/credentials', label: 'Duyệt minh chứng', group: 'Hồ sơ' },
  { href: '/admin/subjects', label: 'Đề xuất môn học', group: 'Danh mục' },
  { href: '/admin/users', label: 'Quản lý người dùng', group: 'Danh mục' },
  { href: '/admin/refunds', label: 'Yêu cầu hoàn tiền', group: 'Tài chính' },
  { href: '/admin/extensions', label: 'Yêu cầu gia hạn', group: 'Tài chính' },
  { href: '/admin/payouts', label: 'Yêu cầu rút tiền', group: 'Tài chính' },
  { href: '/admin/booking-settlements', label: 'Quyết toán buổi học', group: 'Tài chính' },
];

function DashboardSkeleton() {
  return <div className={styles.skeleton} aria-label="Đang tải số liệu quản trị">
    <Skeleton active title={{ width: 240 }} paragraph={{ rows: 3 }} />
    <Skeleton active title={{ width: 200 }} paragraph={{ rows: 2 }} />
  </div>;
}

export const AdminDashboardOverview: React.FC<AdminDashboardOverviewProps> = ({ stats, loading = false }) => {
  if (loading) return <DashboardSkeleton />;
  if (!stats) return <p className={styles.empty} role="status">Chưa có số liệu tổng quan để hiển thị.</p>;

  const financial = [
    { label: 'Tổng GMV', value: formatVnd(stats.totalGmvVnd), note: 'Giá trị các gói học đã thanh toán' },
    { label: 'Hoa hồng', value: formatVnd(stats.totalCommissionVnd), note: 'Từ các buổi học hoàn thành' },
    { label: 'Lệnh rút tiền chờ duyệt', value: `${stats.pendingPayoutsCount} lệnh`, note: formatVnd(stats.pendingPayoutsAmountVnd), href: '/admin/payouts' },
    { label: 'Yêu cầu hoàn tiền chờ xử lý', value: `${stats.pendingRefundsCount} yêu cầu`, note: 'Học viên đang chờ thẩm định', href: '/admin/refunds' },
  ];

  const activity = [
    { label: 'Gia sư trên sàn', value: stats.totalTeachers.toLocaleString('vi-VN') },
    { label: 'Học viên trên sàn', value: stats.totalStudents.toLocaleString('vi-VN') },
    { label: 'Tổng số buổi học', value: stats.totalBookings.toLocaleString('vi-VN') },
    { label: 'Đã hoàn thành', value: `${stats.completedBookings.toLocaleString('vi-VN')} buổi` },
    { label: 'Đã lên lịch', value: `${stats.scheduledBookings.toLocaleString('vi-VN')} buổi` },
    { label: 'Đã hủy', value: `${stats.cancelledBookings.toLocaleString('vi-VN')} buổi` },
  ];

  return <div className={styles.overview}>
    <section aria-labelledby="admin-finance-heading" className={styles.section}>
      <h2 id="admin-finance-heading">Tài chính và yêu cầu</h2>
      <dl className={styles.financeList}>
        {financial.map(item => <div className={styles.financeRow} key={item.label}>
          <div className={styles.financeCopy}>
            <dt>{item.href ? <Link href={item.href}>{item.label}</Link> : item.label}</dt>
            <dd>{item.note}</dd>
          </div>
          <div className={styles.financeValue}>{item.value}</div>
        </div>)}
      </dl>
    </section>

    <section aria-labelledby="admin-activity-heading" className={styles.section}>
      <h2 id="admin-activity-heading">Người dùng và buổi học</h2>
      <dl className={styles.activityList}>
        {activity.map(item => <div className={styles.activityRow} key={item.label}>
          <dt>{item.label}</dt><dd>{item.value}</dd>
        </div>)}
      </dl>
    </section>

    <section aria-labelledby="admin-queues-heading" className={styles.section}>
      <h2 id="admin-queues-heading">Đi đến công việc quản trị</h2>
      <ul className={styles.queueList}>
        {shortcuts.map(item => <li key={item.href}>
          <span className={styles.queueGroup}>{item.group}</span>
          <Link href={item.href}>{item.label}</Link>
        </li>)}
      </ul>
    </section>
  </div>;
};
