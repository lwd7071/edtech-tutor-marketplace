'use client';

import React, { useState } from 'react';
import { Alert, Button, DatePicker, Empty, Form, Input, Modal, Pagination, Popconfirm, Select, Skeleton, Tabs, Tag, Upload, message } from 'antd';
import type { UploadFile } from 'antd';
import type { Dayjs } from 'dayjs';
import { PayoutRequestView, PayoutStatus, formatLedgerTime } from '@/features/finance';
import { CompleteTransferRequest, ProcessPayoutRequest, RejectFinanceRequest } from '../types';
import styles from './AdminPayoutTable.module.css';

interface AdminPayoutTableProps {
  payouts?: PayoutRequestView[];
  loading?: boolean;
  error?: boolean;
  total?: number;
  page?: number;
  pageSize?: number;
  onRetry?: () => void;
  onPageChange?: (page: number, size: number) => void;
  onFilterStatus?: (status?: string) => void;
  onProcessPayout: (id: string, data: ProcessPayoutRequest) => Promise<void> | void;
  onCompletePayout: (id: string, data: CompleteTransferRequest) => Promise<void> | void;
  onRejectPayout: (id: string, data: RejectFinanceRequest) => Promise<void> | void;
}

type CompleteFields = { bankReference: string; transferredAt: Dayjs; proof?: UploadFile[] };
const statuses = [
  { key: 'ALL', value: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', value: 'PENDING', label: 'Chờ duyệt' },
  { key: 'PROCESSING', value: 'PROCESSING', label: 'Đang xử lý' },
  { key: 'SUCCEEDED', value: 'SUCCEEDED', label: 'Đã chuyển' },
  { key: 'REJECTED', value: 'REJECTED', label: 'Bị từ chối' },
];
const statusLabel = (status: PayoutStatus) => ({ PENDING: 'Chờ duyệt', PROCESSING: 'Đang xử lý', SUCCEEDED: 'Đã chuyển', REJECTED: 'Bị từ chối' })[status];
const statusColor = (status: PayoutStatus) => ({ PENDING: 'gold', PROCESSING: 'processing', SUCCEEDED: 'success', REJECTED: 'default' })[status];
const shortId = (id: string) => id.slice(0, 8);
const money = (amount: number) => `${amount.toLocaleString('vi-VN')} ₫`;

function PayoutContext({ payout }: { payout: PayoutRequestView }) {
  return <dl className={styles.context}>
    <div><dt>Mã yêu cầu</dt><dd>#{payout.id}</dd></div>
    <div><dt>Gia sư</dt><dd>{payout.teacherId}</dd></div>
    <div><dt>Ví</dt><dd>{payout.walletId}</dd></div>
    <div><dt>Tài khoản ngân hàng</dt><dd>{payout.bankAccountId}</dd></div>
    <div><dt>Số tiền yêu cầu</dt><dd>{money(payout.amountVnd)}</dd></div>
    <div><dt>Trạng thái</dt><dd>{statusLabel(payout.status)}</dd></div>
    <div><dt>Ngày tạo</dt><dd>{formatLedgerTime(payout.createdAt)}</dd></div>
    {payout.teacherNote && <div className={styles.contextWide}><dt>Ghi chú gia sư</dt><dd>{payout.teacherNote}</dd></div>}
    {payout.adminNote && <div className={styles.contextWide}><dt>Ghi chú admin</dt><dd>{payout.adminNote}</dd></div>}
    {payout.bankReference && <div><dt>Mã giao dịch</dt><dd>{payout.bankReference}</dd></div>}
    {payout.transferredAt && <div><dt>Thời điểm chuyển</dt><dd>{formatLedgerTime(payout.transferredAt)}</dd></div>}
    {payout.processedAt && <div><dt>Thời điểm xử lý</dt><dd>{formatLedgerTime(payout.processedAt)}</dd></div>}
    {payout.proofUrl && <div><dt>Chứng từ</dt><dd><a href={payout.proofUrl} target="_blank" rel="noopener noreferrer">Xem chứng từ</a></dd></div>}
    <div><dt>Phiên bản</dt><dd>{payout.version}</dd></div>
  </dl>;
}

function PayoutLoading() {
  return <div className={styles.loading} aria-label="Đang tải yêu cầu rút tiền">{[0, 1, 2].map(row => <div className={styles.loadingRow} key={row}><Skeleton active title={{ width: '28%' }} paragraph={{ rows: 2 }} /></div>)}</div>;
}

export const AdminPayoutTable: React.FC<AdminPayoutTableProps> = ({
  payouts = [], loading = false, error = false, total = 0, page = 0, pageSize = 20,
  onRetry, onPageChange, onFilterStatus, onProcessPayout, onCompletePayout, onRejectPayout,
}) => {
  const [status, setStatus] = useState('ALL');
  const [selected, setSelected] = useState<PayoutRequestView | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [completeOpen, setCompleteOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [completeForm] = Form.useForm<CompleteFields>();
  const [rejectForm] = Form.useForm<Omit<RejectFinanceRequest, 'version'>>();

  const changeStatus = (next: string) => { setStatus(next); onFilterStatus?.(next === 'ALL' ? undefined : next); };
  const openComplete = (payout: PayoutRequestView) => { setSelected(payout); setActionError(null); completeForm.resetFields(); setCompleteOpen(true); };
  const openReject = (payout: PayoutRequestView) => { setSelected(payout); setActionError(null); rejectForm.resetFields(); setRejectOpen(true); };
  const processPayout = async (payout: PayoutRequestView) => {
    try {
      setActionLoading(true);
      await onProcessPayout(payout.id, { version: payout.version });
    } catch {
      message.error('Không thể chuyển lệnh sang trạng thái xử lý. Hãy thử lại.');
    } finally { setActionLoading(false); }
  };
  const completePayout = async () => {
    if (!selected) return;
    try {
      const values = await completeForm.validateFields();
      const proof = values.proof?.[0]?.originFileObj as File | undefined;
      if (!proof) return;
      setActionLoading(true); setActionError(null);
      await onCompletePayout(selected.id, { bankReference: values.bankReference, transferredAt: values.transferredAt.toISOString(), proof, version: selected.version });
      setCompleteOpen(false);
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setActionError('Không xác nhận được lệnh chuyển tiền. Hãy kiểm tra dữ liệu và thử lại.');
      message.error('Không xác nhận được lệnh chuyển tiền. Hãy thử lại.');
    } finally { setActionLoading(false); }
  };
  const rejectPayout = async () => {
    if (!selected) return;
    try {
      const values = await rejectForm.validateFields();
      setActionLoading(true); setActionError(null);
      await onRejectPayout(selected.id, { ...values, version: selected.version });
      setRejectOpen(false);
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      setActionError('Không từ chối được yêu cầu rút tiền. Hãy thử lại.');
      message.error('Không từ chối được yêu cầu rút tiền. Hãy thử lại.');
    } finally { setActionLoading(false); }
  };

  return <section className={styles.queue} aria-label="Hàng đợi yêu cầu rút tiền">
    <div className={styles.filters}><Tabs activeKey={status} onChange={changeStatus} items={statuses} /><Select className={styles.mobileFilter} aria-label="Lọc trạng thái rút tiền" value={status} onChange={changeStatus} options={statuses} /></div>
    {error ? <Alert className={styles.error} type="error" showIcon message="Không tải được yêu cầu rút tiền." action={<Button onClick={onRetry}>Thử lại</Button>} />
      : loading ? <PayoutLoading />
        : payouts.length === 0 ? <div className={styles.empty}><Empty description="Chưa có yêu cầu rút tiền trong danh sách này." /></div>
          : <>
            <div className={styles.listHeader} aria-hidden="true"><span>Yêu cầu</span><span>Số tiền</span><span>Gia sư và ghi chú</span><span>Trạng thái</span><span>Thao tác</span></div>
            <ul className={styles.rows}>{payouts.map(payout => <li key={payout.id}>
              <article className={styles.row}>
                <div className={styles.request}><h2>#{shortId(payout.id)}</h2><p>{formatLedgerTime(payout.createdAt)}</p><span>{payout.teacherId}</span></div>
                <div className={styles.amount}><strong>{money(payout.amountVnd)}</strong><span>Tài khoản {payout.bankAccountId}</span></div>
                <div className={styles.teacher}><strong>{payout.teacherNote || 'Không có ghi chú gia sư'}</strong><span>Ví {payout.walletId}</span></div>
                <div className={styles.status}><Tag className={styles.statusTag} color={statusColor(payout.status)}>{statusLabel(payout.status)}</Tag><span>{payout.processedAt ? formatLedgerTime(payout.processedAt) : 'Chưa xử lý'}</span></div>
                <div className={styles.actions}>
                  <Button onClick={() => { setSelected(payout); setDetailOpen(true); }}>Chi tiết</Button>
                  {payout.status === 'PENDING' && <>
                    <Popconfirm title="Bắt đầu xử lý lệnh rút này?" description="Lệnh chuyển sang trạng thái đang xử lý." okText="Xử lý" cancelText="Hủy" onConfirm={() => void processPayout(payout)}><Button className={styles.rowAction} type="primary" loading={actionLoading}>Xử lý</Button></Popconfirm>
                    <Button className={styles.rowAction} danger onClick={() => openReject(payout)}>Từ chối</Button>
                  </>}
                  {payout.status === 'PROCESSING' && <>
                    <Button className={styles.rowAction} type="primary" onClick={() => openComplete(payout)}>Xác nhận chuyển</Button>
                    <Button className={styles.rowAction} danger onClick={() => openReject(payout)}>Từ chối</Button>
                  </>}
                </div>
              </article>
            </li>)}</ul>
            <Pagination className={styles.pagination} current={page + 1} pageSize={pageSize} total={total} showSizeChanger pageSizeOptions={[10, 20, 50]} showTotal={count => `${count} yêu cầu`} onChange={(nextPage, nextSize) => onPageChange?.(nextPage - 1, nextSize)} />
          </>}

    <Modal className={styles.modal} title={`Chi tiết yêu cầu rút tiền #${selected ? shortId(selected.id) : ''}`} open={detailOpen} onCancel={() => setDetailOpen(false)} footer={<Button onClick={() => setDetailOpen(false)}>Đóng</Button>} destroyOnHidden>
      {selected && <PayoutContext payout={selected} />}
      {selected?.status === 'PROCESSING' && <div className={styles.detailActions}><Button type="primary" onClick={() => { setDetailOpen(false); openComplete(selected); }}>Xác nhận chuyển</Button><Button danger onClick={() => { setDetailOpen(false); openReject(selected); }}>Từ chối</Button></div>}
    </Modal>
    <Modal className={styles.modal} title={`Xác nhận chuyển tiền #${selected ? shortId(selected.id) : ''}`} open={completeOpen} okText="Xác nhận thành công" cancelText="Hủy" confirmLoading={actionLoading} onCancel={() => setCompleteOpen(false)} onOk={completePayout} destroyOnHidden>
      {selected && <PayoutContext payout={selected} />}
      {actionError && <Alert className={styles.actionError} type="error" showIcon message={actionError} />}
      <Form form={completeForm} layout="vertical" className={styles.form}>
        <Form.Item label="Mã giao dịch ngân hàng" name="bankReference" rules={[{ required: true, message: 'Vui lòng nhập mã tham chiếu giao dịch ngân hàng' }]}><Input placeholder="Mã tham chiếu trên biên nhận" /></Form.Item>
        <Form.Item label="Thời điểm chuyển khoản" name="transferredAt" rules={[{ required: true, message: 'Vui lòng chọn thời điểm chuyển khoản' }]}><DatePicker className={styles.datePicker} showTime format="DD/MM/YYYY HH:mm" /></Form.Item>
        <Form.Item label="Chứng từ ủy nhiệm chi (JPG/PNG/PDF, tối đa 10 MB)" name="proof" valuePropName="fileList" getValueFromEvent={event => event?.fileList} rules={[{ required: true, message: 'Vui lòng tải lên chứng từ' }]}><Upload beforeUpload={() => false} maxCount={1} accept=".jpg,.jpeg,.png,.pdf"><Button>Chọn file</Button></Upload></Form.Item>
      </Form>
    </Modal>
    <Modal className={styles.modal} title={`Từ chối lệnh rút #${selected ? shortId(selected.id) : ''}`} open={rejectOpen} okText="Xác nhận từ chối" okButtonProps={{ danger: true }} cancelText="Hủy" confirmLoading={actionLoading} onCancel={() => setRejectOpen(false)} onOk={rejectPayout} destroyOnHidden>
      {selected && <PayoutContext payout={selected} />}
      {actionError && <Alert className={styles.actionError} type="error" showIcon message={actionError} />}
      <Form form={rejectForm} layout="vertical" className={styles.form}><Form.Item label="Lý do từ chối (bắt buộc)" name="reason" rules={[{ required: true, message: 'Vui lòng nhập lý do từ chối' }]}><Input.TextArea rows={3} /></Form.Item></Form>
    </Modal>
  </section>;
};
