'use client';

import React from 'react';
import { Typography } from 'antd';
import { useAdminDashboardStats } from '../hooks/useAdminFinance';
import { AdminDashboardOverview } from '../components/AdminDashboardOverview';

export const AdminDashboardPage: React.FC = () => {
  const { data: statsRes, isLoading } = useAdminDashboardStats();
  const stats = statsRes?.data;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Bảng điều khiển Quản trị (Admin Dashboard)
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Tổng quan số liệu kinh doanh, hoa hồng sàn và các hàng đợi phê duyệt
        </Typography.Text>
      </div>

      <AdminDashboardOverview stats={stats} loading={isLoading} />
    </div>
  );
};
