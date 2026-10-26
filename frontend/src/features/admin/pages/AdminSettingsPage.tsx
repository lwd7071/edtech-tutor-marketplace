'use client';

import React from 'react';
import { Alert, Button, Skeleton, message } from 'antd';
import { parseApiError } from '@/shared/backend';
import { usePlatformSettings, useUpdatePlatformSettings } from '../hooks/useAdminFinance';
import { AdminSettingsForm } from '../components/AdminSettingsForm';
import styles from './AdminSettingsPage.module.css';

export const AdminSettingsPage: React.FC = () => {
  const query = usePlatformSettings();
  const updateMutation = useUpdatePlatformSettings();
  const settings = query.data?.data;

  return <main className={styles.page}>
    <header className={styles.heading}>
      <h1>Cài đặt nền tảng</h1>
      <p>Quản lý các tham số hiện có của hoa hồng, xếp hạng và lịch học.</p>
    </header>
    {query.isError ? <Alert className={styles.error} type="error" showIcon message="Không tải được cài đặt nền tảng." description="Kiểm tra kết nối rồi thử tải lại." action={<Button onClick={() => void query.refetch()}>Thử lại</Button>} />
      : query.isLoading ? <div className={styles.loading} aria-label="Đang tải cài đặt nền tảng"><Skeleton active title paragraph={{ rows: 6 }} /></div>
        : <AdminSettingsForm
          settings={settings}
          loading={false}
          submitting={updateMutation.isPending}
          onUpdate={async data => {
            try {
              await updateMutation.mutateAsync(data);
              message.success('Cập nhật cài đặt sàn thành công');
            } catch (error) {
              message.error(parseApiError(error).message || 'Cập nhật thất bại');
            }
          }}
        />}
  </main>;
};
