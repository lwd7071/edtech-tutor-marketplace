'use client';

import React, { useState } from 'react';
import { Input, message, Modal } from 'antd';
import { useChangeUserStatus } from '../hooks/useAdminApprovals';
import { parseApiError } from '@/shared/backend';
import styles from './UserModerationModal.module.css';

interface ModerationUser {
  id: string;
  fullName: string;
  email: string;
  currentStatus: 'ACTIVE' | 'LOCKED';
}

interface UserModerationModalProps {
  open: boolean;
  user: ModerationUser | null;
  onClose: () => void;
}

export const UserModerationModal: React.FC<UserModerationModalProps> = ({ open, user, onClose }) => {
  const [reason, setReason] = useState<string>('');
  const changeStatusMutation = useChangeUserStatus();

  if (!user) return null;

  const isLocking = user.currentStatus === 'ACTIVE';
  const targetStatus: 'ACTIVE' | 'LOCKED' = isLocking ? 'LOCKED' : 'ACTIVE';
  const title = isLocking ? 'Khóa tài khoản người dùng' : 'Mở khóa tài khoản người dùng';
  const okText = isLocking ? 'Xác nhận khóa' : 'Xác nhận mở khóa';

  const handleConfirm = async () => {
    if (!reason.trim()) {
      message.error('Vui lòng nhập lý do điều chỉnh trạng thái tài khoản');
      return;
    }

    try {
      await changeStatusMutation.mutateAsync({
        userId: user.id,
        data: { status: targetStatus, reason: reason.trim() },
      });
      message.success(isLocking ? 'Đã khóa tài khoản người dùng' : 'Đã mở khóa tài khoản người dùng');
      setReason('');
      onClose();
    } catch (error) {
      message.error(parseApiError(error).message);
    }
  };

  return <Modal
    className={styles.modal}
    title={title}
    open={open}
    onCancel={() => { setReason(''); onClose(); }}
    onOk={handleConfirm}
    confirmLoading={changeStatusMutation.isPending}
    okText={okText}
    cancelText="Hủy"
    okButtonProps={{ danger: isLocking }}
    destroyOnHidden
  >
    <section className={styles.context} aria-label="Tài khoản được điều chỉnh">
      <div className={styles.identity}>
        <h2>{user.fullName}</h2>
        <a href={`mailto:${user.email}`}>{user.email}</a>
      </div>
      <p className={styles.explanation}>
        {isLocking
          ? 'Người dùng bị khóa sẽ không thể đăng nhập hoặc thực hiện bất kỳ giao dịch nào trên hệ thống.'
          : 'Tài khoản người dùng sẽ được kích hoạt lại trạng thái hoạt động bình thường.'}
      </p>
    </section>
    <div className={styles.reason}>
      <label htmlFor="user-status-reason">Lý do điều chỉnh trạng thái</label>
      <Input.TextArea
        id="user-status-reason"
        aria-label="Lý do điều chỉnh trạng thái tài khoản"
        rows={4}
        placeholder="Nhập lý do thực hiện thao tác..."
        value={reason}
        onChange={event => setReason(event.target.value)}
      />
    </div>
  </Modal>;
};
