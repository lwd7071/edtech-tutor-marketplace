'use client';

import React from 'react';
import { Table, Tag, Typography, Button, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EyeOutlined } from '@ant-design/icons';
import { PayoutRequestView, PayoutStatus } from '../types';
import { formatLedgerTime } from './LedgerTable';

interface PayoutListTableProps {
  payouts?: PayoutRequestView[];
  loading?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, size: number) => void;
}

export const getPayoutStatusTag = (status: PayoutStatus) => {
  switch (status) {
    case 'PENDING':
      return <Tag color="warning">Chờ duyệt</Tag>;
    case 'PROCESSING':
      return <Tag color="processing">Đang xử lý</Tag>;
    case 'SUCCEEDED':
      return <Tag color="success">Đã chuyển</Tag>;
    case 'REJECTED':
      return <Tag color="default">Bị từ chối</Tag>;
    case 'FAILED':
      return <Tag color="error">Thất bại</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
};

export const PayoutListTable: React.FC<PayoutListTableProps> = ({
  payouts = [],
  loading = false,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
}) => {
  const columns: ColumnsType<PayoutRequestView> = [
    {
      title: 'Mã yêu cầu',
      dataIndex: 'id',
      key: 'id',
      width: 130,
      render: (id: string) => (
        <Typography.Text
          copyable={{ text: id }}
          style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}
        >
          #{id.slice(0, 8)}
        </Typography.Text>
      ),
    },
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (val: string) => (
        <Typography.Text style={{ fontSize: 13, color: 'var(--color-text-secondary, #57534E)' }}>
          {formatLedgerTime(val)}
        </Typography.Text>
      ),
    },
    {
      title: 'Số tiền rút',
      dataIndex: 'amountVnd',
      key: 'amountVnd',
      width: 150,
      align: 'right',
      render: (amt: number) => (
        <span
          style={{
            fontWeight: 700,
            fontSize: 15,
            color: 'var(--color-text-primary, #1C1917)',
            fontVariantNumeric: 'tabular-nums',
            whiteSpace: 'nowrap',
          }}
        >
          {amt.toLocaleString('vi-VN')} ₫
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: PayoutStatus) => getPayoutStatusTag(status),
    },
    {
      title: 'Chứng từ ngân hàng',
      key: 'proof',
      width: 170,
      render: (_, record: PayoutRequestView) => {
        if (record.bankReference || record.proofUrl) {
          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {record.bankReference && (
                <Typography.Text style={{ fontSize: 12, fontFamily: 'var(--font-mono)' }}>
                  Mã GD: {record.bankReference}
                </Typography.Text>
              )}
              {record.proofUrl && (
                <a href={record.proofUrl} target="_blank" rel="noopener noreferrer">
                  <Button type="link" size="small" icon={<EyeOutlined />} style={{ padding: 0 }}>
                    Xem biên lai
                  </Button>
                </a>
              )}
            </div>
          );
        }
        return <Typography.Text type="secondary">—</Typography.Text>;
      },
    },
    {
      title: 'Ghi chú / Lý do',
      key: 'notes',
      render: (_, record: PayoutRequestView) => {
        const note = record.adminNote || record.teacherNote;
        if (!note) return <Typography.Text type="secondary">—</Typography.Text>;
        return (
          <Tooltip title={note}>
            <Typography.Text
              type={record.adminNote ? (record.status === 'REJECTED' ? 'danger' : 'secondary') : 'secondary'}
              style={{ fontSize: 13 }}
            >
              {note}
            </Typography.Text>
          </Tooltip>
        );
      },
    },
  ];

  return (
    <Table<PayoutRequestView>
      rowKey="id"
      columns={columns}
      dataSource={payouts}
      loading={loading}
      pagination={{
        current: page + 1,
        pageSize,
        total,
        showSizeChanger: true,
        onChange: (p, s) => onPageChange?.(p - 1, s),
      }}
      scroll={{ x: 800 }}
      style={{
        background: 'var(--color-surface, #FFFFFF)',
        borderRadius: 'var(--radius-lg, 12px)',
      }}
    />
  );
};
