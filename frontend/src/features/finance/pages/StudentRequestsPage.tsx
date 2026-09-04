'use client';

import React from 'react';
import { Typography } from 'antd';
import { useStudentRefunds, useStudentExtensions } from '../hooks/useFinance';
import { StudentRequestsTable } from '../components/StudentRequestsTable';

export const StudentRequestsPage: React.FC = () => {
  const { data: refundsRes, isLoading: refundsLoading } = useStudentRefunds(0, 50);
  const { data: extensionsRes, isLoading: extensionsLoading } = useStudentExtensions(0, 50);

  const refunds = refundsRes?.data || [];
  const extensions = extensionsRes?.data || [];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Yêu cầu của tôi
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Theo dõi tiến độ xử lý các yêu cầu hoàn tiền và gia hạn gói học
        </Typography.Text>
      </div>

      <StudentRequestsTable
        refunds={refunds}
        extensions={extensions}
        loadingRefunds={refundsLoading}
        loadingExtensions={extensionsLoading}
      />
    </div>
  );
};
