'use client';

import { Alert, Button, Skeleton } from 'antd';
import type { WalletView } from '../types';
import styles from './WalletSummaryCard.module.css';

interface WalletSummaryCardProps {
  wallet?: WalletView | null;
  loading?: boolean;
  error?: boolean;
  onRetry?: () => void;
  onRequestPayout?: () => void;
}

const formatVnd = (value: number) => `${value.toLocaleString('vi-VN')} ₫`;

export const WalletSummaryCard = ({
  wallet,
  loading = false,
  error = false,
  onRetry,
  onRequestPayout,
}: WalletSummaryCardProps) => (
  <section className={styles.section} aria-labelledby="wallet-summary-heading">
    <header className={styles.heading}>
      <div>
        <h2 id="wallet-summary-heading">Số dư ví</h2>
        <p>Tiền đang giữ là khoản escrow và chưa thể rút.</p>
      </div>
    </header>

    {error && (
      <Alert
        className={styles.error}
        type="error"
        showIcon
        title="Chưa tải được số dư ví"
        action={<Button onClick={onRetry}>Thử tải số dư</Button>}
      />
    )}

    {loading && (
      <div className={styles.loading} role="status" aria-label="Đang tải số dư ví">
        {[0, 1, 2, 3].map(row => <Skeleton.Input active size="small" key={row} />)}
      </div>
    )}

    {!loading && !error && wallet && (
      <dl className={styles.balances}>
        <div className={`${styles.balance} ${styles.available}`}>
          <dt>Số dư khả dụng</dt>
          <dd>{formatVnd(wallet.availableBalanceVnd)}</dd>
          <p>Có thể yêu cầu rút về tài khoản ngân hàng.</p>
          <Button
            type="primary"
            disabled={wallet.availableBalanceVnd <= 0}
            onClick={onRequestPayout}
          >
            Yêu cầu rút tiền
          </Button>
        </div>
        <div className={styles.balance}>
          <dt>Chờ quyết toán</dt>
          <dd>{formatVnd(wallet.pendingBalanceVnd)}</dd>
          <p>Chưa chuyển sang số dư có thể rút.</p>
        </div>
        <div className={styles.balance}>
          <dt>Đang xử lý rút</dt>
          <dd>{formatVnd(wallet.reservedBalanceVnd)}</dd>
          <p>Đã dành cho các yêu cầu rút đang xử lý.</p>
        </div>
        <div className={`${styles.balance} ${styles.held}`}>
          <dt>Tiền buổi học đang giữ</dt>
          <dd>{formatVnd(wallet.heldBalanceVnd ?? 0)}</dd>
          <p>Khoản escrow này không nằm trong số dư khả dụng.</p>
        </div>
      </dl>
    )}
  </section>
);
