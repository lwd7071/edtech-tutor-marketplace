'use client';

import React, { useState } from 'react';
import { message } from 'antd';
import {
  useAdminExtensions,
  useApproveExtension,
  useRejectExtension,
} from '../hooks/useAdminFinance';
import { AdminExtensionTable } from '../components/AdminExtensionTable';
import styles from './AdminExtensionsPage.module.css';

export const AdminExtensionsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const query = useAdminExtensions(statusFilter, page, pageSize);
  const approveMutation = useApproveExtension();
  const rejectMutation = useRejectExtension();
  const extensions = query.data?.data || [];
  const total = query.data?.meta?.totalElements ?? extensions.length;

  return <div className={styles.page}>
    <header className={styles.heading}>
      <h1>Yêu cầu gia hạn</h1>
      <p>Xem lý do và thời hạn đề xuất trước khi quyết định từng yêu cầu.</p>
    </header>
    <AdminExtensionTable
      extensions={extensions}
      loading={query.isLoading}
      error={query.isError}
      total={total}
      page={page}
      pageSize={pageSize}
      onRetry={() => void query.refetch()}
      onPageChange={(nextPage, nextSize) => { setPage(nextPage); setPageSize(nextSize); }}
      onFilterStatus={nextStatus => { setStatusFilter(nextStatus); setPage(0); }}
      onApproveExtension={async (id, data) => {
        await approveMutation.mutateAsync({ id, data });
        message.success('Đã duyệt gia hạn và kích hoạt lại gói học');
      }}
      onRejectExtension={async (id, data) => {
        await rejectMutation.mutateAsync({ id, data });
        message.success('Đã từ chối yêu cầu gia hạn');
      }}
    />
  </div>;
};
