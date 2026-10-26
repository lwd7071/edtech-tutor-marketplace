'use client';

import { Alert, Button, Empty, Pagination, Skeleton, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { LedgerEntryView, LedgerEntryType, BalanceBucket } from '../types';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import styles from './FinanceActivityLists.module.css';

interface LedgerTableProps {
  entries?: LedgerEntryView[];
  loading?: boolean;
  error?: boolean;
  onRetry?: () => void;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, pageSize: number) => void;
}

export const getEntryTypeLabel = (type: LedgerEntryType): string => {
  switch (type) {
    case 'SETTLEMENT_CREDIT_PENDING': return 'Nhận tiền gói học';
    case 'SETTLEMENT_RELEASE_AVAILABLE': return 'Quyết toán buổi học';
    case 'REFUND_DEBIT_PENDING': return 'Khấu trừ hoàn tiền';
    case 'PAYOUT_RESERVED': return 'Tạo lệnh rút tiền';
    case 'PAYOUT_RELEASED': return 'Hoàn trả lệnh rút';
    case 'PAYOUT_SUCCEEDED': return 'Rút tiền thành công';
    case 'ADJUSTMENT': return 'Điều chỉnh số dư';
    default: return type;
  }
};

export const getBucketTag = (bucket: BalanceBucket) => {
  const label = bucket === 'AVAILABLE' ? 'Khả dụng'
    : bucket === 'PENDING' ? 'Chờ quyết toán'
      : bucket === 'RESERVED' ? 'Đang rút' : bucket;
  return <Tag className={styles.status}>{label}</Tag>;
};

export const formatLedgerTime = formatVietnamDateTime;

const formatAmount = (entry: LedgerEntryView) => `${entry.direction === 'CREDIT' ? '+' : '−'}${entry.amountVnd.toLocaleString('vi-VN')} ₫`;

export const LedgerTable = ({
  entries = [],
  loading = false,
  error = false,
  onRetry,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
}: LedgerTableProps) => {
  const columns: ColumnsType<LedgerEntryView> = [
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (value: string) => <time className={styles.date} dateTime={value}>{formatLedgerTime(value)}</time>,
    },
    {
      title: 'Loại giao dịch',
      dataIndex: 'entryType',
      key: 'entryType',
      width: 200,
      render: (type: LedgerEntryType, entry) => (
        <div className={styles.description}>
          <Typography.Text strong>{getEntryTypeLabel(type)}</Typography.Text>
          <Typography.Text type="secondary">{entry.description || '—'}</Typography.Text>
        </div>
      ),
    },
    {
      title: 'Khoản mục',
      dataIndex: 'balanceBucket',
      key: 'balanceBucket',
      width: 145,
      render: (bucket: BalanceBucket) => getBucketTag(bucket),
    },
    {
      title: 'Biến động',
      key: 'amount',
      width: 170,
      align: 'right',
      render: (_, entry) => (
        <span className={`${styles.amount} ${entry.direction === 'CREDIT' ? styles.credit : styles.debit}`}>
          {formatAmount(entry)}
        </span>
      ),
    },
  ];

  return (
    <section className={styles.section} aria-label="Sổ cái giao dịch">
      {error && (
        <Alert
          className={styles.error}
          type="error"
          showIcon
          title="Chưa tải được sổ cái"
          action={<Button onClick={onRetry}>Thử lại sổ cái</Button>}
        />
      )}
      {loading ? (
        <div className={styles.loading} role="status" aria-label="Đang tải sổ cái">
          {[0, 1, 2].map(row => <Skeleton.Input active block key={row} />)}
        </div>
      ) : !error && entries.length === 0 ? (
        <div className={styles.empty}><Empty description="Chưa có giao dịch trong sổ cái" /></div>
      ) : !error && (
        <>
          <div className={styles.desktopTable}>
            <Table<LedgerEntryView>
              rowKey="id"
              columns={columns}
              dataSource={entries}
              pagination={false}
              loading={false}
              scroll={{ x: 700 }}
            />
          </div>
          <div className={styles.mobileList} role="list" aria-label="Giao dịch">
            {entries.map(entry => (
              <article className={styles.item} key={entry.id} role="listitem">
                <div className={styles.itemHeading}>
                  <div className={styles.description}>
                    <strong>{getEntryTypeLabel(entry.entryType)}</strong>
                    <time>{formatLedgerTime(entry.createdAt)}</time>
                  </div>
                  <span className={`${styles.amount} ${entry.direction === 'CREDIT' ? styles.credit : styles.debit}`}>
                    {formatAmount(entry)}
                  </span>
                </div>
                <p>{entry.description || '—'}</p>
                {getBucketTag(entry.balanceBucket)}
              </article>
            ))}
          </div>
          {total > 0 && (
            <Pagination
              className={styles.pagination}
              current={page + 1}
              pageSize={pageSize}
              total={total}
              showSizeChanger
              onChange={(nextPage, nextSize) => onPageChange?.(nextPage - 1, nextSize)}
            />
          )}
        </>
      )}
    </section>
  );
};
