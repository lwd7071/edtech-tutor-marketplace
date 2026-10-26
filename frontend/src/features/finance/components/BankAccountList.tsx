'use client';

import { useState } from 'react';
import { Alert, Button, Empty, Popconfirm, Skeleton, Tag, Typography } from 'antd';
import type { BankAccountView, UpsertBankAccountRequest } from '../types';
import { BankAccountModal } from './BankAccountModal';
import styles from './BankAccountList.module.css';

interface BankAccountListProps {
  accounts?: BankAccountView[];
  loading?: boolean;
  error?: boolean;
  onRetry?: () => void;
  onCreateAccount: (data: UpsertBankAccountRequest) => Promise<void> | void;
  onUpdateAccount: (id: string, data: UpsertBankAccountRequest) => Promise<void> | void;
  onDeleteAccount: (id: string, version: number) => Promise<void> | void;
}

export const BankAccountList = ({
  accounts = [],
  loading = false,
  error = false,
  onRetry,
  onCreateAccount,
  onUpdateAccount,
  onDeleteAccount,
}: BankAccountListProps) => {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<BankAccountView | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const openCreate = () => {
    setEditingAccount(null);
    setModalOpen(true);
  };

  const openEdit = (account: BankAccountView) => {
    setEditingAccount(account);
    setModalOpen(true);
  };

  const submit = async (values: UpsertBankAccountRequest) => {
    setActionLoading(true);
    try {
      if (editingAccount) {
        await onUpdateAccount(editingAccount.id, { ...values, version: editingAccount.version });
      } else {
        await onCreateAccount({ ...values, version: 0 });
      }
      setModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <section className={styles.section} aria-labelledby="bank-account-heading">
      <header className={styles.heading}>
        <div>
          <h2 id="bank-account-heading">Tài khoản nhận tiền</h2>
          <p>Chỉ hiển thị số tài khoản đã được che.</p>
        </div>
        {!error && accounts.length > 0 && <Button type="primary" onClick={openCreate}>Thêm tài khoản</Button>}
      </header>

      {error && (
        <Alert
          type="error"
          showIcon
          title="Chưa tải được tài khoản ngân hàng"
          action={<Button onClick={onRetry}>Thử lại tài khoản</Button>}
        />
      )}
      {loading ? (
        <div className={styles.loading} role="status" aria-label="Đang tải tài khoản ngân hàng">
          {[0, 1].map(row => <Skeleton.Input active block key={row} />)}
        </div>
      ) : !error && accounts.length === 0 ? (
        <div className={styles.empty}>
          <Empty description="Bạn chưa liên kết tài khoản ngân hàng nào" />
          <Button type="primary" onClick={openCreate}>Thêm tài khoản</Button>
        </div>
      ) : !error && (
        <div className={styles.list} role="list" aria-label="Tài khoản ngân hàng đã liên kết">
          {accounts.map(account => (
            <article className={styles.account} key={account.id} role="listitem">
              <div className={styles.identity}>
                <strong>{account.bankName}</strong>
                {account.isDefault && <Tag className={styles.defaultTag}>Mặc định</Tag>}
                <Typography.Text className={styles.maskedNumber} copyable={{ text: account.accountNumberMasked }}>
                  {account.accountNumberMasked}
                </Typography.Text>
                <span className={styles.holder}>{account.accountHolderName}</span>
              </div>
              <div className={styles.actions}>
                <Button type="link" onClick={() => openEdit(account)} aria-label={`Sửa tài khoản ${account.bankName}`}>
                  Sửa
                </Button>
                <Popconfirm
                  title="Xác nhận xóa tài khoản này?"
                  okText="Xóa"
                  cancelText="Hủy"
                  okButtonProps={{ danger: true }}
                  onConfirm={() => onDeleteAccount(account.id, account.version)}
                >
                  <Button type="link" danger aria-label={`Xóa tài khoản ${account.bankName}`}>
                    Xóa
                  </Button>
                </Popconfirm>
              </div>
            </article>
          ))}
        </div>
      )}

      <BankAccountModal
        open={modalOpen}
        initialData={editingAccount}
        loading={actionLoading}
        onCancel={() => setModalOpen(false)}
        onSubmit={submit}
      />
    </section>
  );
};
