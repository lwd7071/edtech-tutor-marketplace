'use client';

import React, { useState } from 'react';
import { Button, Drawer, Input, message, Tag } from 'antd';
import { TeacherApprovalSnapshot } from '../types';
import { useApproveTeacher, useRejectTeacher } from '../hooks/useAdminApprovals';
import { parseApiError } from '@/shared/backend';
import styles from './TeacherDetailDrawer.module.css';

interface TeacherDetailDrawerProps {
  open: boolean;
  teacher: TeacherApprovalSnapshot | null;
  onClose: () => void;
}

const unknown = 'Chưa cập nhật';

export function TeacherDetailDrawer({ open, teacher, onClose }: TeacherDetailDrawerProps) {
  const [rejectReason, setRejectReason] = useState('');
  const [approvalNote, setApprovalNote] = useState('');
  const [showRejectInput, setShowRejectInput] = useState(false);
  const approveMutation = useApproveTeacher();
  const rejectMutation = useRejectTeacher();

  if (!teacher) return null;

  const handleApprove = async () => {
    try {
      await approveMutation.mutateAsync({ teacherId: teacher.teacherProfileId, data: { note: approvalNote } });
      message.success('Đã phê duyệt hồ sơ gia sư thành công');
      onClose();
    } catch (err) {
      message.error(parseApiError(err).message);
    }
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) {
      message.warning('Vui lòng nhập lý do từ chối hồ sơ');
      return;
    }
    try {
      await rejectMutation.mutateAsync({ teacherId: teacher.teacherProfileId, data: { reason: rejectReason } });
      message.success('Đã từ chối hồ sơ gia sư');
      setShowRejectInput(false);
      setRejectReason('');
      onClose();
    } catch (err) {
      message.error(parseApiError(err).message);
    }
  };

  const pending = teacher.status === 'PENDING_APPROVAL';
  const modes = [teacher.supportsOnline && 'Online', teacher.supportsOffline && 'Trực tiếp'].filter(Boolean).join(', ') || unknown;
  const status = teacher.status === 'APPROVED'
    ? <Tag color="success">Đã duyệt</Tag>
    : teacher.status === 'REJECTED' ? <Tag color="error">Đã từ chối</Tag> : <Tag color="warning">Chờ duyệt</Tag>;

  return <Drawer
    className={styles.drawer}
    rootClassName={styles.drawerRoot}
    title={<div className={styles.drawerTitle}><span>Chi tiết hồ sơ gia sư</span><span>{status}</span></div>}
    open={open}
    onClose={onClose}
    size="large"
    destroyOnHidden
    footer={<div className={styles.actions}>
      {pending ? <>
        <Button onClick={onClose}>Đóng</Button>
        <Button danger onClick={() => setShowRejectInput(value => !value)}>{showRejectInput ? 'Hủy từ chối' : 'Từ chối'}</Button>
        {!showRejectInput && <Button type="primary" loading={approveMutation.isPending} onClick={() => void handleApprove()}>Phê duyệt</Button>}
      </> : <Button onClick={onClose}>Đóng</Button>}
    </div>}
  >
    <div className={styles.details}>
      <section aria-labelledby="teacher-profile-heading">
        <h2 id="teacher-profile-heading">Thông tin gia sư</h2>
        <dl className={styles.profileList}>
          <div><dt>Họ và tên</dt><dd>{teacher.fullName || unknown}</dd></div>
          <div><dt>Email</dt><dd>{teacher.email || unknown}</dd></div>
          <div><dt>Kinh nghiệm</dt><dd>{teacher.yearsOfExperience ? `${teacher.yearsOfExperience} năm` : unknown}</dd></div>
          <div><dt>Ngôn ngữ</dt><dd>{teacher.languages?.join(', ') || unknown}</dd></div>
          <div><dt>Hình thức dạy</dt><dd>{modes}</dd></div>
          <div><dt>Khu vực</dt><dd>{teacher.locationAddress || unknown}</dd></div>
        </dl>
        <div className={styles.bio}><h3>Giới thiệu</h3><p>{teacher.bio || 'Gia sư chưa cung cấp tiểu sử giới thiệu.'}</p></div>
        {teacher.introductionVideoUrl && <p className={styles.video}><a href={teacher.introductionVideoUrl} target="_blank" rel="noopener noreferrer">Xem video giới thiệu</a></p>}
      </section>

      {teacher.rejectionReason && <section className={styles.rejection} aria-labelledby="teacher-rejection-heading">
        <h2 id="teacher-rejection-heading">Lý do từ chối trước đó</h2><p>{teacher.rejectionReason}</p>
      </section>}

      <section aria-labelledby="teacher-documents-heading">
        <h2 id="teacher-documents-heading">Giấy tờ xác minh</h2>
        {teacher.documents.length === 0 ? <p className={styles.noDocuments}>Chưa có giấy tờ định danh nào được đính kèm.</p> :
          <ul className={styles.documents}>
            {teacher.documents.map(document => <li key={document.id}>
              <div><h3>{document.title}</h3><p>{document.type} · {(document.fileSize / 1024).toFixed(0)} KB · {document.verificationStatus}</p></div>
              <a href={document.secureUrl} target="_blank" rel="noopener noreferrer">Xem tài liệu</a>
            </li>)}
          </ul>}
      </section>

      {pending && <section aria-labelledby="teacher-decision-heading" className={styles.decision}>
        <h2 id="teacher-decision-heading">Quyết định hồ sơ</h2>
        {showRejectInput ? <>
          <label htmlFor="teacher-reject-reason">Lý do từ chối (bắt buộc)</label>
          <Input.TextArea id="teacher-reject-reason" rows={4} value={rejectReason} onChange={event => setRejectReason(event.target.value)} placeholder="Nhập lý do để gia sư biết cần bổ sung điều gì." />
          <Button danger type="primary" loading={rejectMutation.isPending} onClick={() => void handleReject()}>Xác nhận từ chối</Button>
        </> : <>
          <label htmlFor="teacher-approval-note">Ghi chú phê duyệt (tùy chọn)</label>
          <Input id="teacher-approval-note" value={approvalNote} onChange={event => setApprovalNote(event.target.value)} placeholder="Ghi chú gửi cho gia sư" />
        </>}
      </section>}
    </div>
  </Drawer>;
}
