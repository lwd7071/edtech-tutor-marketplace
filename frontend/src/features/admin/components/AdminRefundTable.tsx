'use client';

import React, { useState } from 'react';
import { Table, Tag, Typography, Button, Space, Modal, Form, Input, Tabs, Popconfirm } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { CheckOutlined, CloseOutlined, EyeOutlined } from '@ant-design/icons';
import { RefundRequestView, RefundStatus } from '@/features/finance';
import { formatLedgerTime } from '@/features/finance';
import { getRefundStatusTag } from '@/features/finance';
import { ApproveRefundRequest, CompleteTransferRequest, RejectFinanceRequest } from '../types';

interface AdminRefundTableProps {
  refunds?: RefundRequestView[];
  loading?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, size: number) => void;
  onFilterStatus?: (status?: string) => void;
  onApproveRefund: (id: string, data: ApproveRefundRequest) => Promise<void> | void;
  onCompleteRefund: (id: string, data: CompleteTransferRequest) => Promise<void> | void;
  onRejectRefund: (id: string, data: RejectFinanceRequest) => Promise<void> | void;
}

export const AdminRefundTable: React.FC<AdminRefundTableProps> = ({
  refunds = [],
  loading = false,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
  onFilterStatus,
  onApproveRefund,
  onCompleteRefund,
  onRejectRefund,
}) => {
  const [approveModalOpen, setApproveModalOpen] = useState(false);
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [selectedRefund, setSelectedRefund] = useState<RefundRequestView | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [approveForm] = Form.useForm<Omit<ApproveRefundRequest, 'version'>>();
  const [completeForm] = Form.useForm<Omit<CompleteTransferRequest, 'version'>>();
  const [rejectForm] = Form.useForm<Omit<RejectFinanceRequest, 'version'>>();

  const handleOpenApprove = (refund: RefundRequestView) => {
    setSelectedRefund(refund);
    approveForm.resetFields();
    approveForm.setFieldsValue({ approvedSessions: refund.requestedSessions });
    setApproveModalOpen(true);
  };

  const handleOpenComplete = (refund: RefundRequestView) => {
    setSelectedRefund(refund);
    completeForm.resetFields();
    setCompleteModalOpen(true);
  };

  const handleOpenReject = (refund: RefundRequestView) => {
    setSelectedRefund(refund);
    rejectForm.resetFields();
    setRejectModalOpen(true);
  };

  const handleApproveSubmit = async () => {
    if (!selectedRefund) return;
    try {
      const values = await approveForm.validateFields();
      setActionLoading(true);
      await onApproveRefund(selectedRefund.id, { ...values, version: selectedRefund.version });
      setApproveModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const handleCompleteSubmit = async () => {
    if (!selectedRefund) return;
    try {
      const values = await completeForm.validateFields();
      setActionLoading(true);
      await onCompleteRefund(selectedRefund.id, { ...values, version: selectedRefund.version });
      setCompleteModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!selectedRefund) return;
    try {
      const values = await rejectForm.validateFields();
      setActionLoading(true);
      await onRejectRefund(selectedRefund.id, { ...values, version: selectedRefund.version });
      setRejectModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const columns: ColumnsType<RefundRequestView> = [
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
      title: 'Số buổi / Số tiền hoàn',
      key: 'amount',
      width: 180,
      render: (_, record) => (
        <div>
          <div>
            <strong>{record.requestedSessions}</strong> buổi
            {record.approvedSessions ? ` (duyệt ${record.approvedSessions})` : ''}
          </div>
          <Typography.Text strong style={{ color: 'var(--color-primary-600, #0F766E)', fontSize: 14 }}>
            {record.refundAmountVnd ? `${record.refundAmountVnd.toLocaleString('vi-VN')} ₫` : 'Chờ tính toán'}
          </Typography.Text>
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: RefundStatus) => getRefundStatusTag(status),
    },
    {
      title: 'Tài khoản nhận',
      key: 'bank',
      width: 180,
      render: (_, record) => (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography.Text style={{ fontSize: 13, fontWeight: 500 }}>
            {record.bankName || 'Ngân hàng'}
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12, fontFamily: 'var(--font-mono)' }}>
            {record.accountNumberMasked} ({record.accountHolderName})
          </Typography.Text>
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
                title="Duyệt yêu cầu hoàn tiền này?"
                description="Hệ thống sẽ tính toán số tiền hoàn dựa trên số buổi chưa học."
                okText="Duyệt"
                cancelText="Hủy"
                onConfirm={() => handleOpenApprove(record)}
              >
                <Button type="primary" size="small" icon={<CheckOutlined />}>
                  Duyệt
                </Button>
              </Popconfirm>
              <Button danger size="small" icon={<CloseOutlined />} onClick={() => handleOpenReject(record)}>
                Từ chối
              </Button>
            </Space>
          );
        }

        if (record.status === 'APPROVED') {
          return (
            <Space size={8}>
              <Button
                type="primary"
                size="small"
                style={{ background: 'var(--color-success-600, #15803D)' }}
                icon={<CheckOutlined />}
                onClick={() => handleOpenComplete(record)}
              >
                Xác nhận hoàn
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
          { key: 'APPROVED', label: 'Đã duyệt' },
          { key: 'REFUNDED', label: 'Đã hoàn tiền' },
          { key: 'REJECTED', label: 'Bị từ chối' },
        ]}
      />

      <Table<RefundRequestView>
        rowKey="id"
        columns={columns}
        dataSource={refunds}
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

      {/* Modal Xác nhận đã chuyển tiền hoàn */}
      <Modal
        open={approveModalOpen}
        title={`Duyệt yêu cầu hoàn tiền #${selectedRefund?.id.slice(0, 8)}`}
        okText="Duyệt"
        cancelText="Hủy"
        confirmLoading={actionLoading}
        onCancel={() => setApproveModalOpen(false)}
        onOk={handleApproveSubmit}
      >
        <Form form={approveForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="Số buổi duyệt hoàn"
            name="approvedSessions"
            rules={[{ required: true, type: 'number', min: 1, max: selectedRefund?.requestedSessions }]}
          >
            <Input type="number" min={1} max={selectedRefund?.requestedSessions} />
          </Form.Item>
          <Form.Item label="Ghi chú admin" name="adminNote">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={completeModalOpen}
        title={`Xác nhận chuyển tiền hoàn #${selectedRefund?.id.slice(0, 8)}`}
        okText="Xác nhận hoàn tất"
        cancelText="Hủy"
        confirmLoading={actionLoading}
        onCancel={() => setCompleteModalOpen(false)}
        onOk={handleCompleteSubmit}
      >
        <Form form={completeForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="Mã giao dịch ngân hàng"
            name="bankReference"
            rules={[{ required: true, message: 'Vui lòng nhập mã tham chiếu ngân hàng' }]}
          >
            <Input placeholder="Ví dụ: REF2609041234" />
          </Form.Item>

          <Form.Item label="Đường dẫn chứng từ ủy nhiệm chi" name="proofUrl">
            <Input placeholder="https://..." />
          </Form.Item>

        </Form>
      </Modal>

      {/* Modal Từ chối hoàn tiền */}
      <Modal
        open={rejectModalOpen}
        title={`Từ chối yêu cầu hoàn tiền #${selectedRefund?.id.slice(0, 8)}`}
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
            <Input.TextArea rows={3} placeholder="Ví dụ: Gói học đã vượt quá thời hạn cho phép hoàn tiền..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
