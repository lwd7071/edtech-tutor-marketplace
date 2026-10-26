'use client';

import React, { useState } from 'react';
import { Alert, Button, Empty, Input, Modal, Pagination, Skeleton, Tag, Typography, message } from 'antd';
import { SubjectProposalSnapshot } from '../types';
import { useSubjectProposals, useRejectSubjectProposal } from '../hooks/useAdminApprovals';
import { ApproveSubjectModal } from './ApproveSubjectModal';
import { isConcurrentModification, parseApiError } from '@/shared/backend';
import styles from './SubjectProposalTable.module.css';

const statusLabel: Record<SubjectProposalSnapshot['status'], string> = {
  PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối',
};

export function SubjectProposalTable() {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [selectedProposal, setSelectedProposal] = useState<SubjectProposalSnapshot | null>(null);
  const [approveModalOpen, setApproveModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');

  const { data, isLoading, isError, refetch } = useSubjectProposals(page, pageSize);
  const rejectMutation = useRejectSubjectProposal();
  const proposals = data?.data || [];
  const total = data?.meta?.totalElements ?? proposals.length;

  const handleOpenApprove = (record: SubjectProposalSnapshot) => {
    setSelectedProposal(record);
    setApproveModalOpen(true);
  };

  const handleOpenReject = (record: SubjectProposalSnapshot) => {
    setSelectedProposal(record);
    setRejectionReason('');
    setRejectModalOpen(true);
  };

  const closeApprove = () => {
    setApproveModalOpen(false);
    setSelectedProposal(null);
  };

  const closeReject = () => {
    setRejectModalOpen(false);
    setSelectedProposal(null);
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
        data: { reason: rejectionReason.trim(), version: selectedProposal.version },
      });
      message.success('Đã từ chối đề xuất môn học');
      closeReject();
    } catch (error) {
      message.error(isConcurrentModification(error)
        ? 'Dữ liệu đã được thay đổi bởi người khác. Vui lòng tải lại.'
        : parseApiError(error).message);
    }
  };

  return <section className={styles.queue} aria-labelledby="subject-proposals-title">
    <div className={styles.queueHeader}>
      <div>
        <h2 id="subject-proposals-title">Đề xuất từ gia sư</h2>
        <p>{total} đề xuất trong danh sách</p>
      </div>
      {!isLoading && !isError && proposals.length > 0 && <span className={styles.pageSummary}>Trang {page + 1}</span>}
    </div>

    {isError ? <Alert
      className={styles.error}
      type="error"
      showIcon
      message="Không tải được đề xuất môn học."
      action={<Button onClick={() => void refetch()}>Thử lại</Button>}
    /> : isLoading ? <div className={styles.loading} aria-label="Đang tải đề xuất môn học">
      {[0, 1, 2].map(row => <div className={styles.loadingRow} key={row}><Skeleton active title={{ width: '35%' }} paragraph={{ rows: 2 }} /></div>)}
    </div> : proposals.length === 0 ? <div className={styles.empty}>
      <Empty description="Chưa có đề xuất môn học trong danh sách này." />
    </div> : <>
      <ul className={styles.list}>
        {proposals.map(record => {
          const name = record.proposedName || record.proposedSubjectName || 'Chưa có tên môn';
          const level = record.educationLevel || record.proposedCategory || 'Chưa có cấp học';
          const id = record.proposalId || record.id || '';
          return <li key={id}>
            <article className={styles.proposal}>
              <div className={styles.identity}>
                <h3>{name}</h3>
                <p>{level}</p>
              </div>
              <p className={styles.description}>{record.description || 'Chưa có mô tả.'}</p>
              <dl className={styles.metadata}>
                <div><dt>Gia sư</dt><dd>{record.teacherName || record.teacherId}</dd></div>
                <div><dt>Trạng thái</dt><dd><Tag color={record.status === 'APPROVED' ? 'success' : record.status === 'REJECTED' ? 'error' : 'warning'}>{statusLabel[record.status]}</Tag></dd></div>
              </dl>
              {record.reviewNote && record.status !== 'PENDING' && <p className={styles.reviewNote}><strong>Ghi chú:</strong> {record.reviewNote}</p>}
              {record.status === 'PENDING' && <div className={styles.actions}>
                <Button type="primary" onClick={() => handleOpenApprove(record)}>Phê duyệt</Button>
                <Button danger onClick={() => handleOpenReject(record)}>Từ chối</Button>
              </div>}
            </article>
          </li>;
        })}
      </ul>
      <Pagination
        className={styles.pagination}
        current={page + 1}
        pageSize={pageSize}
        total={total}
        showSizeChanger
        pageSizeOptions={[10, 20, 50]}
        showTotal={(count, range) => `${range[0]}–${range[1]} / ${count}`}
        onChange={(nextPage, nextPageSize) => {
          setPage(nextPage - 1);
          setPageSize(nextPageSize);
        }}
      />
    </>}

    <ApproveSubjectModal open={approveModalOpen} proposal={selectedProposal} onClose={closeApprove} />
    <Modal
      title="Từ chối đề xuất môn học"
      open={rejectModalOpen}
      onCancel={closeReject}
      onOk={() => void handleConfirmReject()}
      confirmLoading={rejectMutation.isPending}
      okText="Xác nhận từ chối"
      cancelText="Hủy"
      okButtonProps={{ danger: true }}
      destroyOnHidden
      rootClassName={styles.rejectModal}
    >
      <Typography.Paragraph>
        Nhập lý do từ chối <strong>{selectedProposal?.proposedName || selectedProposal?.proposedSubjectName}</strong>:
      </Typography.Paragraph>
      <Input.TextArea
        aria-label="Lý do từ chối"
        rows={4}
        placeholder="Lý do từ chối..."
        value={rejectionReason}
        onChange={event => setRejectionReason(event.target.value)}
      />
    </Modal>
  </section>;
}
