'use client';

import React, { useState } from 'react';
import { Alert, Button, DatePicker, Empty, Form, Input, InputNumber, Modal, Pagination, Popconfirm, Select, Skeleton, Tabs, Tag, Upload, message } from 'antd';
import type { UploadFile } from 'antd';
import type { Dayjs } from 'dayjs';
import { RefundRequestView, RefundStatus, formatLedgerTime, getRefundStatusTag } from '@/features/finance';
import { ApproveRefundRequest, CompleteTransferRequest, RejectFinanceRequest } from '../types';
import styles from './AdminRefundTable.module.css';

interface AdminRefundTableProps {
  refunds?: RefundRequestView[];
  loading?: boolean;
  error?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onRetry?: () => void;
  onPageChange?: (page: number, size: number) => void;
  onFilterStatus?: (status?: string) => void;
  onApproveRefund: (id: string, data: ApproveRefundRequest) => Promise<void> | void;
  onCompleteRefund: (id: string, data: CompleteTransferRequest) => Promise<void> | void;
  onRejectRefund: (id: string, data: RejectFinanceRequest) => Promise<void> | void;
}

type CompleteRefundFields = { bankReference: string; transferredAt: Dayjs; proof?: UploadFile[] };

const filterOptions = [
  { key: 'ALL', value: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', value: 'PENDING', label: 'Chờ duyệt' },
  { key: 'APPROVED', value: 'APPROVED', label: 'Đã duyệt' },
  { key: 'REFUNDED', value: 'REFUNDED', label: 'Đã hoàn tiền' },
  { key: 'REJECTED', value: 'REJECTED', label: 'Bị từ chối' },
];

const money = (amount?: number | null) => amount == null ? 'Chờ tính toán' : `${amount.toLocaleString('vi-VN')} ₫`;
const shortId = (id: string) => id.slice(0, 8);
const statusText = (status: RefundStatus) => ({ PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REFUNDED: 'Đã hoàn tiền', REJECTED: 'Bị từ chối' })[status];

function RefundLoading() {
  return <div className={styles.loading} aria-label="Đang tải yêu cầu hoàn tiền">
    {[0, 1, 2].map(row => <div className={styles.loadingRow} key={row}><Skeleton active title={{ width: '28%' }} paragraph={{ rows: 2 }} /></div>)}
  </div>;
}

function RefundContext({ refund }: { refund: RefundRequestView }) {
  return <dl className={styles.context}>
    <div><dt>Mã yêu cầu</dt><dd>#{refund.id}</dd></div>
    <div><dt>Học viên</dt><dd>{refund.studentId}</dd></div>
    <div><dt>Gói học</dt><dd>{refund.studentPackageId}</dd></div>
    <div><dt>Số buổi yêu cầu</dt><dd>{refund.requestedSessions}</dd></div>
    {refund.approvedSessions != null && <div><dt>Số buổi đã duyệt</dt><dd>{refund.approvedSessions}</dd></div>}
    <div><dt>Số tiền hoàn</dt><dd>{money(refund.refundAmountVnd)}</dd></div>
    <div><dt>Tài khoản nhận</dt><dd>{[refund.bankName, refund.accountNumberMasked, refund.accountHolderName].filter(Boolean).join(' · ') || 'Chưa có thông tin tài khoản'}</dd></div>
    <div className={styles.contextWide}><dt>Lý do yêu cầu</dt><dd>{refund.reason}</dd></div>
    {refund.adminNote && <div className={styles.contextWide}><dt>Ghi chú xử lý</dt><dd>{refund.adminNote}</dd></div>}
    {refund.bankReference && <div><dt>Mã giao dịch</dt><dd>{refund.bankReference}</dd></div>}
    {refund.processedAt && <div><dt>Thời điểm xử lý</dt><dd>{formatLedgerTime(refund.processedAt)}</dd></div>}
    <div><dt>Phiên bản</dt><dd>{refund.version}</dd></div>
  </dl>;
}

export const AdminRefundTable: React.FC<AdminRefundTableProps> = ({
  refunds = [], loading = false, error = false, total = 0, page = 0, pageSize = 20,
  onRetry, onPageChange, onFilterStatus, onApproveRefund, onCompleteRefund, onRejectRefund,
}) => {
  const [status, setStatus] = useState('ALL');
  const [approveModalOpen, setApproveModalOpen] = useState(false);
  const [completeModalOpen, setCompleteModalOpen] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selectedRefund, setSelectedRefund] = useState<RefundRequestView | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [approveForm] = Form.useForm<Omit<ApproveRefundRequest, 'version'>>();
  const [completeForm] = Form.useForm<CompleteRefundFields>();
  const [rejectForm] = Form.useForm<Omit<RejectFinanceRequest, 'version'>>();

  const handleFilter = (key: string) => {
    setStatus(key);
    onFilterStatus?.(key === 'ALL' ? undefined : key);
  };

  const handleOpenApprove = (refund: RefundRequestView) => {
    setSelectedRefund(refund);
    setActionError(null);
    approveForm.resetFields();
    approveForm.setFieldsValue({ approvedSessions: refund.requestedSessions });
    setApproveModalOpen(true);
  };

  const handleOpenComplete = (refund: RefundRequestView) => {
    setSelectedRefund(refund);
    setActionError(null);
    completeForm.resetFields();
    setCompleteModalOpen(true);
  };

  const handleOpenReject = (refund: RefundRequestView) => {
    setSelectedRefund(refund);
    setActionError(null);
    rejectForm.resetFields();
    setRejectModalOpen(true);
  };

  const handleApproveSubmit = async () => {
    if (!selectedRefund) return;
    try {
      const values = await approveForm.validateFields();
      setActionLoading(true);
      setActionError(null);
      await onApproveRefund(selectedRefund.id, { ...values, version: selectedRefund.version });
      setApproveModalOpen(false);
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setActionError('Không duyệt được yêu cầu hoàn tiền. Hãy kiểm tra dữ liệu rồi thử lại.');
      message.error('Không duyệt được yêu cầu hoàn tiền. Hãy kiểm tra dữ liệu rồi thử lại.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCompleteSubmit = async () => {
    if (!selectedRefund) return;
    try {
      const values = await completeForm.validateFields();
      setActionLoading(true);
      setActionError(null);
      const proof = values.proof?.[0]?.originFileObj as File | undefined;
      if (!proof) return;
      await onCompleteRefund(selectedRefund.id, {
        bankReference: values.bankReference,
        transferredAt: values.transferredAt.toISOString(),
        proof,
        version: selectedRefund.version,
      });
      setCompleteModalOpen(false);
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setActionError('Không xác nhận được giao dịch. Kiểm tra chứng từ và thử lại.');
      message.error('Không xác nhận được giao dịch. Kiểm tra chứng từ và thử lại.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!selectedRefund) return;
    try {
      const values = await rejectForm.validateFields();
      setActionLoading(true);
      setActionError(null);
      await onRejectRefund(selectedRefund.id, { ...values, version: selectedRefund.version });
      setRejectModalOpen(false);
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setActionError('Không từ chối được yêu cầu hoàn tiền. Hãy thử lại.');
      message.error('Không từ chối được yêu cầu hoàn tiền. Hãy thử lại.');
    } finally {
      setActionLoading(false);
    }
  };

  const closeApprove = () => setApproveModalOpen(false);
  const closeComplete = () => setCompleteModalOpen(false);
  const closeReject = () => setRejectModalOpen(false);

  return <section className={styles.queue} aria-label="Hàng đợi yêu cầu hoàn tiền">
    <div className={styles.filters}>
      <Tabs activeKey={status} onChange={handleFilter} items={filterOptions} />
      <Select className={styles.mobileFilter} aria-label="Lọc trạng thái hoàn tiền" value={status} onChange={handleFilter} options={filterOptions} />
    </div>

    {error ? <Alert
      className={styles.error}
      type="error"
      showIcon
      title="Không tải được yêu cầu hoàn tiền."
      action={<Button onClick={onRetry}>Thử lại</Button>}
    /> : loading ? <RefundLoading /> : refunds.length === 0 ? <div className={styles.empty}>
      <Empty description="Chưa có yêu cầu hoàn tiền trong danh sách này." />
    </div> : <>
      <div className={styles.listHeader} aria-hidden="true">
        <span>Yêu cầu</span><span>Buổi học và tiền hoàn</span><span>Tài khoản nhận</span><span>Trạng thái</span><span>Thao tác</span>
      </div>
      <ul className={styles.rows}>
        {refunds.map(refund => <li key={refund.id}>
          <article className={styles.row}>
            <div className={styles.request}>
              <h2>#{shortId(refund.id)}</h2>
              <p>{formatLedgerTime(refund.createdAt)}</p>
              <span>{refund.studentId}</span>
            </div>
            <div className={styles.amount}>
              <strong>{refund.requestedSessions} buổi yêu cầu</strong>
              {refund.approvedSessions != null && <span>Đã duyệt {refund.approvedSessions} buổi</span>}
              <b>{money(refund.refundAmountVnd)}</b>
            </div>
            <div className={styles.bank}>
              <strong>{refund.bankName || 'Ngân hàng chưa xác định'}</strong>
              <span>{refund.accountNumberMasked || 'Chưa có số tài khoản đã che'}</span>
              {refund.accountHolderName && <span>{refund.accountHolderName}</span>}
            </div>
            <div className={styles.status}>
              {getRefundStatusTag(refund.status)}
              <span>{statusText(refund.status)}</span>
            </div>
            <div className={styles.actions}>
              <Button onClick={() => { setSelectedRefund(refund); setDetailOpen(true); }}>Chi tiết</Button>
              {refund.status === 'PENDING' && <>
                <Popconfirm
                  title="Duyệt yêu cầu hoàn tiền này?"
                  description="Hệ thống tính số tiền từ số buổi chưa học."
                  okText="Duyệt"
                  cancelText="Hủy"
                  onConfirm={() => handleOpenApprove(refund)}
                ><Button type="primary">Duyệt</Button></Popconfirm>
                <Button danger onClick={() => handleOpenReject(refund)}>Từ chối</Button>
              </>}
              {refund.status === 'APPROVED' && <Button type="primary" onClick={() => handleOpenComplete(refund)}>Xác nhận hoàn</Button>}
            </div>
          </article>
        </li>)}
      </ul>
      <Pagination
        className={styles.pagination}
        current={page + 1}
        pageSize={pageSize}
        total={total}
        showSizeChanger
        pageSizeOptions={[10, 20, 50]}
        showTotal={count => `${count} yêu cầu`}
        onChange={(nextPage, nextSize) => onPageChange?.(nextPage - 1, nextSize)}
      />
    </>}

    <Modal
      className={styles.detailModal}
      title={`Chi tiết yêu cầu #${selectedRefund ? shortId(selectedRefund.id) : ''}`}
      open={detailOpen}
      onCancel={() => setDetailOpen(false)}
      footer={<Button onClick={() => setDetailOpen(false)}>Đóng</Button>}
      destroyOnHidden
    >
      {selectedRefund && <RefundContext refund={selectedRefund} />}
    </Modal>

    <Modal
      className={styles.actionModal}
      title={`Duyệt yêu cầu hoàn tiền #${selectedRefund ? shortId(selectedRefund.id) : ''}`}
      open={approveModalOpen}
      okText="Duyệt"
      cancelText="Hủy"
      confirmLoading={actionLoading}
      onCancel={closeApprove}
      onOk={handleApproveSubmit}
      destroyOnHidden
    >
      {selectedRefund && <RefundContext refund={selectedRefund} />}
      {actionError && <Alert className={styles.actionError} type="error" showIcon message={actionError} />}
      <Form form={approveForm} layout="vertical" className={styles.form}>
        <Form.Item
          label="Số buổi duyệt hoàn"
          name="approvedSessions"
          rules={[{ required: true, type: 'number', min: 1, max: selectedRefund?.requestedSessions }]}
        ><InputNumber min={1} max={selectedRefund?.requestedSessions} precision={0} /></Form.Item>
        <Form.Item label="Ghi chú admin" name="adminNote"><Input.TextArea rows={2} /></Form.Item>
      </Form>
    </Modal>

    <Modal
      className={styles.actionModal}
      title={`Xác nhận chuyển tiền hoàn #${selectedRefund ? shortId(selectedRefund.id) : ''}`}
      open={completeModalOpen}
      okText="Xác nhận hoàn tất"
      cancelText="Hủy"
      confirmLoading={actionLoading}
      onCancel={closeComplete}
      onOk={handleCompleteSubmit}
      destroyOnHidden
    >
      {selectedRefund && <RefundContext refund={selectedRefund} />}
      {actionError && <Alert className={styles.actionError} type="error" showIcon message={actionError} />}
      <Form form={completeForm} layout="vertical" className={styles.form}>
        <Form.Item
          label="Mã giao dịch ngân hàng"
          name="bankReference"
          rules={[{ required: true, message: 'Vui lòng nhập mã tham chiếu ngân hàng' }]}
        ><Input placeholder="Mã tham chiếu trên biên nhận" /></Form.Item>
        <Form.Item
          label="Thời điểm chuyển khoản"
          name="transferredAt"
          rules={[{ required: true, message: 'Vui lòng chọn thời điểm chuyển khoản' }]}
        ><DatePicker className={styles.datePicker} showTime format="DD/MM/YYYY HH:mm" /></Form.Item>
        <Form.Item
          label="Chứng từ ủy nhiệm chi (JPG/PNG/PDF, tối đa 10 MB)"
          name="proof"
          valuePropName="fileList"
          getValueFromEvent={event => event?.fileList}
          rules={[{ required: true, message: 'Vui lòng tải lên chứng từ' }]}
        ><Upload beforeUpload={() => false} maxCount={1} accept=".jpg,.jpeg,.png,.pdf"><Button>Chọn file</Button></Upload></Form.Item>
      </Form>
    </Modal>

    <Modal
      className={styles.actionModal}
      title={`Từ chối yêu cầu hoàn tiền #${selectedRefund ? shortId(selectedRefund.id) : ''}`}
      open={rejectModalOpen}
      okText="Xác nhận từ chối"
      okButtonProps={{ danger: true }}
      cancelText="Hủy"
      confirmLoading={actionLoading}
      onCancel={closeReject}
      onOk={handleRejectSubmit}
      destroyOnHidden
    >
      {selectedRefund && <RefundContext refund={selectedRefund} />}
      {actionError && <Alert className={styles.actionError} type="error" showIcon message={actionError} />}
      <Form form={rejectForm} layout="vertical" className={styles.form}>
        <Form.Item
          label="Lý do từ chối (bắt buộc)"
          name="reason"
          rules={[{ required: true, message: 'Vui lòng nhập lý do từ chối' }]}
        ><Input.TextArea rows={3} placeholder="Ghi rõ lý do để học viên hiểu quyết định." /></Form.Item>
      </Form>
    </Modal>
  </section>;
};
