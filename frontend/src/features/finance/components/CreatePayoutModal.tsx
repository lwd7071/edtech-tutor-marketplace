'use client';

import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Select, Alert, Space, Typography, Button } from 'antd';
import { CreatePayoutRequest, BankAccountView } from '../types';

interface CreatePayoutModalProps {
  open: boolean;
  availableBalanceVnd: number;
  bankAccounts?: BankAccountView[];
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: CreatePayoutRequest) => Promise<void> | void;
}

export const CreatePayoutModal: React.FC<CreatePayoutModalProps> = ({
  open,
  availableBalanceVnd,
  bankAccounts = [],
  loading = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<CreatePayoutRequest>();
  const defaultAccount = bankAccounts.find((b) => b.isDefault) || bankAccounts[0];

  useEffect(() => {
    if (open) {
      form.resetFields();
      if (defaultAccount) {
        form.setFieldsValue({ bankAccountId: defaultAccount.id });
      }
    }
  }, [open, defaultAccount, form]);

  const watchedAmount = Form.useWatch('amountVnd', form) || 0;

  const handleSetAmount = (amt: number) => {
    const validAmount = Math.min(amt, availableBalanceVnd);
    form.setFieldsValue({ amountVnd: validAmount });
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit(values);
    } catch {
      // Validation error
    }
  };

  return (
    <Modal
      open={open}
      title="Tạo yêu cầu rút tiền"
      okText="Xác nhận rút tiền"
      cancelText="Hủy"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnClose
    >
      <div style={{ margin: '16px 0' }}>
        <Alert
          type="info"
          showIcon
          title="Thông tin số dư khả dụng"
          description={
            <div style={{ marginTop: 4 }}>
              Số dư khả dụng hiện tại: <strong style={{ color: 'var(--color-primary-600, #0F766E)' }}>{availableBalanceVnd.toLocaleString('vi-VN')} ₫</strong>.
              Tiền sẽ được giữ ở trạng thái "Đang rút" cho đến khi Admin duyệt chuyển khoản.
            </div>
          }
        />
      </div>

      <Form form={form} layout="vertical">
        <Form.Item
          label="Tài khoản thụ hưởng"
          name="bankAccountId"
          rules={[{ required: true, message: 'Vui lòng chọn tài khoản nhận tiền' }]}
        >
          <Select
            placeholder="Chọn tài khoản ngân hàng"
            options={bankAccounts.map((b) => ({
              label: `${b.bankName} - ${b.accountNumberMasked} (${b.accountHolderName})${b.isDefault ? ' [Mặc định]' : ''}`,
              value: b.id,
            }))}
          />
        </Form.Item>

        <Form.Item
          label="Số tiền muốn rút (VND)"
          name="amountVnd"
          rules={[
            { required: true, message: 'Vui lòng nhập số tiền muốn rút' },
            {
              type: 'number',
              min: 50000,
              message: 'Số tiền rút tối thiểu là 50.000 ₫',
            },
            {
              type: 'number',
              max: availableBalanceVnd,
              message: `Số tiền vượt quá số dư khả dụng (${availableBalanceVnd.toLocaleString('vi-VN')} ₫)`,
            },
          ]}
        >
          <InputNumber
            style={{ width: '100%' }}
            formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
            parser={(value) => Number(value?.replace(/\./g, '') || 0)}
            placeholder="Tối thiểu 50.000 ₫"
            step={50000}
          />
        </Form.Item>

        <Space size={8} style={{ marginBottom: 16 }}>
          <Button size="small" onClick={() => handleSetAmount(200000)}>
            200.000 ₫
          </Button>
          <Button size="small" onClick={() => handleSetAmount(500000)}>
            500.000 ₫
          </Button>
          <Button size="small" onClick={() => handleSetAmount(1000000)}>
            1.000.000 ₫
          </Button>
          <Button size="small" type="dashed" onClick={() => handleSetAmount(availableBalanceVnd)}>
            Rút toàn bộ
          </Button>
        </Space>

        <Form.Item label="Ghi chú cho ban quản trị (không bắt buộc)" name="teacherNote">
          <Input.TextArea rows={2} placeholder="Nhập ghi chú hoặc yêu cầu đặc biệt..." />
        </Form.Item>

        {/* Tóm tắt giao dịch */}
        <div
          style={{
            background: 'var(--color-surface-sunken, #F5F3EF)',
            padding: 12,
            borderRadius: 'var(--radius-md, 8px)',
            marginTop: 8,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
            <Typography.Text type="secondary">Phí rút tiền:</Typography.Text>
            <Typography.Text strong style={{ color: 'var(--color-success-600, #15803D)' }}>
              0 ₫ (Miễn phí)
            </Typography.Text>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <Typography.Text type="secondary">Thực nhận về tài khoản:</Typography.Text>
            <Typography.Text strong style={{ fontSize: 16, color: 'var(--color-primary-600, #0F766E)' }}>
              {watchedAmount.toLocaleString('vi-VN')} ₫
            </Typography.Text>
          </div>
        </div>
      </Form>
    </Modal>
  );
};
