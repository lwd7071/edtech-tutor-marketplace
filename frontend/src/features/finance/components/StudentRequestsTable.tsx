'use client';

import React from 'react';
import { Table, Tag, Typography, Tabs, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { RefundRequestView, ExtensionRequestView, RefundStatus, ExtensionStatus } from '../types';
import { formatLedgerTime } from './LedgerTable';

interface StudentRequestsTableProps {
  refunds?: RefundRequestView[];
  extensions?: ExtensionRequestView[];
  loadingRefunds?: boolean;
  loadingExtensions?: boolean;
}

export const getRefundStatusTag = (status: RefundStatus) => {
  switch (status) {
    case 'PENDING':
      return <Tag color="warning">Chờ duyệt</Tag>;
    case 'APPROVED':
      return <Tag color="processing">Đã duyệt</Tag>;
    case 'PROCESSING':
      return <Tag color="processing">Đang chuyển khoản</Tag>;
    case 'REFUNDED':
      return <Tag color="success">Đã hoàn tiền</Tag>;
    case 'REJECTED':
      return <Tag color="default">Bị từ chối</Tag>;
    case 'FAILED':
      return <Tag color="error">Thất bại</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
};

export const getExtensionStatusTag = (status: ExtensionStatus) => {
  switch (status) {
    case 'PENDING':
      return <Tag color="warning">Chờ duyệt</Tag>;
    case 'APPROVED':
      return <Tag color="success">Đã gia hạn</Tag>;
    case 'REJECTED':
      return <Tag color="default">Bị từ chối</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
};

export const StudentRequestsTable: React.FC<StudentRequestsTableProps> = ({
  refunds = [],
  extensions = [],
  loadingRefunds = false,
  loadingExtensions = false,
}) => {
  const refundColumns: ColumnsType<RefundRequestView> = [
    {
      title: 'Mã yêu cầu',
      dataIndex: 'id',
      key: 'id',
      width: 130,
      render: (id: string) => (
        <Typography.Text copyable={{ text: id }} style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
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
      title: 'Số buổi hoàn',
      key: 'sessions',
      width: 140,
      render: (_, record) => (
        <span>
          <strong>{record.requestedSessions}</strong> buổi
          {record.approvedSessions && record.approvedSessions !== record.requestedSessions && (
            <span style={{ color: 'var(--color-success-600)', marginLeft: 4 }}>
              (duyệt {record.approvedSessions})
            </span>
          )}
        </span>
      ),
    },
    {
      title: 'Số tiền hoàn',
      dataIndex: 'refundAmountVnd',
      key: 'refundAmountVnd',
      width: 150,
      align: 'right',
      render: (amt: number | null) => (
        <span
          style={{
            fontWeight: 600,
            fontSize: 14,
            fontVariantNumeric: 'tabular-nums',
            color: 'var(--color-primary-600, #0F766E)',
          }}
        >
          {amt ? `${amt.toLocaleString('vi-VN')} ₫` : '—'}
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (status: RefundStatus) => getRefundStatusTag(status),
    },
    {
      title: 'Tài khoản nhận',
      key: 'bank',
      width: 200,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography.Text style={{ fontSize: 13, fontWeight: 500 }}>
            {record.bankName || 'Ngân hàng'}
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12, fontFamily: 'var(--font-mono)' }}>
            {record.accountNumberMasked} ({record.accountHolderName})
          </Typography.Text>
        </div>
      ),
    },
    {
      title: 'Ghi chú / Phản hồi',
      key: 'note',
      render: (_, record) => {
        const note = record.adminNote || record.reason;
        if (!note) return <Typography.Text type="secondary">—</Typography.Text>;
        return (
          <Tooltip title={note}>
            <Typography.Text
              type={record.status === 'REJECTED' ? 'danger' : 'secondary'}
              style={{ fontSize: 13 }}
            >
              {note}
            </Typography.Text>
          </Tooltip>
        );
      },
    },
  ];

  const extensionColumns: ColumnsType<ExtensionRequestView> = [
    {
      title: 'Mã yêu cầu',
      dataIndex: 'id',
      key: 'id',
      width: 130,
      render: (id: string) => (
        <Typography.Text copyable={{ text: id }} style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
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
      title: 'Hạn xin gia hạn',
      dataIndex: 'requestedExpiryDate',
      key: 'requestedExpiryDate',
      width: 160,
      render: (val: string) => (
        <Typography.Text strong style={{ fontSize: 13 }}>
          {formatLedgerTime(val)}
        </Typography.Text>
      ),
    },
    {
      title: 'Hạn được duyệt',
      dataIndex: 'approvedExpiryDate',
      key: 'approvedExpiryDate',
      width: 160,
      render: (val: string | null) => (
        <Typography.Text
          style={{
            fontSize: 13,
            color: val ? 'var(--color-success-600, #15803D)' : undefined,
            fontWeight: val ? 600 : undefined,
          }}
        >
          {val ? formatLedgerTime(val) : '—'}
        </Typography.Text>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: ExtensionStatus) => getExtensionStatusTag(status),
    },
    {
      title: 'Lý do / Phản hồi Admin',
      key: 'reason',
      render: (_, record) => {
        const text = record.adminNote || record.reason;
        return (
          <Tooltip title={text}>
            <Typography.Text
              type={record.status === 'REJECTED' ? 'danger' : 'secondary'}
              style={{ fontSize: 13 }}
            >
              {text || '—'}
            </Typography.Text>
          </Tooltip>
        );
      },
    },
  ];

  return (
    <div
      style={{
        background: 'var(--color-surface, #FFFFFF)',
        borderRadius: 'var(--radius-lg, 12px)',
        padding: 20,
      }}
    >
      <Tabs
        items={[
          {
            key: 'refunds',
            label: `Yêu cầu hoàn tiền (${refunds.length})`,
            children: (
              <Table<RefundRequestView>
                rowKey="id"
                columns={refundColumns}
                dataSource={refunds}
                loading={loadingRefunds}
                pagination={{ pageSize: 10 }}
                scroll={{ x: 850 }}
              />
            ),
          },
          {
            key: 'extensions',
            label: `Yêu cầu gia hạn (${extensions.length})`,
            children: (
              <Table<ExtensionRequestView>
                rowKey="id"
                columns={extensionColumns}
                dataSource={extensions}
                loading={loadingExtensions}
                pagination={{ pageSize: 10 }}
                scroll={{ x: 800 }}
              />
            ),
          },
        ]}
      />
    </div>
  );
};
