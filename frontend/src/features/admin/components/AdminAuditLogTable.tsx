'use client';

import React, { useState } from 'react';
import { Button, Empty, Modal, Pagination, Skeleton, Tag, Tooltip, Typography } from 'antd';
import { AuditAction, AuditLogView } from '../types';
import { formatLedgerTime } from '@/features/finance';
import styles from './AdminAuditLogTable.module.css';

interface AdminAuditLogTableProps {
  logs?: AuditLogView[];
  loading?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, size: number) => void;
}

export const getAuditActionTag = (action: AuditAction) => {
  switch (action) {
    case 'USER_LOCKED': return <Tag color="error">{action}</Tag>;
    case 'USER_UNLOCKED': return <Tag color="warning">{action}</Tag>;
    default: return <Tag>{action}</Tag>;
  }
};

function AuditLoading() {
  return <div className={styles.loading} aria-label="Đang tải nhật ký hoạt động">{[0, 1, 2].map(item => <div key={item} className={styles.loadingRow}><Skeleton active title={{ width: '30%' }} paragraph={{ rows: 2 }} /></div>)}</div>;
}

const shortId = (id?: string | null) => id ? `${id.slice(0, 8)}…${id.slice(-4)}` : '—';
const jsonData = (value?: Record<string, unknown> | null) => JSON.stringify(value || {}, null, 2);

export const AdminAuditLogTable: React.FC<AdminAuditLogTableProps> = ({ logs = [], loading = false, total = 0, page = 0, pageSize = 20, onPageChange }) => {
  const [selectedLog, setSelectedLog] = useState<AuditLogView | null>(null);

  return <section className={styles.table} aria-label="Nhật ký hoạt động">
    {loading ? <AuditLoading /> : logs.length === 0 ? <div className={styles.empty}><Empty description="Chưa có nhật ký hoạt động." /></div> : <>
      <div className={styles.listHeader} aria-hidden="true"><span>Thời gian</span><span>Người thực hiện / Hành động</span><span>Đối tượng</span><span>Mạng</span><span>Chi tiết</span></div>
      <ul className={styles.rows}>
        {logs.map(log => <li key={log.id}>
          <article className={styles.row}>
            <div className={styles.time}><strong>{formatLedgerTime(log.createdAt)}</strong><span>Mã log · {shortId(log.id)}</span></div>
            <div className={styles.actor}><Typography.Text copyable={{ text: log.actorId }} className={styles.actorId}>{shortId(log.actorId)}</Typography.Text>{getAuditActionTag(log.action)}</div>
            <div className={styles.target}><strong>{log.targetType}</strong><Tooltip title={log.targetId}><span>{shortId(log.targetId)}</span></Tooltip></div>
            <div className={styles.network}><span>{log.ipAddress || 'IP không có'}</span>{log.userAgent && <Tooltip title={log.userAgent}><small>{log.userAgent}</small></Tooltip>}</div>
            <div className={styles.actions}><Button className={styles.detailButton} onClick={() => setSelectedLog(log)}>Chi tiết</Button></div>
          </article>
        </li>)}
      </ul>
      <Pagination className={styles.pagination} current={page + 1} pageSize={pageSize} total={total} showSizeChanger pageSizeOptions={[10, 20, 50]} showTotal={count => `${count} bản ghi`} onChange={(nextPage, nextSize) => onPageChange?.(nextPage - 1, nextSize)} />
    </>}

    <Modal className={styles.detailModal} open={!!selectedLog} title={`Chi tiết nhật ký #${selectedLog ? shortId(selectedLog.id) : ''}`} onCancel={() => setSelectedLog(null)} footer={<Button className={styles.modalClose} onClick={() => setSelectedLog(null)}>Đóng</Button>} destroyOnHidden>
      {selectedLog && <div className={styles.detail}>
        <dl className={styles.metadata}>
          <div><dt>Thời gian</dt><dd>{formatLedgerTime(selectedLog.createdAt)}</dd></div>
          <div><dt>Hành động</dt><dd>{getAuditActionTag(selectedLog.action)}</dd></div>
          <div><dt>Người thực hiện</dt><dd><Typography.Text copyable={{ text: selectedLog.actorId }}>{selectedLog.actorId}</Typography.Text></dd></div>
          <div><dt>Đối tượng</dt><dd>{selectedLog.targetType} · <Typography.Text copyable={{ text: selectedLog.targetId }}>{selectedLog.targetId}</Typography.Text></dd></div>
          <div><dt>Địa chỉ IP</dt><dd>{selectedLog.ipAddress || '—'}</dd></div>
          {selectedLog.userAgent && <div className={styles.metadataWide}><dt>Trình duyệt</dt><dd>{selectedLog.userAgent}</dd></div>}
        </dl>
        <section className={styles.jsonSection} aria-labelledby="audit-before-title"><h3 id="audit-before-title">Dữ liệu trước thay đổi</h3><pre>{jsonData(selectedLog.beforeData)}</pre></section>
        <section className={styles.jsonSection} aria-labelledby="audit-after-title"><h3 id="audit-after-title">Dữ liệu sau thay đổi</h3><pre>{jsonData(selectedLog.afterData)}</pre></section>
      </div>}
    </Modal>
  </section>;
};
