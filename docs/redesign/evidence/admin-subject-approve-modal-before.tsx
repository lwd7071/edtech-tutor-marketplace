'use client';

import React, { useEffect } from 'react';
import { Modal, Form, Input, Button, message, Select } from 'antd';
import { SubjectProposalSnapshot, ApproveSubjectProposalRequest } from '../types';
import { useApproveSubjectProposal } from '../hooks/useAdminApprovals';
import { isConcurrentModification, parseApiError } from '@/shared/backend';

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
        resolution: 'CREATE_NEW',
        code: '',
        name: currentName,
        educationLevel: currentCategory as ApproveSubjectProposalRequest['educationLevel'],
        description: proposal.description || '',
        version: proposal.version,
      });
    }
  }, [proposal, open, form, currentName, currentCategory]);

  const handleSubmit = async (values: ApproveSubjectProposalRequest) => {
    if (!proposal) return;
    const propId = proposal.proposalId || proposal.id || '';
    try {
      await approveMutation.mutateAsync({
        proposalId: propId,
        data: { ...values, version: proposal.version },
      });
      message.success('Phê duyệt môn học thành công');
      onClose();
    } catch (error) {
      message.error(isConcurrentModification(error)
        ? 'Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.'
        : parseApiError(error).message);
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
          resolution: 'CREATE_NEW',
          name: currentName,
          educationLevel: currentCategory,
          description: proposal?.description || '',
        }}
      >
        <Form.Item name="resolution" hidden><Input /></Form.Item>
        <Form.Item
          name="code"
          label="Mã môn học"
          rules={[{ required: true, message: 'Vui lòng nhập mã môn học' }, { pattern: /^[A-Z0-9_-]+$/, message: 'Dùng chữ in hoa, số, dấu gạch ngang hoặc gạch dưới' }]}
        >
          <Input placeholder="Ví dụ: MATH_12" style={{ textTransform: 'uppercase' }} />
        </Form.Item>
        <Form.Item
          name="name"
          label="Tên môn học chuẩn hóa"
          rules={[{ required: true, message: 'Vui lòng nhập tên môn học chuẩn hóa' }]}
        >
          <Input placeholder="Ví dụ: Toán học 12" />
        </Form.Item>

        <Form.Item
          name="educationLevel"
          label="Cấp học"
          rules={[{ required: true, message: 'Vui lòng chọn cấp học' }]}
        >
          <Select options={[
            { value: 'ELEMENTARY', label: 'Tiểu học' },
            { value: 'MIDDLE_SCHOOL', label: 'THCS' },
            { value: 'HIGH_SCHOOL', label: 'THPT' },
            { value: 'UNIVERSITY', label: 'Đại học' },
            { value: 'OTHER', label: 'Khác' },
          ]} />
        </Form.Item>

        <Form.Item name="description" label="Mô tả môn học">
          <Input.TextArea rows={3} placeholder="Mô tả chương trình học, mục tiêu..." />
        </Form.Item>
        <Form.Item name="note" label="Ghi chú cho gia sư">
          <Input.TextArea rows={2} placeholder="Thông tin cần lưu ý sau khi duyệt" />
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
