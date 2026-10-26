'use client';

import React from 'react';
import { Alert, Button } from 'antd';
import { useAdminDashboardStats } from '../hooks/useAdminFinance';
import { AdminDashboardOverview } from '../components/AdminDashboardOverview';
import styles from './AdminDashboardPage.module.css';

export const AdminDashboardPage: React.FC = () => {
  const query = useAdminDashboardStats();

  return <div className={styles.page}>
    <header className={styles.heading}>
      <h1>Tổng quan quản trị</h1>
      <p>Số liệu hoạt động và các yêu cầu cần xử lý.</p>
    </header>

    {query.isError
      ? <Alert
          className={styles.error}
          type="error"
          showIcon
          message="Chưa tải được số liệu quản trị"
          description="Dữ liệu chưa sẵn sàng. Hãy thử tải lại trước khi xử lý công việc."
          action={<Button onClick={() => query.refetch()}>Tải lại</Button>}
        />
      : <AdminDashboardOverview stats={query.data?.data} loading={query.isLoading} />}
  </div>;
};
