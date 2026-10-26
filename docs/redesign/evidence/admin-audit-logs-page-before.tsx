'use client';

import React, { useState } from 'react';
import { Typography } from 'antd';
import { useSearchParams } from 'next/navigation';
import { useAdminAuditLogs } from '../hooks/useAdminFinance';
import { AdminAuditLogTable } from '../components/AdminAuditLogTable';

export const AdminAuditLogsPage: React.FC = () => {
  const searchParams = useSearchParams();
  const targetType = searchParams.get('targetType') ?? undefined;
  const targetId = searchParams.get('targetId') ?? undefined;
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data: logsRes, isLoading } = useAdminAuditLogs(undefined, undefined, targetType, targetId, page, pageSize);

  const logs = logsRes?.data || [];
  const total = logsRes?.meta?.totalElements || logs.length;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Nhật ký Kiểm toán (Audit Logs)
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Theo dõi toàn bộ lịch sử thao tác nhạy cảm, thay đổi dữ liệu và trạng thái trong hệ thống
        </Typography.Text>
        {targetId && <Typography.Paragraph type="secondary">Đang lọc theo người dùng {targetId}</Typography.Paragraph>}
      </div>

      <AdminAuditLogTable
        logs={logs}
        loading={isLoading}
        total={total}
        page={page}
        pageSize={pageSize}
        onPageChange={(p, s) => {
          setPage(p);
          setPageSize(s);
        }}
      />
    </div>
  );
};
