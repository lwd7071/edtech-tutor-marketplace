'use client';

import React, { useState } from 'react';
import { Tag, Button, Space, Typography, Card, Modal, Input, message } from 'antd';
import { CheckOutlined, CloseOutlined } from '@ant-design/icons';
import ResponsiveTable from '@/shared/components/ui/ResponsiveTable';
import { SubjectProposalSnapshot } from '../types';
import { useSubjectProposals, useRejectSubjectProposal } from '../hooks/useAdminApprovals';
import { ApproveSubjectModal } from './ApproveSubjectModal';

export function SubjectProposalTable() {
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(10);
  const [selectedProposal, setSelectedProposal] = useState<SubjectProposalSnapshot | null>(null);
  const [approveModalOpen, setApproveModalOpen] = useState<boolean>(false);
  const [rejectModalOpen, setRejectModalOpen] = useState<boolean>(false);
  const [rejectionReason, setRejectionReason] = useState<string>('');

  const { data, isLoading } = useSubjectProposals(page, pageSize);
  const rejectMutation = useRejectSubjectProposal();

  const proposals = data?.data || [];
  const total = data?.meta?.totalElements || proposals.length;

  const handleOpenApprove = (record: SubjectProposalSnapshot) => {
    setSelectedProposal(record);
    setApproveModalOpen(true);
  };

  const handleOpenReject = (record: SubjectProposalSnapshot) => {
    setSelectedProposal(record);
    setRejectionReason('');
    setRejectModalOpen(true);
  };

  const handleConfirmReject = async () => {
    if (!selectedProposal) return;
    if (!rejectionReason.trim()) {
      message.error('Vui lòng nhập lý do từ chối đề xuất');
      return;
    }
    const propId = selectedProposal.proposalId || selectedProposal.id || '';
    try {
      await rejectMutation.mutateAsync({
        proposalId: propId,
        data: {
          reason: rejectionReason.trim(),
        },
      });
      message.success('Đã từ chối đề xuất môn học');
      setRejectModalOpen(false);
      setSelectedProposal(null);
    } catch {
      message.error('Từ chối đề xuất thất bại');
    }
  };

  const columns = [
    {
      title: 'Tên môn đề xuất',
      key: 'proposedName',
      render: (_: unknown, record: SubjectProposalSnapshot) => (
        <Typography.Text strong>
          {record.proposedName || record.proposedSubjectName}
        </Typography.Text>
      ),
    },
    {
      title: 'Danh mục',
      key: 'educationLevel',
      render: (_: unknown, record: SubjectProposalSnapshot) =>
        record.educationLevel || record.proposedCategory || '---',
    },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      key: 'description',
      render: (text?: string) => text || '---',
    },
    {
      title: 'Gia sư đề xuất',
      dataIndex: 'teacherName',
      key: 'teacherName',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        switch (status) {
          case 'APPROVED':
            return <Tag color="success">Đã duyệt</Tag>;
          case 'REJECTED':
            return <Tag color="error">Từ chối</Tag>;
          case 'PENDING':
          default:
            return <Tag color="warning">Chờ duyệt</Tag>;
        }
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_: unknown, record: SubjectProposalSnapshot) => (
        <Space size="small">
          {record.status === 'PENDING' && (
            <>
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => handleOpenApprove(record)}
              >
                Phê duyệt
              </Button>
              <Button
                danger
                size="small"
                icon={<CloseOutlined />}
                onClick={() => handleOpenReject(record)}
              >
                Từ chối
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card title="Quản lý Đề xuất Môn học mới" style={{ width: '100%' }}>
      <ResponsiveTable<SubjectProposalSnapshot>
        rowKey={(record) => record.proposalId || record.id || ''}
        columns={columns}
        dataSource={proposals}
        loading={isLoading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          onChange: (p, ps) => {
            setPage(p - 1);
            setPageSize(ps);
          },
          showSizeChanger: true,
        }}
      />

      <ApproveSubjectModal
        open={approveModalOpen}
        proposal={selectedProposal}
        onClose={() => {
          setApproveModalOpen(false);
          setSelectedProposal(null);
        }}
      />

      <Modal
        title="Từ chối Đề xuất Môn học"
        open={rejectModalOpen}
        onCancel={() => {
          setRejectModalOpen(false);
          setSelectedProposal(null);
        }}
        onOk={handleConfirmReject}
        confirmLoading={rejectMutation.isPending}
        okText="Xác nhận từ chối"
        cancelText="Hủy"
        okButtonProps={{ danger: true }}
        destroyOnHidden
      >
        <Typography.Paragraph>
          Vui lòng nhập lý do từ chối đề xuất môn học <strong>{selectedProposal?.proposedName || selectedProposal?.proposedSubjectName}</strong>:
        </Typography.Paragraph>
        <Input.TextArea
          rows={4}
          placeholder="Lý do từ chối..."
          value={rejectionReason}
          onChange={(e) => setRejectionReason(e.target.value)}
        />
      </Modal>
    </Card>
  );
}
