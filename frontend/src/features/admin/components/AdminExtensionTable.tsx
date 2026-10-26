'use client';

import React, { useState } from 'react';
import { Alert, Button, DatePicker, Empty, Form, Input, Modal, Pagination, Select, Skeleton, Tabs, message } from 'antd';
import type { Dayjs } from 'dayjs';
import dayjs from 'dayjs';
import { ExtensionRequestView, ExtensionStatus, formatLedgerTime, getExtensionStatusTag } from '@/features/finance';
import { ApproveExtensionRequest, RejectRequest } from '../types';
import styles from './AdminExtensionTable.module.css';

interface AdminExtensionTableProps {
  extensions?: ExtensionRequestView[];
  loading?: boolean;
  error?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onRetry?: () => void;
  onPageChange?: (page: number, size: number) => void;
  onFilterStatus?: (status?: string) => void;
  onApproveExtension: (id: string, data: ApproveExtensionRequest) => Promise<void> | void;
  onRejectExtension: (id: string, data: RejectRequest) => Promise<void> | void;
}

type ApproveFields = { date: Dayjs; adminNote?: string };
const statuses = [
  { key: 'ALL', value: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', value: 'PENDING', label: 'Chờ duyệt' },
  { key: 'APPROVED', value: 'APPROVED', label: 'Đã gia hạn' },
  { key: 'REJECTED', value: 'REJECTED', label: 'Bị từ chối' },
];
const statusText = (status: ExtensionStatus) => ({ PENDING: 'Chờ duyệt', APPROVED: 'Đã gia hạn', REJECTED: 'Bị từ chối' })[status];
const shortId = (id: string) => id.slice(0, 8);

function ExtensionContext({ extension }: { extension: ExtensionRequestView }) {
  return <dl className={styles.context}>
    <div><dt>Mã yêu cầu</dt><dd>#{extension.id}</dd></div>
    <div><dt>Học viên</dt><dd>{extension.studentId}</dd></div>
    <div><dt>Gói học</dt><dd>{extension.studentPackageId}</dd></div>
    <div><dt>Ngày gửi</dt><dd>{formatLedgerTime(extension.createdAt)}</dd></div>
    <div><dt>Hạn gia hạn đề xuất</dt><dd>{formatLedgerTime(extension.requestedExpiryDate)}</dd></div>
    {extension.approvedExpiryDate && <div><dt>Hạn đã duyệt</dt><dd>{formatLedgerTime(extension.approvedExpiryDate)}</dd></div>}
    <div className={styles.contextWide}><dt>Lý do</dt><dd>{extension.reason || 'Không có lý do được cung cấp.'}</dd></div>
    {extension.adminNote && <div className={styles.contextWide}><dt>Ghi chú xử lý</dt><dd>{extension.adminNote}</dd></div>}
    {extension.reviewedAt && <div><dt>Thời điểm xử lý</dt><dd>{formatLedgerTime(extension.reviewedAt)}</dd></div>}
  </dl>;
}

function ExtensionLoading() {
  return <div className={styles.loading} aria-label="Đang tải yêu cầu gia hạn">
    {[0, 1, 2].map(row => <div className={styles.loadingRow} key={row}><Skeleton active title={{ width: '28%' }} paragraph={{ rows: 2 }} /></div>)}
  </div>;
}

export const AdminExtensionTable: React.FC<AdminExtensionTableProps> = ({
  extensions = [], loading = false, error = false, total = 0, page = 0, pageSize = 20,
  onRetry, onPageChange, onFilterStatus, onApproveExtension, onRejectExtension,
}) => {
  const [status, setStatus] = useState('ALL');
  const [selected, setSelected] = useState<ExtensionRequestView | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [approveOpen, setApproveOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [approveForm] = Form.useForm<ApproveFields>();
  const [rejectForm] = Form.useForm<RejectRequest>();

  const changeStatus = (next: string) => {
    setStatus(next);
    onFilterStatus?.(next === 'ALL' ? undefined : next);
  };
  const openApprove = (extension: ExtensionRequestView) => {
    setSelected(extension);
    setActionError(null);
    approveForm.resetFields();
    approveForm.setFieldsValue({ date: dayjs(extension.requestedExpiryDate) });
    setApproveOpen(true);
  };
  const openReject = (extension: ExtensionRequestView) => {
    setSelected(extension);
    setActionError(null);
    rejectForm.resetFields();
    setRejectOpen(true);
  };
  const submitApprove = async () => {
    if (!selected) return;
    try {
      const values = await approveForm.validateFields();
      setActionLoading(true);
      setActionError(null);
      await onApproveExtension(selected.id, { approvedExpiryDate: values.date.toISOString(), adminNote: values.adminNote });
      setApproveOpen(false);
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setActionError('Không duyệt được yêu cầu gia hạn. Hãy thử lại.');
      message.error('Không duyệt được yêu cầu gia hạn. Hãy thử lại.');
    } finally {
      setActionLoading(false);
    }
  };
  const submitReject = async () => {
    if (!selected) return;
    try {
      const values = await rejectForm.validateFields();
      setActionLoading(true);
      setActionError(null);
      await onRejectExtension(selected.id, values);
      setRejectOpen(false);
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setActionError('Không từ chối được yêu cầu gia hạn. Hãy thử lại.');
      message.error('Không từ chối được yêu cầu gia hạn. Hãy thử lại.');
    } finally {
      setActionLoading(false);
    }
  };

  return <section className={styles.queue} aria-label="Hàng đợi yêu cầu gia hạn">
    <div className={styles.filters}>
      <Tabs activeKey={status} onChange={changeStatus} items={statuses} />
      <Select className={styles.mobileFilter} aria-label="Lọc trạng thái gia hạn" value={status} onChange={changeStatus} options={statuses} />
    </div>

    {error ? <Alert className={styles.error} type="error" showIcon message="Không tải được yêu cầu gia hạn." action={<Button onClick={onRetry}>Thử lại</Button>} />
      : loading ? <ExtensionLoading />
        : extensions.length === 0 ? <div className={styles.empty}><Empty description="Chưa có yêu cầu gia hạn trong danh sách này." /></div>
          : <>
            <div className={styles.listHeader} aria-hidden="true"><span>Yêu cầu</span><span>Hạn đề xuất</span><span>Gói học</span><span>Trạng thái</span><span>Thao tác</span></div>
            <ul className={styles.rows}>
              {extensions.map(extension => <li key={extension.id}>
                <article className={styles.row}>
                  <div className={styles.request}><h2>#{shortId(extension.id)}</h2><p>{formatLedgerTime(extension.createdAt)}</p><span>{extension.studentId}</span></div>
                  <div className={styles.expiry}><span>Gia hạn đến</span><strong>{formatLedgerTime(extension.requestedExpiryDate)}</strong></div>
                  <div className={styles.package}><strong>{extension.studentPackageId}</strong><span>{extension.reason}</span></div>
                  <div className={styles.status}>{getExtensionStatusTag(extension.status)}<span>{statusText(extension.status)}</span></div>
                  <div className={styles.actions}>
                    <Button onClick={() => { setSelected(extension); setDetailOpen(true); }}>Chi tiết</Button>
                    {extension.status === 'PENDING' && <>
                      <Button type="primary" onClick={() => openApprove(extension)}>Duyệt</Button>
                      <Button danger onClick={() => openReject(extension)}>Từ chối</Button>
                    </>}
                  </div>
                </article>
              </li>)}
            </ul>
            <Pagination className={styles.pagination} current={page + 1} pageSize={pageSize} total={total} showSizeChanger pageSizeOptions={[10, 20, 50]} showTotal={count => `${count} yêu cầu`} onChange={(nextPage, nextSize) => onPageChange?.(nextPage - 1, nextSize)} />
          </>}

    <Modal className={styles.modal} title={`Chi tiết yêu cầu gia hạn #${selected ? shortId(selected.id) : ''}`} open={detailOpen} onCancel={() => setDetailOpen(false)} footer={<Button onClick={() => setDetailOpen(false)}>Đóng</Button>} destroyOnHidden>
      {selected && <ExtensionContext extension={selected} />}
      {selected?.status === 'PENDING' && <div className={styles.detailActions}><Button type="primary" onClick={() => { setDetailOpen(false); openApprove(selected); }}>Duyệt</Button><Button danger onClick={() => { setDetailOpen(false); openReject(selected); }}>Từ chối</Button></div>}
    </Modal>
    <Modal className={styles.modal} title={`Duyệt gia hạn #${selected ? shortId(selected.id) : ''}`} open={approveOpen} okText="Xác nhận gia hạn" cancelText="Hủy" confirmLoading={actionLoading} onCancel={() => setApproveOpen(false)} onOk={submitApprove} destroyOnHidden>
      {selected && <ExtensionContext extension={selected} />}
      {actionError && <Alert className={styles.actionError} type="error" showIcon message={actionError} />}
      <Form form={approveForm} layout="vertical" className={styles.form}>
        <Form.Item label="Ngày hết hạn mới được duyệt" name="date" rules={[{ required: true, message: 'Vui lòng chọn ngày hết hạn mới' }]}><DatePicker className={styles.datePicker} format="DD/MM/YYYY" disabledDate={current => Boolean(current && current < dayjs().endOf('day'))} /></Form.Item>
        <Form.Item label="Ghi chú admin" name="adminNote"><Input.TextArea rows={2} placeholder="Ghi chú xử lý (không bắt buộc)" /></Form.Item>
      </Form>
    </Modal>
    <Modal className={styles.modal} title={`Từ chối gia hạn #${selected ? shortId(selected.id) : ''}`} open={rejectOpen} okText="Xác nhận từ chối" okButtonProps={{ danger: true }} cancelText="Hủy" confirmLoading={actionLoading} onCancel={() => setRejectOpen(false)} onOk={submitReject} destroyOnHidden>
      {selected && <ExtensionContext extension={selected} />}
      {actionError && <Alert className={styles.actionError} type="error" showIcon message={actionError} />}
      <Form form={rejectForm} layout="vertical" className={styles.form}>
        <Form.Item label="Lý do từ chối (bắt buộc)" name="reason" rules={[{ required: true, message: 'Vui lòng nhập lý do từ chối' }]}><Input.TextArea rows={3} placeholder="Nêu rõ lý do để học viên hiểu quyết định." /></Form.Item>
      </Form>
    </Modal>
  </section>;
};
