'use client';

import React from 'react';
import { Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { LedgerEntryView, LedgerEntryType, BalanceBucket } from '../types';

interface LedgerTableProps {
  entries?: LedgerEntryView[];
  loading?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, pageSize: number) => void;
}

export const getEntryTypeLabel = (type: LedgerEntryType): string => {
  switch (type) {
    case 'SETTLEMENT_CREDIT_PENDING':
      return 'Nhận tiền gói học';
    case 'SETTLEMENT_RELEASE_AVAILABLE':
      return 'Quyết toán buổi học';
    case 'REFUND_DEBIT_PENDING':
      return 'Khấu trừ hoàn tiền';
    case 'PAYOUT_RESERVED':
      return 'Tạo lệnh rút tiền';
    case 'PAYOUT_RELEASED':
      return 'Hoàn trả lệnh rút';
    case 'PAYOUT_SUCCEEDED':
      return 'Rút tiền thành công';
    case 'ADJUSTMENT':
      return 'Điều chỉnh số dư';
    default:
      return type;
  }
};

export const getBucketTag = (bucket: BalanceBucket) => {
  switch (bucket) {
    case 'AVAILABLE':
      return <Tag color="success">Khả dụng</Tag>;
    case 'PENDING':
      return <Tag color="warning">Chờ quyết toán</Tag>;
    case 'RESERVED':
      return <Tag color="default">Đang rút</Tag>;
    default:
      return <Tag>{bucket}</Tag>;
  }
};

export const formatLedgerTime = (isoString: string): string => {
  try {
    const date = new Date(isoString);
    return date.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Asia/Ho_Chi_Minh',
    });
  } catch {
    return isoString;
  }
};

export const LedgerTable: React.FC<LedgerTableProps> = ({
  entries = [],
  loading = false,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
}) => {
  const columns: ColumnsType<LedgerEntryView> = [
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
      title: 'Loại giao dịch',
      dataIndex: 'entryType',
      key: 'entryType',
      width: 190,
      render: (type: LedgerEntryType) => (
        <Typography.Text strong style={{ fontSize: 14 }}>
          {getEntryTypeLabel(type)}
        </Typography.Text>
      ),
    },
    {
      title: 'Khoản mục',
      dataIndex: 'balanceBucket',
      key: 'balanceBucket',
      width: 140,
      render: (bucket: BalanceBucket) => getBucketTag(bucket),
    },
    {
      title: 'Biến động',
      key: 'amount',
      width: 160,
      align: 'right',
      render: (_, record: LedgerEntryView) => {
        const isCredit = record.direction === 'CREDIT';
        const prefix = isCredit ? '+' : '−';
        const color = isCredit ? 'var(--color-success-600, #15803D)' : 'var(--color-error-600, #B91C1C)';

        return (
          <span
            style={{
              fontWeight: 600,
              fontSize: 14,
              color,
              fontVariantNumeric: 'tabular-nums',
              whiteSpace: 'nowrap',
            }}
          >
            {prefix}
            {record.amountVnd.toLocaleString('vi-VN')} ₫
          </span>
        );
      },
    },
    {
      title: 'Mô tả / Diễn giải',
      dataIndex: 'description',
      key: 'description',
      render: (desc: string) => (
        <Typography.Text type="secondary" style={{ fontSize: 13 }}>
          {desc || '—'}
        </Typography.Text>
      ),
    },
  ];

  return (
    <Table<LedgerEntryView>
      rowKey="id"
      columns={columns}
      dataSource={entries}
      loading={loading}
      pagination={{
        current: page + 1,
        pageSize,
        total,
        showSizeChanger: true,
        onChange: (p, s) => onPageChange?.(p - 1, s),
      }}
      scroll={{ x: 750 }}
      style={{
        background: 'var(--color-surface, #FFFFFF)',
        borderRadius: 'var(--radius-lg, 12px)',
      }}
    />
  );
};
