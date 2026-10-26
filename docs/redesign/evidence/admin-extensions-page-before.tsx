'use client';

import React, { useState } from 'react';
import { Typography, message } from 'antd';
import {
  useAdminExtensions,
  useApproveExtension,
  useRejectExtension,
} from '../hooks/useAdminFinance';
import { AdminExtensionTable } from '../components/AdminExtensionTable';

export const AdminExtensionsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data: extensionsRes, isLoading } = useAdminExtensions(statusFilter, page, pageSize);
  const approveMutation = useApproveExtension();
  const rejectMutation = useRejectExtension();

  const extensions = extensionsRes?.data || [];
  const total = extensionsRes?.meta?.totalElements || extensions.length;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Hàng đợi Gia hạn Gói học
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Xét duyệt các yêu cầu xin gia hạn hạn sử dụng gói học đã hết hạn
        </Typography.Text>
      </div>

      <AdminExtensionTable
        extensions={extensions}
        loading={isLoading}
        total={total}
        page={page}
        pageSize={pageSize}
        onPageChange={(p, s) => {
          setPage(p);
          setPageSize(s);
        }}
        onFilterStatus={(st) => {
          setStatusFilter(st);
          setPage(0);
        }}
        onApproveExtension={async (id, data) => {
          await approveMutation.mutateAsync({ id, data });
          message.success('Đã duyệt gia hạn và kích hoạt lại gói học');
        }}
        onRejectExtension={async (id, data) => {
          await rejectMutation.mutateAsync({ id, data });
          message.success('Đã từ chối yêu cầu gia hạn');
        }}
      />
    </div>
  );
};
