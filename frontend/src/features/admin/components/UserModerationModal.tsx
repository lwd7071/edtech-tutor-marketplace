'use client';

import React, { useState } from 'react';
import { Modal, Input, Typography, message } from 'antd';
import { useChangeUserStatus } from '../hooks/useAdminApprovals';

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

export const UserModerationModal: React.FC<UserModerationModalProps> = ({
  open,
  user,
  onClose,
}) => {
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
        data: {
          status: targetStatus,
          reason: reason.trim(),
        },
      });
      message.success(isLocking ? 'Đã khóa tài khoản người dùng' : 'Đã mở khóa tài khoản người dùng');
      setReason('');
      onClose();
    } catch {
      message.error('Thao tác thay đổi trạng thái thất bại');
    }
  };

  return (
    <Modal
      title={title}
      open={open}
      onCancel={() => {
        setReason('');
        onClose();
      }}
      onOk={handleConfirm}
      confirmLoading={changeStatusMutation.isPending}
      okText={okText}
      cancelText="Hủy"
      okButtonProps={{ danger: isLocking }}
      destroyOnHidden
    >
      <Typography.Paragraph>
        Bạn đang thực hiện thao tác đối với người dùng <strong>{user.fullName}</strong> ({user.email}).
      </Typography.Paragraph>
      <Typography.Paragraph type="secondary">
        {isLocking
          ? 'Người dùng bị khóa sẽ không thể đăng nhập hoặc thực hiện bất kỳ giao dịch nào trên hệ thống.'
          : 'Tài khoản người dùng sẽ được kích hoạt lại trạng thái hoạt động bình thường.'}
      </Typography.Paragraph>
      <Input.TextArea
        rows={4}
        placeholder="Nhập lý do thực hiện thao tác..."
        value={reason}
        onChange={(e) => setReason(e.target.value)}
      />
    </Modal>
  );
};
