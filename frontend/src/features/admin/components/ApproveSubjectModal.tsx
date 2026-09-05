'use client';

import React, { useEffect } from 'react';
import { Modal, Form, Input, Button, message } from 'antd';
import { SubjectProposalSnapshot, ApproveSubjectProposalRequest } from '../types';
import { useApproveSubjectProposal } from '../hooks/useAdminApprovals';

interface ApproveSubjectModalProps {
  open: boolean;
  proposal: SubjectProposalSnapshot | null;
  onClose: () => void;
}

export const ApproveSubjectModal: React.FC<ApproveSubjectModalProps> = ({
  open,
  proposal,
  onClose,
}) => {
  const [form] = Form.useForm<ApproveSubjectProposalRequest>();
  const approveMutation = useApproveSubjectProposal();

  const currentName = proposal?.proposedName || proposal?.proposedSubjectName || '';
  const currentCategory = proposal?.educationLevel || proposal?.proposedCategory || '';

  useEffect(() => {
    if (proposal && open) {
      form.setFieldsValue({
        name: currentName,
        category: currentCategory,
        description: proposal.description || '',
      });
    }
  }, [proposal, open, form, currentName, currentCategory]);

  const handleSubmit = async (values: ApproveSubjectProposalRequest) => {
    if (!proposal) return;
    const propId = proposal.proposalId || proposal.id || '';
    try {
      await approveMutation.mutateAsync({
        proposalId: propId,
        data: values,
      });
      message.success('Phê duyệt môn học thành công');
      onClose();
    } catch {
      message.error('Phê duyệt môn học thất bại');
    }
  };

  return (
    <Modal
      title="Chuẩn hóa & Phê duyệt Môn học"
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          name: currentName,
          category: currentCategory,
          description: proposal?.description || '',
        }}
      >
        <Form.Item
          name="name"
          label="Tên môn học chuẩn hóa"
          rules={[{ required: true, message: 'Vui lòng nhập tên môn học chuẩn hóa' }]}
        >
          <Input placeholder="Ví dụ: Toán học 12" />
        </Form.Item>

        <Form.Item
          name="category"
          label="Danh mục môn học"
          rules={[{ required: true, message: 'Vui lòng nhập danh mục' }]}
        >
          <Input placeholder="Ví dụ: Toán học" />
        </Form.Item>

        <Form.Item name="description" label="Mô tả môn học">
          <Input.TextArea rows={3} placeholder="Mô tả chương trình học, mục tiêu..." />
        </Form.Item>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
          <Button onClick={onClose}>Hủy</Button>
          <Button type="primary" htmlType="submit" loading={approveMutation.isPending}>
            Phê duyệt & Tạo môn học
          </Button>
        </div>
      </Form>
    </Modal>
  );
};
