'use client';

import React, { useState } from 'react';
import { message } from 'antd';
import { useAdminPayouts, useProcessPayout, useCompletePayout, useRejectPayout } from '../hooks/useAdminFinance';
import { AdminPayoutTable } from '../components/AdminPayoutTable';
import styles from './AdminPayoutsPage.module.css';

export const AdminPayoutsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const query = useAdminPayouts(statusFilter, page, pageSize);
  const processMutation = useProcessPayout();
  const completeMutation = useCompletePayout();
  const rejectMutation = useRejectPayout();
  const payouts = query.data?.data || [];
  const total = query.data?.meta?.totalElements ?? payouts.length;

  return <div className={styles.page}>
    <header className={styles.heading}>
      <h1>Yêu cầu rút tiền</h1>
      <p>Thẩm định số tiền, xử lý lệnh và ghi nhận chứng từ chuyển khoản.</p>
    </header>
    <AdminPayoutTable
      payouts={payouts}
      loading={query.isLoading}
      error={query.isError}
      total={total}
      page={page}
      pageSize={pageSize}
      onRetry={() => void query.refetch()}
      onPageChange={(nextPage, nextSize) => { setPage(nextPage); setPageSize(nextSize); }}
      onFilterStatus={nextStatus => { setStatusFilter(nextStatus); setPage(0); }}
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
  </div>;
};
