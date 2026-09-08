'use client';

import React, { useEffect } from 'react';
import { Modal, Form, Input, DatePicker, Alert } from 'antd';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { CreateExtensionRequest } from '../types';
import { formatLedgerTime } from './LedgerTable';

interface CreateExtensionModalProps {
  open: boolean;
  packageId: string;
  packageName: string;
  currentExpiryDate?: string;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: CreateExtensionRequest) => Promise<void> | void;
}

export const CreateExtensionModal: React.FC<CreateExtensionModalProps> = ({
  open,
  packageId,
  packageName,
  currentExpiryDate,
  loading = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<{ requestedDate: Dayjs; reason: string }>();

  useEffect(() => {
    if (open) {
      form.resetFields();
    }
  }, [open, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit({
        studentPackageId: packageId,
        requestedExpiryDate: values.requestedDate.toISOString(),
        reason: values.reason,
      });
    } catch {
      // Form validation error
    }
  };

  return (
    <Modal
      open={open}
      title="Yêu cầu gia hạn gói học"
      okText="Gửi yêu cầu gia hạn"
      cancelText="Hủy"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnHidden
    >
      <div style={{ margin: '16px 0' }}>
        <Alert
          type="info"
          showIcon
          title={`Gói học: ${packageName}`}
          description={
            <div style={{ marginTop: 4 }}>
              {currentExpiryDate && (
                <div>
                  Hạn sử dụng hiện tại: <strong>{formatLedgerTime(currentExpiryDate)}</strong>
                </div>
              )}
              Gói học sau khi được ban quản trị phê duyệt gia hạn sẽ kích hoạt lại trạng thái hoạt động để tiếp tục đặt lịch học.
            </div>
          }
        />
      </div>

      <Form form={form} layout="vertical">
        <Form.Item
          label="Ngày hết hạn mong muốn"
          name="requestedDate"
          rules={[{ required: true, message: 'Vui lòng chọn ngày gia hạn mong muốn' }]}
        >
          <DatePicker
            style={{ width: '100%' }}
            format="DD/MM/YYYY"
            disabledDate={(current) => current && current < dayjs().endOf('day')}
            placeholder="Chọn ngày kết thúc mới"
          />
        </Form.Item>

        <Form.Item
          label="Lý do xin gia hạn"
          name="reason"
          rules={[
            { required: true, message: 'Vui lòng nhập lý do xin gia hạn' },
            { min: 10, message: 'Vui lòng mô tả chi tiết (tối thiểu 10 ký tự)' },
          ]}
        >
          <Input.TextArea
            rows={3}
            placeholder="Ví dụ: Đợt vừa rồi em thi học kỳ bận, mong ban quản trị gia hạn thêm 30 ngày để học nốt..."
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};
