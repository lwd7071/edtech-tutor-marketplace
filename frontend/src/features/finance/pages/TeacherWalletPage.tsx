'use client';

import React, { useState } from 'react';
import { Tabs, Typography, message } from 'antd';
import {
  WalletOutlined,
  BankOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
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
import { CreatePayoutRequest } from '../types';

export const TeacherWalletPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [payoutModalOpen, setPayoutModalOpen] = useState(false);
  const [ledgerPage, setLedgerPage] = useState(0);
  const [ledgerPageSize, setLedgerPageSize] = useState(20);
  const [payoutPage, setPayoutPage] = useState(0);
  const [payoutPageSize, setPayoutPageSize] = useState(20);

  // Queries
  const { data: walletRes, isLoading: walletLoading } = useTeacherWallet();
  const { data: ledgerRes, isLoading: ledgerLoading } = useTeacherLedger(ledgerPage, ledgerPageSize);
  const { data: bankRes, isLoading: bankLoading } = useTeacherBankAccounts();
  const { data: payoutsRes, isLoading: payoutsLoading } = useTeacherPayouts(undefined, payoutPage, payoutPageSize);

  // Mutations
  const createBankMutation = useCreateBankAccount();
  const updateBankMutation = useUpdateBankAccount();
  const deleteBankMutation = useDeleteBankAccount();
  const createPayoutMutation = useCreatePayout();

  const wallet = walletRes?.data;
  const ledgerEntries = ledgerRes?.data || [];
  const bankAccounts = bankRes?.data || [];
  const payouts = payoutsRes?.data || [];

  const handleCreatePayoutSubmit = async (values: CreatePayoutRequest) => {
    try {
      await createPayoutMutation.mutateAsync(values);
      message.success('Tạo yêu cầu rút tiền thành công. Ban quản trị sẽ sớm duyệt và chuyển khoản.');
      setPayoutModalOpen(false);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Không thể tạo yêu cầu rút tiền. Vui lòng thử lại.');
    }
  };

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 'var(--space-6) 0' }}>
      <div style={{ marginBottom: 24 }}>
        <Typography.Title level={2} style={{ marginBottom: 4 }}>
          Ví & Tài chính Giáo viên
        </Typography.Title>
        <Typography.Text type="secondary" style={{ fontSize: 14 }}>
          Quản lý số dư thu nhập, sổ cái biến động tiền, tài khoản nhận và yêu cầu rút tiền
        </Typography.Text>
      </div>

      <WalletSummaryCard
        wallet={wallet}
        loading={walletLoading}
        onRequestPayout={() => setPayoutModalOpen(true)}
      />

      <div
        style={{
          background: 'var(--color-surface, #FFFFFF)',
          borderRadius: 'var(--radius-lg, 12px)',
          padding: 20,
        }}
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'overview',
              label: (
                <span>
                  <WalletOutlined style={{ marginRight: 6 }} />
                  Sổ cái giao dịch
                </span>
              ),
              children: (
                <LedgerTable
                  entries={ledgerEntries}
                  loading={ledgerLoading}
                  total={ledgerRes?.meta?.totalElements || ledgerEntries.length}
                  page={ledgerPage}
                  pageSize={ledgerPageSize}
                  onPageChange={(p, s) => {
                    setLedgerPage(p);
                    setLedgerPageSize(s);
                  }}
                />
              ),
            },
            {
              key: 'payouts',
              label: (
                <span>
                  <HistoryOutlined style={{ marginRight: 6 }} />
                  Lịch sử rút tiền ({payouts.length})
                </span>
              ),
              children: (
                <PayoutListTable
                  payouts={payouts}
                  loading={payoutsLoading}
                  total={payoutsRes?.meta?.totalElements || payouts.length}
                  page={payoutPage}
                  pageSize={payoutPageSize}
                  onPageChange={(p, s) => {
                    setPayoutPage(p);
                    setPayoutPageSize(s);
                  }}
                />
              ),
            },
            {
              key: 'bank',
              label: (
                <span>
                  <BankOutlined style={{ marginRight: 6 }} />
                  Tài khoản ngân hàng ({bankAccounts.length})
                </span>
              ),
              children: (
                <BankAccountList
                  accounts={bankAccounts}
                  loading={bankLoading}
                  onCreateAccount={async (data) => {
                    await createBankMutation.mutateAsync(data);
                    message.success('Thêm tài khoản ngân hàng thành công');
                  }}
                  onUpdateAccount={async (id, data) => {
                    await updateBankMutation.mutateAsync({ id, data });
                    message.success('Cập nhật tài khoản ngân hàng thành công');
                  }}
                  onDeleteAccount={async (id) => {
                    await deleteBankMutation.mutateAsync(id);
                    message.success('Đã xóa tài khoản ngân hàng');
                  }}
                />
              ),
            },
          ]}
        />
      </div>

      <CreatePayoutModal
        open={payoutModalOpen}
        availableBalanceVnd={wallet?.availableBalanceVnd ?? 0}
        bankAccounts={bankAccounts}
        loading={createPayoutMutation.isPending}
        onCancel={() => setPayoutModalOpen(false)}
        onSubmit={handleCreatePayoutSubmit}
      />
    </div>
  );
};
