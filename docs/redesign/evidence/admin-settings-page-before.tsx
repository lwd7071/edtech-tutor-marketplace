'use client';

import React from 'react';
import { Typography, message } from 'antd';
import { usePlatformSettings, useUpdatePlatformSettings } from '../hooks/useAdminFinance';
import { AdminSettingsForm } from '../components/AdminSettingsForm';

export const AdminSettingsPage: React.FC = () => {
  const { data: settingsRes, isLoading } = usePlatformSettings();
  const updateMutation = useUpdatePlatformSettings();

  const settings = settingsRes?.data;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Cài đặt Sàn & Tham số Hệ thống
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Quản lý tỉ lệ hoa hồng, thuật toán xếp hạng uy tín và chu kỳ tự động của hệ thống
        </Typography.Text>
      </div>

      <AdminSettingsForm
        settings={settings}
        loading={isLoading}
        submitting={updateMutation.isPending}
        onUpdate={async (data) => {
          try {
            await updateMutation.mutateAsync(data);
            message.success('Cập nhật cài đặt sàn thành công');
          } catch (err: any) {
            message.error(err?.response?.data?.message || 'Cập nhật thất bại');
          }
        }}
      />
    </div>
  );
};
