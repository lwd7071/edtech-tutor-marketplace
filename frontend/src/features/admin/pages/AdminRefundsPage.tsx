'use client';

import React, { useState } from 'react';
import { message } from 'antd';
import { useAdminRefunds, useApproveRefund, useCompleteRefund, useRejectRefund } from '../hooks/useAdminFinance';
import { AdminRefundTable } from '../components/AdminRefundTable';
import styles from './AdminRefundsPage.module.css';

export const AdminRefundsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const query = useAdminRefunds(statusFilter, page, pageSize);
  const approveMutation = useApproveRefund();
  const completeMutation = useCompleteRefund();
  const rejectMutation = useRejectRefund();
  const refunds = query.data?.data || [];
  const total = query.data?.meta?.totalElements ?? refunds.length;

  return <div className={styles.page}>
    <header className={styles.heading}>
      <h1>Yêu cầu hoàn tiền</h1>
      <p>Thẩm định yêu cầu, xem thông tin nhận tiền và ghi nhận kết quả chuyển khoản.</p>
    </header>
    <AdminRefundTable
      refunds={refunds}
      loading={query.isLoading}
      error={query.isError}
      total={total}
      page={page}
      pageSize={pageSize}
      onRetry={() => void query.refetch()}
      onPageChange={(nextPage, nextSize) => { setPage(nextPage); setPageSize(nextSize); }}
      onFilterStatus={nextStatus => { setStatusFilter(nextStatus); setPage(0); }}
      onApproveRefund={async (id, data) => {
        await approveMutation.mutateAsync({ id, data });
        message.success('Đã duyệt yêu cầu hoàn tiền');
      }}
      onCompleteRefund={async (id, data) => {
        await completeMutation.mutateAsync({ id, data });
        message.success('Xác nhận hoàn tiền thành công');
      }}
      onRejectRefund={async (id, data) => {
        await rejectMutation.mutateAsync({ id, data });
        message.success('Đã từ chối yêu cầu hoàn tiền');
      }}
    />
  </div>;
};
