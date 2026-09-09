'use client';

import React, { useState } from 'react';
import { Table, Tag, Typography, Button, Space, Modal, Form, Input, Tabs, Popconfirm } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { CheckOutlined, CloseOutlined, SyncOutlined, EyeOutlined } from '@ant-design/icons';
import { PayoutRequestView, PayoutStatus } from '@/features/finance';
import { formatLedgerTime } from '@/features/finance';
import { CompleteTransferRequest, RejectRequest } from '../types';

interface AdminPayoutTableProps {
  payouts?: PayoutRequestView[];
  loading?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, size: number) => void;
  onFilterStatus?: (status?: string) => void;
  onProcessPayout: (id: string) => Promise<void> | void;
  onCompletePayout: (id: string, data: CompleteTransferRequest) => Promise<void> | void;
  onRejectPayout: (id: string, data: RejectRequest) => Promise<void> | void;
}

export const AdminPayoutTable: React.FC<AdminPayoutTableProps> = ({
  payouts = [],
  loading = false,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
  onFilterStatus,
  onProcessPayout,
  onCompletePayout,
  onRejectPayout,
}) => {
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [selectedPayout, setSelectedPayout] = useState<PayoutRequestView | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [completeForm] = Form.useForm<CompleteTransferRequest>();
  const [rejectForm] = Form.useForm<RejectRequest>();

  const handleOpenComplete = (payout: PayoutRequestView) => {
    setSelectedPayout(payout);
    completeForm.resetFields();
    setCompleteModalOpen(true);
  };

  const handleOpenReject = (payout: PayoutRequestView) => {
    setSelectedPayout(payout);
    rejectForm.resetFields();
    setRejectModalOpen(true);
  };

  const handleCompleteSubmit = async () => {
    if (!selectedPayout) return;
    try {
      const values = await completeForm.validateFields();
      setActionLoading(true);
      await onCompletePayout(selectedPayout.id, values);
      setCompleteModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!selectedPayout) return;
    try {
      const values = await rejectForm.validateFields();
      setActionLoading(true);
      await onRejectPayout(selectedPayout.id, values);
      setRejectModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const columns: ColumnsType<PayoutRequestView> = [
    {
      title: 'Mã yêu cầu',
      dataIndex: 'id',
      key: 'id',
      width: 120,
      render: (id: string) => (
        <Typography.Text copyable={{ text: id }} style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
          #{id.slice(0, 8)}
        </Typography.Text>
      ),
    },
    {
      title: 'Thời gian tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (val: string) => (
        <Typography.Text style={{ fontSize: 13, color: 'var(--color-text-secondary, #57534E)' }}>
          {formatLedgerTime(val)}
        </Typography.Text>
      ),
    },
    {
      title: 'Số tiền rút',
      dataIndex: 'amountVnd',
      key: 'amountVnd',
      width: 150,
      align: 'right',
      render: (amt: number) => (
        <span
          style={{
            fontWeight: 700,
            fontSize: 15,
            fontVariantNumeric: 'tabular-nums',
            color: 'var(--color-primary-600, #0F766E)',
          }}
        >
          {amt.toLocaleString('vi-VN')} ₫
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: PayoutStatus) => {
        switch (status) {
          case 'PENDING':
            return <Tag color="warning">Chờ duyệt</Tag>;
          case 'PROCESSING':
            return <Tag color="processing">Đang xử lý</Tag>;
          case 'SUCCEEDED':
            return <Tag color="success">Đã chuyển</Tag>;
          case 'REJECTED':
            return <Tag color="default">Bị từ chối</Tag>;
          case 'FAILED':
            return <Tag color="error">Thất bại</Tag>;
          default:
            return <Tag>{status}</Tag>;
        }
      },
    },
    {
      title: 'Mã GD / Biên lai',
      key: 'ref',
      width: 160,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {record.bankReference && (
            <Typography.Text style={{ fontSize: 12, fontFamily: 'var(--font-mono)' }}>
              {record.bankReference}
            </Typography.Text>
          )}
          {record.proofUrl && (
            <a href={record.proofUrl} target="_blank" rel="noopener noreferrer">
              <Button type="link" size="small" icon={<EyeOutlined />} style={{ padding: 0 }}>
                Xem chứng từ
              </Button>
            </a>
          )}
          {!record.bankReference && !record.proofUrl && <Typography.Text type="secondary">—</Typography.Text>}
        </div>
      ),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_, record) => {
        if (record.status === 'PENDING') {
          return (
            <Space size={8}>
              <Popconfirm
                title="Bắt đầu xử lý lệnh rút này?"
                description="Lệnh rút sẽ chuyển sang PROCESSING để kế toán chuyển khoản ngân hàng."
                okText="Xử lý"
                cancelText="Hủy"
                onConfirm={() => onProcessPayout(record.id)}
              >
                <Button type="primary" size="small" icon={<SyncOutlined />}>
                  Xử lý
                </Button>
              </Popconfirm>
              <Button danger size="small" icon={<CloseOutlined />} onClick={() => handleOpenReject(record)}>
                Từ chối
              </Button>
            </Space>
          );
        }

        if (record.status === 'PROCESSING') {
          return (
            <Space size={8}>
              <Button
                type="primary"
                size="small"
                style={{ background: 'var(--color-success-600, #15803D)' }}
                icon={<CheckOutlined />}
                onClick={() => handleOpenComplete(record)}
              >
                Xác nhận chuyển
              </Button>
              <Button danger size="small" icon={<CloseOutlined />} onClick={() => handleOpenReject(record)}>
                Từ chối
              </Button>
            </Space>
          );
        }

        return <Typography.Text type="secondary">—</Typography.Text>;
      },
    },
  ];

  return (
    <div style={{ background: 'var(--color-surface, #FFFFFF)', borderRadius: 'var(--radius-lg, 12px)', padding: 20 }}>
      <Tabs
        defaultActiveKey="ALL"
        onChange={(key) => onFilterStatus?.(key === 'ALL' ? undefined : key)}
        items={[
          { key: 'ALL', label: 'Tất cả' },
          { key: 'PENDING', label: 'Chờ duyệt' },
          { key: 'PROCESSING', label: 'Đang xử lý' },
          { key: 'SUCCEEDED', label: 'Đã chuyển' },
          { key: 'REJECTED', label: 'Bị từ chối' },
        ]}
      />

      <Table<PayoutRequestView>
        rowKey="id"
        columns={columns}
        dataSource={payouts}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showSizeChanger: true,
          onChange: (p, s) => onPageChange?.(p - 1, s),
        }}
        scroll={{ x: 950 }}
      />

      {/* Modal Xác nhận chuyển tiền */}
      <Modal
        open={completeModalOpen}
        title={`Xác nhận chuyển tiền cho lệnh #${selectedPayout?.id.slice(0, 8)}`}
        okText="Xác nhận thành công"
        cancelText="Hủy"
        confirmLoading={actionLoading}
        onCancel={() => setCompleteModalOpen(false)}
        onOk={handleCompleteSubmit}
      >
        <Form form={completeForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="Mã giao dịch ngân hàng (FT / Ref Code)"
            name="bankReference"
            rules={[{ required: true, message: 'Vui lòng nhập mã tham chiếu giao dịch ngân hàng' }]}
          >
            <Input placeholder="Ví dụ: FT260904123456" />
          </Form.Item>

          <Form.Item label="Đường dẫn chứng từ ủy nhiệm chi (Proof URL)" name="proofUrl">
            <Input placeholder="https://..." />
          </Form.Item>

          <Form.Item label="Ghi chú admin (không bắt buộc)" name="adminNote">
            <Input.TextArea rows={2} placeholder="Đã chuyển khoản thành công..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal Từ chối rút tiền */}
      <Modal
        open={rejectModalOpen}
        title={`Từ chối lệnh rút tiền #${selectedPayout?.id.slice(0, 8)}`}
        okText="Xác nhận từ chối"
        okButtonProps={{ danger: true }}
        cancelText="Hủy"
        confirmLoading={actionLoading}
        onCancel={() => setRejectModalOpen(false)}
        onOk={handleRejectSubmit}
      >
        <Form form={rejectForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="Lý do từ chối (bắt buộc)"
            name="reason"
            rules={[{ required: true, message: 'Vui lòng nhập lý do từ chối' }]}
          >
            <Input.TextArea rows={3} placeholder="Ví dụ: Thông tin tài khoản ngân hàng không khớp..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
