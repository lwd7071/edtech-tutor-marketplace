'use client';

import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Alert, Typography } from 'antd';
import { CreateRefundRequest } from '../types';

interface CreateRefundModalProps {
  open: boolean;
  packageId: string;
  packageName: string;
  remainingSessions: number;
  estimatedPricePerSession?: number;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: CreateRefundRequest) => Promise<void> | void;
}

const POPULAR_BANKS = [
  { bin: '970422', name: 'MB Bank (Ngân hàng Quân đội)' },
  { bin: '970436', name: 'Vietcombank (Ngoại thương Việt Nam)' },
  { bin: '970407', name: 'Techcombank (Kỹ thương Việt Nam)' },
  { bin: '970415', name: 'VietinBank (Công thương Việt Nam)' },
  { bin: '970418', name: 'BIDV (Đầu tư & Phát triển Việt Nam)' },
  { bin: '970432', name: 'VPBank (Việt Nam Thịnh Vượng)' },
  { bin: '970416', name: 'ACB (Á Châu)' },
  { bin: '970423', name: 'TPBank (Tiên Phong)' },
];

export const CreateRefundModal: React.FC<CreateRefundModalProps> = ({
  open,
  packageId,
  packageName,
  remainingSessions,
  estimatedPricePerSession = 0,
  loading = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<CreateRefundRequest>();
  const watchedSessions = Form.useWatch('requestedSessions', form) || 1;
  const estimatedRefund = watchedSessions * estimatedPricePerSession;

  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue({
        studentPackageId: packageId,
        requestedSessions: Math.min(1, remainingSessions),
      });
    }
  }, [open, packageId, remainingSessions, form]);

  const handleBankChange = (bin: string) => {
    const selected = POPULAR_BANKS.find((b) => b.bin === bin);
    if (selected) {
      form.setFieldsValue({ bankName: selected.name });
    }
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit({
        ...values,
        studentPackageId: packageId,
        accountHolderName: values.accountHolderName.trim().toUpperCase(),
      });
    } catch {
      // Form validation error
    }
  };

  return (
    <Modal
      open={open}
      title="Yêu cầu hoàn tiền gói học"
      okText="Gửi yêu cầu hoàn tiền"
      cancelText="Hủy"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnClose
    >
      <div style={{ margin: '16px 0' }}>
        <Alert
          type="warning"
          showIcon
          title={`Gói học: ${packageName}`}
          description={
            <div style={{ marginTop: 4 }}>
              Số buổi chưa học còn lại: <strong>{remainingSessions} buổi</strong>.
              Khi gửi yêu cầu hoàn tiền, gói học sẽ chuyển sang trạng thái "Đang xử lý hoàn tiền" và tạm khóa chức năng đặt lịch học mới.
            </div>
          }
        />
      </div>

      <Form form={form} layout="vertical">
        <Form.Item name="studentPackageId" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          label="Số buổi muốn hoàn lại"
          name="requestedSessions"
          rules={[
            { required: true, message: 'Vui lòng nhập số buổi muốn hoàn' },
            { type: 'number', min: 1, message: 'Tối thiểu 1 buổi' },
            { type: 'number', max: remainingSessions, message: `Tối đa ${remainingSessions} buổi` },
          ]}
        >
          <InputNumber min={1} max={remainingSessions} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="Lý do hoàn tiền"
          name="reason"
          rules={[
            { required: true, message: 'Vui lòng nhập lý do hoàn tiền' },
            { min: 10, message: 'Vui lòng mô tả chi tiết lý do (tối thiểu 10 ký tự)' },
          ]}
        >
          <Input.TextArea rows={3} placeholder="Ví dụ: Lịch học bận không sắp xếp được tiếp tục..." />
        </Form.Item>

        <Typography.Text strong style={{ display: 'block', marginTop: 12, marginBottom: 8 }}>
          Thông tin tài khoản nhận tiền hoàn trả:
        </Typography.Text>

        <Form.Item
          label="Ngân hàng"
          name="bankBin"
          rules={[{ required: true, message: 'Vui lòng chọn ngân hàng nhận hoàn tiền' }]}
        >
          <Select
            placeholder="Chọn ngân hàng thụ hưởng"
            options={POPULAR_BANKS.map((b) => ({ label: b.name, value: b.bin }))}
            onChange={handleBankChange}
          />
        </Form.Item>

        <Form.Item name="bankName" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          label="Số tài khoản"
          name="accountNumber"
          rules={[
            { required: true, message: 'Vui lòng nhập số tài khoản ngân hàng' },
            { pattern: /^[0-9A-Za-z]{6,25}$/, message: 'Số tài khoản không hợp lệ (6-25 ký tự)' },
          ]}
        >
          <Input placeholder="Ví dụ: 0123456789" />
        </Form.Item>

        <Form.Item
          label="Tên chủ tài khoản (viết hoa không dấu)"
          name="accountHolderName"
          rules={[
            { required: true, message: 'Vui lòng nhập tên chủ tài khoản' },
            { min: 3, message: 'Tên tối thiểu 3 ký tự' },
          ]}
        >
          <Input
            placeholder="NGUYEN VAN A"
            onChange={(e) => {
              form.setFieldsValue({ accountHolderName: e.target.value.toUpperCase() });
            }}
          />
        </Form.Item>

        {estimatedPricePerSession > 0 && (
          <div
            style={{
              background: 'var(--color-surface-sunken, #F5F3EF)',
              padding: 12,
              borderRadius: 'var(--radius-md, 8px)',
              marginTop: 12,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography.Text type="secondary">Ước tính số tiền hoàn trả:</Typography.Text>
              <Typography.Text strong style={{ fontSize: 16, color: 'var(--color-primary-600, #0F766E)' }}>
                ~{estimatedRefund.toLocaleString('vi-VN')} ₫
              </Typography.Text>
            </div>
          </div>
        )}
      </Form>
    </Modal>
  );
};
