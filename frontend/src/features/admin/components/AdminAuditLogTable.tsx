'use client';

import React, { useState } from 'react';
import { Table, Tag, Typography, Button, Modal } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EyeOutlined } from '@ant-design/icons';
import { AuditLogView, AuditAction } from '../types';
import { formatLedgerTime } from '@/features/finance';

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
    case 'CREATE':
      return <Tag color="success">CREATE</Tag>;
    case 'UPDATE':
      return <Tag color="processing">UPDATE</Tag>;
    case 'DELETE':
      return <Tag color="error">DELETE</Tag>;
    case 'APPROVE':
      return <Tag color="success">APPROVE</Tag>;
    case 'REJECT':
      return <Tag color="default">REJECT</Tag>;
    case 'LOCK':
      return <Tag color="error">LOCK</Tag>;
    case 'UNLOCK':
      return <Tag color="warning">UNLOCK</Tag>;
    default:
      return <Tag>{action}</Tag>;
  }
};

export const AdminAuditLogTable: React.FC<AdminAuditLogTableProps> = ({
  logs = [],
  loading = false,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
}) => {
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [selectedLog, setSelectedLog] = useState<AuditLogView | null>(null);

  const handleViewDetail = (log: AuditLogView) => {
    setSelectedLog(log);
    setDetailModalOpen(true);
  };

  const columns: ColumnsType<AuditLogView> = [
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (val: string) => (
        <Typography.Text style={{ fontSize: 13, color: 'var(--color-text-secondary, #57534E)' }}>
          {formatLedgerTime(val)}
        </Typography.Text>
      ),
    },
    {
      title: 'Hành động',
      dataIndex: 'action',
      key: 'action',
      width: 110,
      render: (action: AuditAction) => getAuditActionTag(action),
    },
    {
      title: 'Đối tượng tác động',
      key: 'target',
      width: 180,
      render: (_, record) => (
        <div>
          <Typography.Text strong style={{ fontSize: 13 }}>
            {record.targetType}
          </Typography.Text>
          <div style={{ fontSize: 12, fontFamily: 'var(--font-mono)', color: 'var(--color-text-tertiary)' }}>
            #{record.targetId?.slice(0, 8)}
          </div>
        </div>
      ),
    },
    {
      title: 'Người thực hiện (Actor ID)',
      dataIndex: 'actorId',
      key: 'actorId',
      width: 140,
      render: (id: string) => (
        <Typography.Text copyable={{ text: id }} style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
          #{id?.slice(0, 8)}
        </Typography.Text>
      ),
    },
    {
      title: 'IP Address',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 130,
      render: (ip: string) => (
        <Typography.Text style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
          {ip || '—'}
        </Typography.Text>
      ),
    },
    {
      title: 'Chi tiết thay đổi',
      key: 'detail',
      width: 120,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record)}
        >
          Xem JSON
        </Button>
      ),
    },
  ];

  return (
    <div style={{ background: 'var(--color-surface, #FFFFFF)', borderRadius: 'var(--radius-lg, 12px)', padding: 20 }}>
      <Table<AuditLogView>
        rowKey="id"
        columns={columns}
        dataSource={logs}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showSizeChanger: true,
          onChange: (p, s) => onPageChange?.(p - 1, s),
        }}
        scroll={{ x: 850 }}
      />

      <Modal
        open={detailModalOpen}
        title={`Chi tiết Audit Log #${selectedLog?.id.slice(0, 8)}`}
        footer={null}
        onCancel={() => setDetailModalOpen(false)}
        width={650}
      >
        <div style={{ marginTop: 16 }}>
          <Typography.Title level={5}>Dữ liệu trước thay đổi (Before):</Typography.Title>
          <pre
            style={{
              background: 'var(--color-surface-sunken, #F5F3EF)',
              padding: 12,
              borderRadius: 8,
              fontSize: 12,
              maxHeight: 180,
              overflow: 'auto',
            }}
          >
            {JSON.stringify(selectedLog?.beforeData || {}, null, 2)}
          </pre>

          <Typography.Title level={5} style={{ marginTop: 16 }}>
            Dữ liệu sau thay đổi (After):
          </Typography.Title>
          <pre
            style={{
              background: 'var(--color-surface-sunken, #F5F3EF)',
              padding: 12,
              borderRadius: 8,
              fontSize: 12,
              maxHeight: 180,
              overflow: 'auto',
            }}
          >
            {JSON.stringify(selectedLog?.afterData || {}, null, 2)}
          </pre>
        </div>
      </Modal>
    </div>
  );
};
