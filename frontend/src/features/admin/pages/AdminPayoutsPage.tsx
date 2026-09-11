'use client';

import React, { useState } from 'react';
import { Typography, message } from 'antd';
import {
  useAdminPayouts,
  useProcessPayout,
  useCompletePayout,
  useRejectPayout,
} from '../hooks/useAdminFinance';
import { AdminPayoutTable } from '../components/AdminPayoutTable';

export const AdminPayoutsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const { data: payoutsRes, isLoading } = useAdminPayouts(statusFilter, page, pageSize);
  const processMutation = useProcessPayout();
  const completeMutation = useCompletePayout();
  const rejectMutation = useRejectPayout();

  const payouts = payoutsRes?.data || [];
  const total = payoutsRes?.meta?.totalElements || payouts.length;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Yêu cầu rút tiền của gia sư
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Thẩm định, xử lý và xác nhận chuyển khoản cho các yêu cầu rút tiền
        </Typography.Text>
      </div>

      <AdminPayoutTable
        payouts={payouts}
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
        onProcessPayout={async (id, data) => {
          await processMutation.mutateAsync({ id, data });
          message.success('Đã chuyển lệnh rút sang trạng thái đang xử lý');
        }}
        onCompletePayout={async (id, data) => {
          await completeMutation.mutateAsync({ id, data });
          message.success('Xác nhận chuyển khoản thành công');
        }}
        onRejectPayout={async (id, data) => {
          await rejectMutation.mutateAsync({ id, data });
          message.success('Đã từ chối lệnh rút tiền');
        }}
      />
    </div>
  );
};
