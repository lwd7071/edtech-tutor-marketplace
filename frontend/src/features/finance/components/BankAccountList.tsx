'use client';

import React, { useState } from 'react';
import { Card, Row, Col, Typography, Button, Tag, Space, Popconfirm, Empty } from 'antd';
import {
  BankOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { BankAccountView, UpsertBankAccountRequest } from '../types';
import { BankAccountModal } from './BankAccountModal';

interface BankAccountListProps {
  accounts?: BankAccountView[];
  loading?: boolean;
  onCreateAccount: (data: UpsertBankAccountRequest) => Promise<void> | void;
  onUpdateAccount: (id: string, data: UpsertBankAccountRequest) => Promise<void> | void;
  onDeleteAccount: (id: string) => Promise<void> | void;
}

export const BankAccountList: React.FC<BankAccountListProps> = ({
  accounts = [],
  loading = false,
  onCreateAccount,
  onUpdateAccount,
  onDeleteAccount,
}) => {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<BankAccountView | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const handleOpenCreate = () => {
    setEditingAccount(null);
    setModalOpen(true);
  };

  const handleOpenEdit = (account: BankAccountView) => {
    setEditingAccount(account);
    setModalOpen(true);
  };

  const handleSubmit = async (values: UpsertBankAccountRequest) => {
    setActionLoading(true);
    try {
      if (editingAccount) {
        await onUpdateAccount(editingAccount.id, values);
      } else {
        await onCreateAccount(values);
      }
      setModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div style={{ marginBottom: 24 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>
            Tài khoản ngân hàng
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Tài khoản dùng để nhận tiền khi thực hiện rút tiền từ ví giáo viên
          </Typography.Text>
        </div>

        <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate}>
          Thêm tài khoản
        </Button>
      </div>

      {accounts.length === 0 && !loading ? (
        <Card style={{ textAlign: 'center', padding: '32px 0' }}>
          <Empty
            description="Bạn chưa liên kết tài khoản ngân hàng nào"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate}>
              Thêm tài khoản ngay
            </Button>
          </Empty>
        </Card>
      ) : (
        <Row gutter={[16, 16]}>
          {accounts.map((acc) => (
            <Col xs={24} sm={12} md={8} key={acc.id}>
              <Card
                style={{
                  borderRadius: 'var(--radius-lg, 12px)',
                  border: acc.isDefault
                    ? '1px solid var(--color-primary-500, #14B8A6)'
                    : '1px solid var(--color-border, #E7E3DC)',
                  boxShadow: acc.isDefault ? 'var(--shadow-sm)' : undefined,
                }}
                actions={[
                  <Button
                    type="link"
                    key="edit"
                    icon={<EditOutlined />}
                    onClick={() => handleOpenEdit(acc)}
                  >
                    Sửa
                  </Button>,
                  <Popconfirm
                    key="delete"
                    title="Xác nhận xóa tài khoản này?"
                    okText="Xóa"
                    cancelText="Hủy"
                    okButtonProps={{ danger: true }}
                    onConfirm={() => onDeleteAccount(acc.id)}
                  >
                    <Button type="link" danger icon={<DeleteOutlined />}>
                      Xóa
                    </Button>
                  </Popconfirm>,
                ]}
              >
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography.Text strong style={{ fontSize: 15 }}>
                      <BankOutlined style={{ marginRight: 8, color: 'var(--color-primary-600, #0F766E)' }} />
                      {acc.bankName}
                    </Typography.Text>
                    {acc.isDefault && (
                      <Tag color="success" icon={<CheckCircleOutlined />}>
                        Mặc định
                      </Tag>
                    )}
                  </div>

                  <Typography.Text
                    copyable={{ text: acc.accountNumberMasked }}
                    style={{
                      fontSize: 16,
                      fontFamily: 'var(--font-mono)',
                      letterSpacing: '0.05em',
                      color: 'var(--color-text-primary, #1C1917)',
                    }}
                  >
                    {acc.accountNumberMasked}
                  </Typography.Text>

                  <Typography.Text type="secondary" style={{ fontSize: 13, textTransform: 'uppercase' }}>
                    {acc.accountHolderName}
                  </Typography.Text>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <BankAccountModal
        open={modalOpen}
        initialData={editingAccount}
        loading={actionLoading}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </div>
  );
};
