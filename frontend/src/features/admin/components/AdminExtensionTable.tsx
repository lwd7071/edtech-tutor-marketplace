'use client';

import React, { useState } from 'react';
import { Table, Tag, Typography, Button, Space, Modal, Form, Input, DatePicker, Tabs } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { ExtensionRequestView, ExtensionStatus } from '@/features/finance';
import { formatLedgerTime } from '@/features/finance';
import { getExtensionStatusTag } from '@/features/finance';
import { ApproveExtensionRequest, RejectRequest } from '../types';

interface AdminExtensionTableProps {
  extensions?: ExtensionRequestView[];
  loading?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number, size: number) => void;
  onFilterStatus?: (status?: string) => void;
  onApproveExtension: (id: string, data: ApproveExtensionRequest) => Promise<void> | void;
  onRejectExtension: (id: string, data: RejectRequest) => Promise<void> | void;
}

export const AdminExtensionTable: React.FC<AdminExtensionTableProps> = ({
  extensions = [],
  loading = false,
  total = 0,
  page = 0,
  pageSize = 20,
  onPageChange,
  onFilterStatus,
  onApproveExtension,
  onRejectExtension,
}) => {
  const [approveModalOpen, setApproveModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [selectedExtension, setSelectedExtension] = useState<ExtensionRequestView | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [approveForm] = Form.useForm<{ date: Dayjs; adminNote?: string }>();
  const [rejectForm] = Form.useForm<RejectRequest>();

  const handleOpenApprove = (extension: ExtensionRequestView) => {
    setSelectedExtension(extension);
    approveForm.resetFields();
    if (extension.requestedExpiryDate) {
      approveForm.setFieldsValue({ date: dayjs(extension.requestedExpiryDate) });
    }
    setApproveModalOpen(true);
  };

  const handleOpenReject = (extension: ExtensionRequestView) => {
    setSelectedExtension(extension);
    rejectForm.resetFields();
    setRejectModalOpen(true);
  };

  const handleApproveSubmit = async () => {
    if (!selectedExtension) return;
    try {
      const values = await approveForm.validateFields();
      setActionLoading(true);
      await onApproveExtension(selectedExtension.id, {
        approvedExpiryDate: values.date.toISOString(),
        adminNote: values.adminNote,
      });
      setApproveModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!selectedExtension) return;
    try {
      const values = await rejectForm.validateFields();
      setActionLoading(true);
      await onRejectExtension(selectedExtension.id, values);
      setRejectModalOpen(false);
    } finally {
      setActionLoading(false);
    }
  };

  const columns: ColumnsType<ExtensionRequestView> = [
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
      title: 'Thời gian gửi',
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
      title: 'Hạn xin gia hạn',
      dataIndex: 'requestedExpiryDate',
      key: 'requestedExpiryDate',
      width: 160,
      render: (val: string) => (
        <Typography.Text strong style={{ fontSize: 13 }}>
          {formatLedgerTime(val)}
        </Typography.Text>
      ),
    },
    {
      title: 'Hạn được duyệt',
      dataIndex: 'approvedExpiryDate',
      key: 'approvedExpiryDate',
      width: 160,
      render: (val: string | null) => (
        <Typography.Text
          style={{
            fontSize: 13,
            color: val ? 'var(--color-success-600, #15803D)' : undefined,
            fontWeight: val ? 600 : undefined,
          }}
        >
          {val ? formatLedgerTime(val) : '—'}
        </Typography.Text>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      render: (status: ExtensionStatus) => getExtensionStatusTag(status),
    },
    {
      title: 'Lý do xin gia hạn',
      dataIndex: 'reason',
      key: 'reason',
      render: (reason: string) => (
        <Typography.Text type="secondary" style={{ fontSize: 13 }}>
          {reason || '—'}
        </Typography.Text>
      ),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => {
        if (record.status === 'PENDING') {
          return (
            <Space size={8}>
              <Button type="primary" size="small" icon={<CheckOutlined />} onClick={() => handleOpenApprove(record)}>
                Duyệt
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
          { key: 'APPROVED', label: 'Đã gia hạn' },
          { key: 'REJECTED', label: 'Bị từ chối' },
        ]}
      />

      <Table<ExtensionRequestView>
        rowKey="id"
        columns={columns}
        dataSource={extensions}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showSizeChanger: true,
          onChange: (p, s) => onPageChange?.(p - 1, s),
        }}
        scroll={{ x: 900 }}
      />

      {/* Modal Duyệt gia hạn */}
      <Modal
        open={approveModalOpen}
        title={`Duyệt gia hạn gói học #${selectedExtension?.id.slice(0, 8)}`}
        okText="Xác nhận gia hạn"
        cancelText="Hủy"
        confirmLoading={actionLoading}
        onCancel={() => setApproveModalOpen(false)}
        onOk={handleApproveSubmit}
      >
        <Form form={approveForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="Ngày hết hạn mới được duyệt"
            name="date"
            rules={[{ required: true, message: 'Vui lòng chọn ngày hết hạn mới' }]}
          >
            <DatePicker
              style={{ width: '100%' }}
              format="DD/MM/YYYY"
              disabledDate={(current) => current && current < dayjs().endOf('day')}
            />
          </Form.Item>

          <Form.Item label="Ghi chú admin" name="adminNote">
            <Input.TextArea rows={2} placeholder="Đã duyệt gia hạn gói học theo đề xuất..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal Từ chối gia hạn */}
      <Modal
        open={rejectModalOpen}
        title={`Từ chối gia hạn #${selectedExtension?.id.slice(0, 8)}`}
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
            <Input.TextArea rows={3} placeholder="Ví dụ: Gói học đã hết hạn quá 30 ngày..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
