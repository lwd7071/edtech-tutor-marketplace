'use client';

import React, { useState } from 'react';
import { Drawer, Descriptions, Tag, Button, Space, Typography, List, Input, Divider, message, Card } from 'antd';
import { TeacherApprovalSnapshot } from '../types';
import { useApproveTeacher, useRejectTeacher } from '../hooks/useAdminApprovals';
import { parseApiError } from '@/shared/api/types';

interface TeacherDetailDrawerProps {
  open: boolean;
  teacher: TeacherApprovalSnapshot | null;
  onClose: () => void;
}

export function TeacherDetailDrawer({ open, teacher, onClose }: TeacherDetailDrawerProps) {
  const [rejectReason, setRejectReason] = useState('');
  const [approvalNote, setApprovalNote] = useState('');
  const [showRejectInput, setShowRejectInput] = useState(false);

  const approveMutation = useApproveTeacher();
  const rejectMutation = useRejectTeacher();

  if (!teacher) return null;

  const handleApprove = async () => {
    try {
      await approveMutation.mutateAsync({
        teacherId: teacher.teacherProfileId,
        data: { note: approvalNote },
      });
      message.success('Đã phê duyệt hồ sơ giáo viên thành công');
      onClose();
    } catch (err) {
      const parsed = parseApiError(err);
      message.error(parsed.message);
    }
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) {
      message.warning('Vui lòng nhập lý do từ chối hồ sơ');
      return;
    }

    try {
      await rejectMutation.mutateAsync({
        teacherId: teacher.teacherProfileId,
        data: { reason: rejectReason },
      });
      message.success('Đã từ chối hồ sơ giáo viên');
      setShowRejectInput(false);
      setRejectReason('');
      onClose();
    } catch (err) {
      const parsed = parseApiError(err);
      message.error(parsed.message);
    }
  };

  const renderStatusTag = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return <Tag color="success">Đã duyệt</Tag>;
      case 'REJECTED':
        return <Tag color="error">Đã từ chối</Tag>;
      case 'PENDING_APPROVAL':
        return <Tag color="warning">Chờ duyệt</Tag>;
      default:
        return <Tag color="default">{status}</Tag>;
    }
  };

  return (
    <Drawer
      title="Chi tiết Hồ sơ Giáo viên"
      open={open}
      onClose={onClose}
      size="large"
      footer={
        teacher.status === 'PENDING_APPROVAL' ? (
          <Space style={{ display: 'flex', justifyContent: 'flex-end', width: '100%' }}>
            <Button onClick={onClose}>Đóng</Button>
            <Button danger onClick={() => setShowRejectInput(!showRejectInput)}>
              {showRejectInput ? 'Hủy từ chối' : 'Từ chối'}
            </Button>
            <Button type="primary" loading={approveMutation.isPending} onClick={handleApprove}>
              Phê duyệt
            </Button>
          </Space>
        ) : (
          <Button onClick={onClose} style={{ float: 'right' }}>
            Đóng
          </Button>
        )
      }
    >
      <Descriptions bordered column={1} size="small">
        <Descriptions.Item label="Họ và tên">{teacher.fullName || 'Chưa cập nhật'}</Descriptions.Item>
        <Descriptions.Item label="Email">{teacher.email || 'Chưa cập nhật'}</Descriptions.Item>
        <Descriptions.Item label="Trạng thái">{renderStatusTag(teacher.status)}</Descriptions.Item>
        <Descriptions.Item label="Học vấn">{teacher.education || 'Chưa cập nhật'}</Descriptions.Item>
        <Descriptions.Item label="Kinh nghiệm">{teacher.experienceYears ? `${teacher.experienceYears} năm` : 'Chưa cập nhật'}</Descriptions.Item>
        <Descriptions.Item label="Tiểu sử">{teacher.bio || 'Chưa có tiểu sử giới thiệu'}</Descriptions.Item>
      </Descriptions>

      {teacher.rejectionReason && (
        <Card title="Lý do từ chối trước đó" style={{ marginTop: 'var(--space-4)', borderColor: 'var(--color-error-300)' }}>
          <Typography.Text type="danger">{teacher.rejectionReason}</Typography.Text>
        </Card>
      )}

      <Divider titlePlacement="left">Danh sách Bằng cấp & Chứng chỉ</Divider>
      <List
        dataSource={teacher.documents}
        renderItem={(doc) => (
          <List.Item
            actions={[
              <Button type="link" key="view" href={doc.secureUrl} target="_blank" rel="noopener noreferrer">
                Xem tài liệu
              </Button>,
            ]}
          >
            <List.Item.Meta
              title={doc.title}
              description={`Loại: ${doc.type} · Dung lượng: ${(doc.fileSize / 1024).toFixed(0)} KB`}
            />
          </List.Item>
        )}
        locale={{ emptyText: 'Chưa có chứng chỉ hoặc tài liệu nào được đính kèm.' }}
      />

      {showRejectInput && (
        <div style={{ marginTop: 'var(--space-6)', padding: 'var(--space-4)', background: 'var(--color-surface-hover)', borderRadius: 'var(--radius-md)' }}>
          <Typography.Title level={5} type="danger">
            Nhập lý do từ chối hồ sơ (bắt buộc)
          </Typography.Title>
          <Input.TextArea
            rows={3}
            placeholder="Ví dụ: Thiếu chứng chỉ sư phạm phù hợp, ảnh chứng minh nhân dân mờ..."
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
          <Button
            type="primary"
            danger
            style={{ marginTop: 'var(--space-3)' }}
            loading={rejectMutation.isPending}
            onClick={handleReject}
          >
            Xác nhận Từ chối Hồ sơ
          </Button>
        </div>
      )}

      {teacher.status === 'PENDING_APPROVAL' && !showRejectInput && (
        <div style={{ marginTop: 'var(--space-6)' }}>
          <Typography.Text strong>Ghi chú phê duyệt (tùy chọn):</Typography.Text>
          <Input
            placeholder="Nhập ghi chú gửi cho giáo viên (nếu có)"
            value={approvalNote}
            onChange={(e) => setApprovalNote(e.target.value)}
            style={{ marginTop: 'var(--space-2)' }}
          />
        </div>
      )}
    </Drawer>
  );
}
