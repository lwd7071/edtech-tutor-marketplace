'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Alert, App, Button, Skeleton } from 'antd';
import { useCreatePayout, useTeacherBankAccounts, useTeacherPayouts, useTeacherWallet } from '../hooks/useFinance';
import { CreatePayoutModal } from '../components/CreatePayoutModal';
import { PayoutListTable } from '../components/PayoutListTable';
import type { CreatePayoutRequest } from '../types';
import { parseApiError } from '@/shared/backend';
import styles from './teacher-payouts-page.module.css';

const money = (value: number) => `${value.toLocaleString('vi-VN')} ₫`;

export const TeacherPayoutsPage = () => {
  const { message } = App.useApp();
  const [payoutModalOpen, setPayoutModalOpen] = useState(false);
  const [payoutPage, setPayoutPage] = useState(0);
  const [payoutPageSize, setPayoutPageSize] = useState(20);
  const walletQuery = useTeacherWallet();
  const bankQuery = useTeacherBankAccounts();
  const payoutQuery = useTeacherPayouts(undefined, payoutPage, payoutPageSize);
  const createPayoutMutation = useCreatePayout();

  const wallet = walletQuery.data?.data;
  const bankAccounts = bankQuery.data?.data ?? [];
  const payouts = payoutQuery.data?.data ?? [];

  const handleCreatePayoutSubmit = async (values: CreatePayoutRequest) => {
    if (!wallet) {
      message.error('Chưa tải được dữ liệu ví. Vui lòng tải lại.');
      return;
    }
    try {
      await createPayoutMutation.mutateAsync({ ...values, walletVersion: wallet.version });
      message.success('Tạo yêu cầu rút tiền thành công. Ban quản trị sẽ sớm duyệt và chuyển khoản.');
      setPayoutModalOpen(false);
    } catch (err: unknown) {
      message.error(parseApiError(err).message || 'Không thể tạo yêu cầu rút tiền. Vui lòng thử lại.');
    }
  };

  return (
    <div className={styles.page}>
      <header className={styles.heading}>
        <h1>Yêu cầu rút tiền</h1>
        <p>Tạo lệnh chuyển khoản và theo dõi trạng thái xử lý của từng yêu cầu.</p>
      </header>

      {walletQuery.isLoading ? (
        <section className={styles.balanceLoading} role="status" aria-label="Đang tải số dư khả dụng">
          <Skeleton.Input active size="small" />
          <Skeleton.Input active />
          <Skeleton.Input active size="small" />
        </section>
      ) : walletQuery.isError ? (
        <Alert
          type="error"
          showIcon
          title="Chưa tải được số dư khả dụng"
          action={<Button onClick={() => { void walletQuery.refetch(); }}>Thử tải số dư</Button>}
        />
      ) : wallet ? (
        <section className={styles.balance} aria-labelledby="payout-balance-heading">
          <div className={styles.balanceTop}>
            <div className={styles.available}>
              <p id="payout-balance-heading">Có thể rút</p>
              <strong>{money(wallet.availableBalanceVnd)}</strong>
            </div>
            <Button className={styles.requestButton} type="primary" onClick={() => setPayoutModalOpen(true)}>
              Tạo yêu cầu rút tiền
            </Button>
          </div>
          <p className={styles.condition}>Mỗi lần rút tối thiểu 50.000 ₫. Chỉ số dư khả dụng mới được dùng cho yêu cầu rút tiền.</p>
          <dl className={styles.otherBalances}>
            <div>
              <dt>Chờ quyết toán</dt>
              <dd>{money(wallet.pendingBalanceVnd)}</dd>
            </div>
            <div>
              <dt>Đang rút</dt>
              <dd>{money(wallet.reservedBalanceVnd)}</dd>
            </div>
          </dl>
        </section>
      ) : null}

      <section className={styles.bankContext} aria-label="Tài khoản nhận tiền">
        {bankQuery.isLoading && <p role="status">Đang tải tài khoản nhận tiền…</p>}
        {bankQuery.isError && (
          <Alert
            type="warning"
            showIcon
            title="Chưa tải được tài khoản nhận tiền"
            action={<Button onClick={() => { void bankQuery.refetch(); }}>Thử tải lại tài khoản</Button>}
          />
        )}
        {!bankQuery.isLoading && !bankQuery.isError && bankAccounts.length === 0 && (
          <p>Chưa có tài khoản nhận tiền. <Link href="/teacher/bank-accounts">Thêm tài khoản ngân hàng</Link> trước khi xác nhận yêu cầu.</p>
        )}
      </section>

      <section className={styles.history} aria-labelledby="payout-history-heading">
        <div className={styles.historyHeading}>
          <h2 id="payout-history-heading">Lịch sử yêu cầu</h2>
          {!payoutQuery.isError && !payoutQuery.isLoading && (
            <span>{payoutQuery.data?.meta?.totalElements ?? payouts.length} yêu cầu</span>
          )}
        </div>
        <PayoutListTable
          payouts={payouts}
          loading={payoutQuery.isLoading}
          error={payoutQuery.isError}
          onRetry={() => { void payoutQuery.refetch(); }}
          total={payoutQuery.data?.meta?.totalElements ?? payouts.length}
          page={payoutPage}
          pageSize={payoutPageSize}
          onPageChange={(nextPage, nextSize) => {
            setPayoutPage(nextPage);
            setPayoutPageSize(nextSize);
          }}
        />
      </section>

      {wallet && (
        <CreatePayoutModal
          open={payoutModalOpen}
          availableBalanceVnd={wallet.availableBalanceVnd}
          walletVersion={wallet.version}
          bankAccounts={bankAccounts}
          loading={createPayoutMutation.isPending}
          onCancel={() => setPayoutModalOpen(false)}
          onSubmit={handleCreatePayoutSubmit}
        />
      )}
    </div>
  );
};
