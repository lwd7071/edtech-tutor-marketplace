'use client';

import React from 'react';
import { Typography, Space } from 'antd';
import { TeacherApprovalTable } from '../components/TeacherApprovalTable';

export function AdminTeachersPage() {
  return (
    <div style={{ padding: 'var(--space-6)', maxWidth: 1200, margin: '0 auto' }}>
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Typography.Title level={2} style={{ marginBottom: 4 }}>
            Xét duyệt hồ sơ gia sư
          </Typography.Title>
          <Typography.Paragraph type="secondary">
            Xem hồ sơ, kiểm tra văn bằng và phê duyệt hoặc từ chối gia sư đăng ký giảng dạy.
          </Typography.Paragraph>
        </div>
        <TeacherApprovalTable />
      </Space>
    </div>
  );
}
