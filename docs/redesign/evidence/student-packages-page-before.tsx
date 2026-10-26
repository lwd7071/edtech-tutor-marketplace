'use client';

import React from 'react';
import { Typography, Space } from 'antd';
import { StudentPackageList } from '../components/StudentPackageList';

export const StudentPackagesPage: React.FC = () => {
  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6, 24px) var(--space-4, 16px)' }}>
      <Space orientation="vertical" size="small" style={{ width: '100%', marginBottom: 24 }}>
        <Typography.Title level={2} style={{ margin: 0 }}>
          Gói học của tôi
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ margin: 0 }}>
          Theo dõi tiến độ, số buổi còn lại và quản lý các gói học gia sư bạn đã đăng ký.
        </Typography.Paragraph>
      </Space>

      <StudentPackageList />
    </div>
  );
};
