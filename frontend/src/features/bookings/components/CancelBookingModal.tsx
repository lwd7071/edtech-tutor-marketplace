'use client';

import React, { useState } from 'react';
import { Modal, Radio, Input, Typography, message, Space } from 'antd';
import { BookingDetail } from '../types';
import { useCancelBooking } from '../hooks/useBookings';

interface CancelBookingModalProps {
  open: boolean;
  booking: BookingDetail | null;
  onClose: () => void;
}

export const CancelBookingModal: React.FC<CancelBookingModalProps> = ({
  open,
  booking,
  onClose,
}) => {
  const [initiatedBy, setInitiatedBy] = useState<'STUDENT_REQUEST' | 'TEACHER_EMERGENCY'>('STUDENT_REQUEST');
  const [reason, setReason] = useState<string>('');
  const cancelMutation = useCancelBooking();

  if (!booking) return null;

  const handleConfirm = async () => {
    if (!reason.trim()) {
      message.error('Vui lòng nhập lý do hủy lịch học');
      return;
    }

    try {
      await cancelMutation.mutateAsync({
        id: booking.id,
        data: {
          version: booking.version,
          reason: reason.trim(),
          initiatedBy,
        },
      });
      message.success('Đã hủy lịch học thành công');
      setReason('');
      onClose();
    } catch {
      message.error('Hủy lịch học thất bại. Vui lòng thử lại.');
    }
  };

  return (
    <Modal
      title="Hủy lịch học"
      open={open}
      onCancel={() => {
        setReason('');
        onClose();
      }}
      onOk={handleConfirm}
      confirmLoading={cancelMutation.isPending}
      okText="Xác nhận hủy buổi học"
      cancelText="Đóng"
      okButtonProps={{ danger: true }}
      destroyOnHidden
    >
      <Typography.Paragraph>
        Bạn đang hủy buổi học môn <strong>{booking.subject.name}</strong> với học viên <strong>{booking.student.fullName}</strong>.
      </Typography.Paragraph>

      <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
        Đối tượng khởi tạo hủy lịch:
      </Typography.Text>
      <Radio.Group
        value={initiatedBy}
        onChange={(e) => setInitiatedBy(e.target.value)}
        style={{ marginBottom: 16, display: 'flex', flexDirection: 'column', gap: 8 }}
      >
        <Radio value="STUDENT_REQUEST">Học viên yêu cầu đổi hoặc hủy lịch</Radio>
        <Radio value="TEACHER_EMERGENCY">Gia sư có việc đột xuất</Radio>
      </Radio.Group>

      <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
        Lý do hủy buổi học:
      </Typography.Text>
      <Input.TextArea
        rows={3}
        placeholder="Nhập lý do hủy lịch học chi tiết..."
        value={reason}
        onChange={(e) => setReason(e.target.value)}
      />
    </Modal>
  );
};
