'use client';

import { useState } from 'react';
import { Alert, App } from 'antd';
import {
  useTeacherWallet,
  useTeacherLedger,
  useTeacherBankAccounts,
  useTeacherPayouts,
  useCreateBankAccount,
  useUpdateBankAccount,
  useDeleteBankAccount,
  useCreatePayout,
} from '../hooks/useFinance';
import { WalletSummaryCard } from '../components/WalletSummaryCard';
import { LedgerTable } from '../components/LedgerTable';
import { BankAccountList } from '../components/BankAccountList';
import { CreatePayoutModal } from '../components/CreatePayoutModal';
import { PayoutListTable } from '../components/PayoutListTable';
import type { CreatePayoutRequest } from '../types';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import styles from './teacher-wallet-page.module.css';

export const TeacherWalletPage = () => {
  const { message } = App.useApp();
  const [activeTab, setActiveTab] = useState('overview');
  const [payoutModalOpen, setPayoutModalOpen] = useState(false);
  const [ledgerPage, setLedgerPage] = useState(0);
  const [ledgerPageSize, setLedgerPageSize] = useState(20);
  const [payoutPage, setPayoutPage] = useState(0);
  const [payoutPageSize, setPayoutPageSize] = useState(20);

  const {
    data: walletRes,
    isLoading: walletLoading,
    isError: walletError,
    refetch: refetchWallet,
  } = useTeacherWallet();
  const {
    data: ledgerRes,
    isLoading: ledgerLoading,
    isError: ledgerError,
    refetch: refetchLedger,
  } = useTeacherLedger(ledgerPage, ledgerPageSize);
  const {
    data: bankRes,
    isLoading: bankLoading,
    isError: bankError,
    refetch: refetchBankAccounts,
  } = useTeacherBankAccounts();
  const {
    data: payoutsRes,
    isLoading: payoutsLoading,
    isError: payoutsError,
    refetch: refetchPayouts,
  } = useTeacherPayouts(undefined, payoutPage, payoutPageSize);

  const createBankMutation = useCreateBankAccount();
  const updateBankMutation = useUpdateBankAccount();
  const deleteBankMutation = useDeleteBankAccount();
  const createPayoutMutation = useCreatePayout();

  const wallet = walletRes?.data;
  const ledgerEntries = ledgerRes?.data ?? [];
  const bankAccounts = bankRes?.data ?? [];
  const payouts = payoutsRes?.data ?? [];

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

  const tabs = [
    {
      key: 'overview',
      label: 'Sổ cái giao dịch',
      content: (
        <LedgerTable
          entries={ledgerEntries}
          loading={ledgerLoading}
          error={ledgerError}
          onRetry={() => { void refetchLedger(); }}
          total={ledgerRes?.meta?.totalElements ?? ledgerEntries.length}
          page={ledgerPage}
          pageSize={ledgerPageSize}
          onPageChange={(nextPage, nextSize) => {
            setLedgerPage(nextPage);
            setLedgerPageSize(nextSize);
          }}
        />
      ),
    },
    {
      key: 'payouts',
      label: `Lịch sử rút tiền (${payoutsRes?.meta?.totalElements ?? payouts.length})`,
      content: (
        <PayoutListTable
          payouts={payouts}
          loading={payoutsLoading}
          error={payoutsError}
          onRetry={() => { void refetchPayouts(); }}
          total={payoutsRes?.meta?.totalElements ?? payouts.length}
          page={payoutPage}
          pageSize={payoutPageSize}
          onPageChange={(nextPage, nextSize) => {
            setPayoutPage(nextPage);
            setPayoutPageSize(nextSize);
          }}
        />
      ),
    },
    {
      key: 'bank',
      label: `Tài khoản ngân hàng (${bankAccounts.length})`,
      content: (
        <BankAccountList
          accounts={bankAccounts}
          loading={bankLoading}
          error={bankError}
          onRetry={() => { void refetchBankAccounts(); }}
          onCreateAccount={async data => {
            try {
              await createBankMutation.mutateAsync(data);
              message.success('Thêm tài khoản ngân hàng thành công');
            } catch (error) {
              message.error(parseApiError(error).message);
            }
          }}
          onUpdateAccount={async (id, data) => {
            try {
              await updateBankMutation.mutateAsync({ id, data });
              message.success('Cập nhật tài khoản ngân hàng thành công');
            } catch (error) {
              message.error(isConcurrentModification(error)
                ? 'Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.'
                : parseApiError(error).message);
            }
          }}
          onDeleteAccount={async (id, version) => {
            try {
              await deleteBankMutation.mutateAsync({ id, version });
              message.success('Đã xóa tài khoản ngân hàng');
            } catch (error) {
              message.error(isConcurrentModification(error)
                ? 'Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.'
                : parseApiError(error).message);
            }
          }}
        />
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <header className={styles.heading}>
        <h1>Ví và thu nhập gia sư</h1>
        <p>Quản lý số dư, theo dõi biến động và kiểm tra các yêu cầu rút tiền.</p>
      </header>

      <WalletSummaryCard
        wallet={wallet}
        loading={walletLoading}
        error={walletError}
        onRetry={() => { void refetchWallet(); }}
        onRequestPayout={() => setPayoutModalOpen(true)}
      />

      <section className={styles.activity} aria-label="Giao dịch và tài khoản">
        <div className={styles.tabs}>
          <div className={styles.tabList} role="tablist" aria-label="Giao dịch và tài khoản">
            {tabs.map((tab, index) => (
              <button
                key={tab.key}
                id={`wallet-tab-${tab.key}`}
                className={`${styles.tabButton} ${activeTab === tab.key ? styles.tabButtonActive : ''}`}
                type="button"
                role="tab"
                aria-selected={activeTab === tab.key}
                aria-controls={`wallet-panel-${tab.key}`}
                tabIndex={activeTab === tab.key ? 0 : -1}
                onClick={() => setActiveTab(tab.key)}
                onKeyDown={event => {
                  const nextIndex = event.key === 'ArrowRight' ? (index + 1) % tabs.length
                    : event.key === 'ArrowLeft' ? (index - 1 + tabs.length) % tabs.length
                      : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : -1;
                  if (nextIndex >= 0) {
                    event.preventDefault();
                    const nextTab = tabs[nextIndex];
                    setActiveTab(nextTab.key);
                    document.getElementById(`wallet-tab-${nextTab.key}`)?.focus();
                  }
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>
          {tabs.map(tab => (
            <div
              key={tab.key}
              className={styles.tabPanel}
              id={`wallet-panel-${tab.key}`}
              role="tabpanel"
              aria-labelledby={`wallet-tab-${tab.key}`}
              hidden={activeTab !== tab.key}
            >
              {tab.content}
            </div>
          ))}
        </div>
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
      {walletError && activeTab !== 'overview' && (
        <Alert className={styles.walletNotice} type="warning" showIcon title="Chưa tải được số dư ví. Không thể tạo yêu cầu rút tiền." />
      )}
    </div>
  );
};
