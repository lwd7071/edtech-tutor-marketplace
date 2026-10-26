'use client';

import React, { useState } from 'react';
import { Alert, Button } from 'antd';
import { useSearchParams } from 'next/navigation';
import styles from './AdminAuditLogsPage.module.css';
import { useAdminAuditLogs } from '../hooks/useAdminFinance';
import { AdminAuditLogTable } from '../components/AdminAuditLogTable';

export const AdminAuditLogsPage: React.FC = () => {
  const searchParams = useSearchParams();
  const targetType = searchParams.get('targetType') ?? undefined;
  const targetId = searchParams.get('targetId') ?? undefined;
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const query = useAdminAuditLogs(undefined, undefined, targetType, targetId, page, pageSize);
  const logs = query.data?.data ?? [];
  const total = query.data?.meta?.totalElements ?? logs.length;
  const targetLabel = targetType === 'USER' ? 'người dùng' : targetType === 'TEACHER' ? 'gia sư' : targetType === 'BOOKING' ? 'buổi học' : targetType;

  return <main className={styles.page}>
    <header className={styles.heading}>
      <h1>Nhật ký hoạt động</h1>
      <p>Lịch sử các thao tác quản trị và thay đổi dữ liệu quan trọng trong hệ thống.</p>
    </header>
    {targetId && <div className={styles.context} role="status">
      Đang lọc theo {targetLabel ? `${targetLabel} · ` : 'đối tượng · '}<code>{targetId}</code>
    </div>}
    {query.isError ? <Alert className={styles.error} type="error" showIcon message="Không tải được nhật ký hoạt động." description="Kiểm tra kết nối rồi thử lại." action={<Button onClick={() => void query.refetch()}>Thử lại</Button>} />
      : <AdminAuditLogTable
        logs={logs}
        loading={query.isLoading}
        total={total}
        page={page}
        pageSize={pageSize}
        onPageChange={(nextPage, nextSize) => { setPage(nextPage); setPageSize(nextSize); }}
      />}
  </main>;
};
