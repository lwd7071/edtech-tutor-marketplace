'use client';

import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, Switch } from 'antd';
import { UpsertBankAccountRequest, BankAccountView } from '../types';

interface BankAccountModalProps {
  open: boolean;
  initialData?: BankAccountView | null;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: UpsertBankAccountRequest) => Promise<void> | void;
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

export const BankAccountModal: React.FC<BankAccountModalProps> = ({
  open,
  initialData,
  loading = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<UpsertBankAccountRequest>();

  useEffect(() => {
    if (open) {
      if (initialData) {
        form.setFieldsValue({
          bankBin: initialData.bankBin,
          bankName: initialData.bankName,
          accountNumber: '',
          accountHolderName: initialData.accountHolderName,
          isDefault: initialData.isDefault,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ isDefault: false });
      }
    }
  }, [open, initialData, form]);

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
        accountHolderName: values.accountHolderName.trim().toUpperCase(),
      });
    } catch {
      // Form validation error
    }
  };

  return (
    <Modal
      open={open}
      title={initialData ? 'Chỉnh sửa tài khoản ngân hàng' : 'Thêm tài khoản ngân hàng'}
      okText="Lưu tài khoản"
      cancelText="Hủy"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item
          label="Ngân hàng"
          name="bankBin"
          rules={[{ required: true, message: 'Vui lòng chọn ngân hàng' }]}
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
            { required: true, message: 'Vui lòng nhập số tài khoản' },
            { pattern: /^[0-9A-Za-z]{6,25}$/, message: 'Số tài khoản không hợp lệ (từ 6-25 ký tự số)' },
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

        <Form.Item label="Đặt làm tài khoản mặc định" name="isDefault" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
};
