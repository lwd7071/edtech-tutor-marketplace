'use client';

import { App } from 'antd';
import {
  useCreateBankAccount,
  useDeleteBankAccount,
  useTeacherBankAccounts,
  useUpdateBankAccount,
} from '../hooks/useFinance';
import { BankAccountList } from '../components/BankAccountList';
import type { UpsertBankAccountRequest } from '../types';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import styles from './teacher-bank-accounts-page.module.css';

export const TeacherBankAccountsPage = () => {
  const { message } = App.useApp();
  const {
    data: bankRes,
    isLoading,
    isError,
    refetch,
  } = useTeacherBankAccounts();
  const createMutation = useCreateBankAccount();
  const updateMutation = useUpdateBankAccount();
  const deleteMutation = useDeleteBankAccount();

  const handleCreate = async (data: UpsertBankAccountRequest) => {
    try {
      await createMutation.mutateAsync(data);
      message.success('Thêm tài khoản ngân hàng thành công');
    } catch (error) {
      message.error(parseApiError(error).message);
    }
  };

  const handleUpdate = async (id: string, data: UpsertBankAccountRequest) => {
    try {
      await updateMutation.mutateAsync({ id, data });
      message.success('Cập nhật tài khoản ngân hàng thành công');
    } catch (error) {
      message.error(isConcurrentModification(error)
        ? 'Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.'
        : parseApiError(error).message);
    }
  };

  const handleDelete = async (id: string, version: number) => {
    try {
      await deleteMutation.mutateAsync({ id, version });
      message.success('Đã xóa tài khoản ngân hàng');
    } catch (error) {
      message.error(isConcurrentModification(error)
        ? 'Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.'
        : parseApiError(error).message);
    }
  };

  return (
    <div className={styles.page}>
      <header className={styles.heading}>
        <h1>Tài khoản ngân hàng</h1>
        <p>Quản lý thông tin nhận tiền. Số tài khoản luôn được che khi hiển thị.</p>
      </header>

      <BankAccountList
        accounts={bankRes?.data ?? []}
        loading={isLoading}
        error={isError}
        onRetry={() => { void refetch(); }}
        onCreateAccount={handleCreate}
        onUpdateAccount={handleUpdate}
        onDeleteAccount={handleDelete}
      />
    </div>
  );
};
