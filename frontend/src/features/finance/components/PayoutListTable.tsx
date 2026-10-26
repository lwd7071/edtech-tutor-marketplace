'use client';

import { Alert, Button, Empty, Pagination, Skeleton, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { PayoutRequestView, PayoutStatus } from '../types';
import { formatLedgerTime } from './LedgerTable';
import styles from './FinanceActivityLists.module.css';

interface PayoutListTableProps {
  payouts?: PayoutRequestView[];
  loading?: boolean;
  error?: boolean;
  onRetry?: () => void;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, size: number) => void;
}

const payoutStatusLabel = (status: PayoutStatus) => {
  switch (status) {
    case 'PENDING': return 'Chờ duyệt';
    case 'PROCESSING': return 'Đang xử lý';
    case 'SUCCEEDED': return 'Đã chuyển';
    case 'REJECTED': return 'Bị từ chối';
    default: return status;
  }
};

export const getPayoutStatusTag = (status: PayoutStatus) => (
  <Tag className={styles.status}>{payoutStatusLabel(status)}</Tag>
);

const PayoutReference = ({ payout }: { payout: PayoutRequestView }) => (
  <div className={styles.description}>
    {payout.bankReference && <span>Mã GD: {payout.bankReference}</span>}
    {payout.proofUrl && (
      <a href={payout.proofUrl} target="_blank" rel="noopener noreferrer">Xem biên lai</a>
    )}
    {!payout.bankReference && !payout.proofUrl && <Typography.Text type="secondary">Chưa có chứng từ</Typography.Text>}
  </div>
);

const PayoutNote = ({ payout }: { payout: PayoutRequestView }) => {
  const note = payout.adminNote || payout.teacherNote;
  return note ? <span>{note}</span> : <Typography.Text type="secondary">—</Typography.Text>;
};

export const PayoutListTable = ({
  payouts = [],
  loading = false,
  error = false,
  onRetry,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
}: PayoutListTableProps) => {
  const columns: ColumnsType<PayoutRequestView> = [
    {
      title: 'Mã yêu cầu',
      dataIndex: 'id',
      key: 'id',
      width: 150,
      render: (id: string) => <Typography.Text copyable={{ text: id }} className={styles.requestId}>#{id.slice(0, 8)}</Typography.Text>,
    },
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (value: string) => <Typography.Text type="secondary">{formatLedgerTime(value)}</Typography.Text>,
    },
    {
      title: 'Số tiền rút',
      dataIndex: 'amountVnd',
      key: 'amountVnd',
      width: 160,
      align: 'right',
      render: (amount: number) => <span className={styles.amount}>{amount.toLocaleString('vi-VN')} ₫</span>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (status: PayoutStatus) => getPayoutStatusTag(status),
    },
    {
      title: 'Chứng từ',
      key: 'proof',
      width: 180,
      render: (_, payout) => <PayoutReference payout={payout} />,
    },
    {
      title: 'Ghi chú / Lý do',
      key: 'notes',
      width: 220,
      render: (_, payout) => <PayoutNote payout={payout} />,
    },
  ];

  return (
    <section className={styles.section} aria-label="Lịch sử rút tiền">
      {error && (
        <Alert
          className={styles.error}
          type="error"
          showIcon
          title="Chưa tải được lịch sử rút tiền"
          action={<Button onClick={onRetry}>Thử lại lịch sử</Button>}
        />
      )}
      {loading ? (
        <div className={styles.loading} role="status" aria-label="Đang tải lịch sử rút tiền">
          {[0, 1, 2].map(row => <Skeleton.Input active block key={row} />)}
        </div>
      ) : !error && payouts.length === 0 ? (
        <div className={styles.empty}><Empty description="Chưa có yêu cầu rút tiền" /></div>
      ) : !error && (
        <>
          <div className={styles.desktopTable}>
            <Table<PayoutRequestView>
              rowKey="id"
              columns={columns}
              dataSource={payouts}
              pagination={false}
              loading={false}
              scroll={{ x: 950 }}
            />
          </div>
          <div className={styles.mobileList} role="list" aria-label="Yêu cầu rút tiền">
            {payouts.map(request => (
              <article className={styles.item} key={request.id} role="listitem">
                <div className={styles.itemHeading}>
                  <div className={styles.description}>
                    <strong>#{request.id.slice(0, 8)}</strong>
                    <time>{formatLedgerTime(request.createdAt)}</time>
                  </div>
                  <span className={styles.amount}>{request.amountVnd.toLocaleString('vi-VN')} ₫</span>
                </div>
                <div className={styles.itemMeta}>
                  {getPayoutStatusTag(request.status)}
                  <PayoutReference payout={request} />
                </div>
                {(request.adminNote || request.teacherNote) && <p><span>Ghi chú:</span> <PayoutNote payout={request} /></p>}
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
