'use client';

import React, { useState } from 'react';
import { Typography, message } from 'antd';
import {
  useAdminRefunds,
  useApproveRefund,
  useProcessRefund,
  useRejectRefund,
} from '../hooks/useAdminFinance';
import { AdminRefundTable } from '../components/AdminRefundTable';

export const AdminRefundsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data: refundsRes, isLoading } = useAdminRefunds(statusFilter, page, pageSize);
  const approveMutation = useApproveRefund();
  const processMutation = useProcessRefund();
  const rejectMutation = useRejectRefund();

  const refunds = refundsRes?.data || [];
  const total = refundsRes?.meta?.totalElements || refunds.length;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Hàng đợi Hoàn tiền Học viên
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Thẩm định, duyệt và xác nhận hoàn tiền cho học viên yêu cầu trả gói học
        </Typography.Text>
      </div>

      <AdminRefundTable
        refunds={refunds}
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
        onApproveRefund={async (id) => {
          await approveMutation.mutateAsync(id);
          message.success('Đã duyệt yêu cầu hoàn tiền');
        }}
        onProcessRefund={async (id, data) => {
          await processMutation.mutateAsync({ id, data });
          message.success('Xác nhận hoàn tiền thành công');
        }}
        onRejectRefund={async (id, data) => {
          await rejectMutation.mutateAsync({ id, data });
          message.success('Đã từ chối yêu cầu hoàn tiền');
        }}
      />
    </div>
  );
};
