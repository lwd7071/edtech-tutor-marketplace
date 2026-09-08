'use client';

import React from 'react';
import { Typography, Space } from 'antd';
import { SubjectProposalTable } from '../components/SubjectProposalTable';

export function AdminSubjectsPage() {
  return (
    <div style={{ padding: 'var(--space-6)', maxWidth: 1200, margin: '0 auto' }}>
      <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Typography.Title level={2} style={{ marginBottom: 4 }}>
            Quản trị Đề xuất Môn học
          </Typography.Title>
          <Typography.Paragraph type="secondary">
            Xem các yêu cầu mở môn học mới từ gia sư, chuẩn hóa và đưa vào danh mục chính thức.
          </Typography.Paragraph>
        </div>
        <SubjectProposalTable />
      </Space>
    </div>
  );
}
