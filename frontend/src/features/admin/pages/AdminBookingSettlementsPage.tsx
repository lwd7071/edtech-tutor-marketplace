'use client';

import { useState } from 'react';
import { Alert, Button, Drawer, Empty, Input, Modal, Pagination, Select, Skeleton, Tag, Tooltip, Typography, message } from 'antd';
import styles from './AdminBookingSettlementsPage.module.css';
import { useAdminBookingSettlements, useBookingSettlementAction } from '../hooks/useAdminFinance';
import type { BookingSettlementAdminView } from '../types/finance';
import { availableSettlementActions, type SettlementAction } from '../model/settlementActions';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import { isConcurrentModification, parseApiError } from '@/shared/backend';

const labels: Record<string, string> = {
  HELD: 'Đang giữ tiền', DISPUTE_PENDING: 'Chờ khiếu nại', REOPENED: 'Đã mở lại',
  AWAITING_ADMIN_DECISION: 'Chờ quyết định', RELEASED: 'Đã giải ngân', RETAINED: 'Đã giữ lại',
};
const actionLabels: Record<SettlementAction, string> = { reopen: 'Mở lại', release: 'Chuyển gia sư', retain: 'Giữ nền tảng' };
const statusValues = Object.entries(labels).map(([value, label]) => ({ value, label }));
const shortId = (id: string) => `${id.slice(0, 8)}…${id.slice(-4)}`;
const confirmation = (value?: string | null) => value ? `Đã xác nhận · ${formatVietnamDateTime(value)}` : 'Chưa xác nhận';
const amount = (value?: number | null) => value == null ? '—' : `${value.toLocaleString('vi-VN')} ₫`;

function SettlementLoading() {
  return <div className={styles.loading} aria-label="Đang tải quyết toán">{[0, 1, 2].map(item => <div key={item} className={styles.loadingRow}><Skeleton active title={{ width: '24%' }} paragraph={{ rows: 2 }} /></div>)}</div>;
}

function SettlementDetail({ settlement }: { settlement: BookingSettlementAdminView }) {
  return <div className={styles.detail}>
    <section aria-labelledby="settlement-people">
      <h3 id="settlement-people">Người tham gia</h3>
      <dl className={styles.detailGrid}>
        <div><dt>Học viên</dt><dd>{settlement.studentName}</dd><dd className={styles.id}><Tooltip title={settlement.studentId}><Typography.Text copyable={{ text: settlement.studentId }}>{shortId(settlement.studentId)}</Typography.Text></Tooltip></dd></div>
        <div><dt>Gia sư</dt><dd>{settlement.teacherName}</dd><dd className={styles.id}><Tooltip title={settlement.teacherId}><Typography.Text copyable={{ text: settlement.teacherId }}>{shortId(settlement.teacherId)}</Typography.Text></Tooltip></dd></div>
        <div className={styles.detailWide}><dt>Mã booking</dt><dd><Tooltip title={settlement.bookingId}><Typography.Text copyable={{ text: settlement.bookingId }}>{shortId(settlement.bookingId)}</Typography.Text></Tooltip></dd></div>
      </dl>
    </section>
    <section aria-labelledby="settlement-status">
      <h3 id="settlement-status">Trạng thái và thời gian</h3>
      <dl className={styles.detailGrid}>
        <div><dt>Buổi học</dt><dd>{settlement.bookingStatus}</dd></div>
        <div><dt>Quyết toán</dt><dd><Tag>{labels[settlement.status] ?? settlement.status}</Tag></dd></div>
        <div className={styles.detailWide}><dt>Thời gian học</dt><dd>{formatVietnamDateTime(settlement.startTime)} – {formatVietnamDateTime(settlement.endTime)}</dd></div>
        <div><dt>Gia sư xác nhận</dt><dd>{confirmation(settlement.teacherConfirmedAt)}</dd></div>
        <div><dt>Học viên xác nhận</dt><dd>{confirmation(settlement.studentConfirmedAt)}</dd></div>
        <div><dt>Hạn xác nhận ban đầu</dt><dd>{formatVietnamDateTime(settlement.confirmationDeadline)}</dd></div>
        {settlement.reopenDeadline && <div><dt>Hạn mở lại</dt><dd>{formatVietnamDateTime(settlement.reopenDeadline)}</dd></div>}
        <div><dt>Tiền gia sư</dt><dd className={styles.money}>{amount(settlement.netAmountVnd)}</dd></div>
      </dl>
    </section>
    <section aria-labelledby="settlement-dispute">
      <h3 id="settlement-dispute">Thông tin khiếu nại</h3>
      <dl className={styles.detailGrid}>
        <div className={styles.detailWide}><dt>Lý do</dt><dd>{settlement.disputeReason?.trim() || 'Không có lý do được ghi nhận.'}</dd></div>
        <div><dt>Thời điểm gửi</dt><dd>{settlement.disputedAt ? formatVietnamDateTime(settlement.disputedAt) : '—'}</dd></div>
      </dl>
    </section>
  </div>;
}

export function AdminBookingSettlementsPage() {
  const [status, setStatus] = useState<string>();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<BookingSettlementAdminView | null>(null);
  const [action, setAction] = useState<SettlementAction | null>(null);
  const [note, setNote] = useState('');
  const query = useAdminBookingSettlements(status, page, 20);
  const mutation = useBookingSettlementAction(action ?? 'retain');
  const missingReason = selected?.status === 'DISPUTE_PENDING' && !selected.disputeReason?.trim();
  const settlements = query.data?.data ?? [];
  const total = query.data?.meta?.totalElements ?? settlements.length;

  const resetSelection = () => { setSelected(null); setAction(null); setNote(''); };
  const closeDrawer = () => { if (!mutation.isPending && !action) resetSelection(); };
  const run = async () => {
    if (!selected || !action || !note.trim() || mutation.isPending || missingReason) return;
    try {
      await mutation.mutateAsync({ id: selected.bookingId, version: selected.version, note: note.trim() });
      message.success('Đã cập nhật quyết toán.');
      resetSelection();
      void query.refetch();
    } catch (error) {
      if (isConcurrentModification(error)) {
        message.error('Quyết toán đã thay đổi. Danh sách đang được tải lại; hãy kiểm tra phiên bản mới trước khi quyết định.');
        resetSelection();
      } else message.error(parseApiError(error).message);
      void query.refetch();
    }
  };
  const choose = (nextAction: SettlementAction) => {
    if (mutation.isPending || missingReason) return;
    setAction(nextAction);
    setNote('');
  };

  return <div className={styles.page}>
    <header className={styles.heading}><div><h1>Quyết toán buổi học</h1><p>Đối chiếu xác nhận và thông tin buổi học trước khi phân bổ khoản đang giữ.</p></div>
      <Select allowClear className={styles.filter} aria-label="Lọc trạng thái quyết toán" placeholder="Lọc trạng thái" value={status} onChange={value => { setStatus(value); setPage(0); }} options={statusValues} />
    </header>
    {query.isError ? <Alert className={styles.error} type="error" showIcon message="Chưa tải được quyết toán" description="Kiểm tra kết nối rồi thử lại." action={<Button onClick={() => void query.refetch()}>Thử lại</Button>} />
      : query.isLoading ? <SettlementLoading />
        : settlements.length === 0 ? <div className={styles.empty}><Empty description="Chưa có quyết toán buổi học." /></div>
          : <>
            <div className={styles.listHeader} aria-hidden="true"><span>Booking</span><span>Học viên / Gia sư</span><span>Buổi học</span><span>Quyết toán</span><span>Tiền gia sư</span><span>Chi tiết</span></div>
            <ul className={styles.rows}>{settlements.map(settlement => <li key={settlement.bookingId}>
              <article className={styles.row}>
                <div className={styles.booking}><span>Booking</span><strong>{shortId(settlement.bookingId)}</strong><small>{formatVietnamDateTime(settlement.startTime)}</small></div>
                <div className={styles.people}><span>{settlement.studentName}</span><small>{settlement.teacherName}</small></div>
                <div className={styles.session}><span>{settlement.bookingStatus}</span><small>{confirmation(settlement.studentConfirmedAt)} · học viên</small></div>
                <div className={styles.status}><Tag>{labels[settlement.status] ?? settlement.status}</Tag><small>{confirmation(settlement.teacherConfirmedAt)} · gia sư</small></div>
                <div className={styles.amount}><strong>{amount(settlement.netAmountVnd)}</strong></div>
                <div className={styles.actions}><Button onClick={() => setSelected(settlement)}>Chi tiết</Button></div>
              </article>
            </li>)}</ul>
            <Pagination className={styles.pagination} current={page + 1} pageSize={20} total={total} showTotal={count => `${count} quyết toán`} onChange={next => setPage(next - 1)} />
          </>}

    <Drawer className={styles.drawer} title="Chi tiết quyết toán" open={!!selected} onClose={closeDrawer} destroyOnHidden>
      {selected && <>
        <SettlementDetail settlement={selected} />
        {missingReason && <Alert className={styles.missingReason} type="error" showIcon message="Không có lý do khiếu nại được ghi nhận." />}
        {!missingReason && availableSettlementActions(selected).length > 0 && <div className={styles.drawerActions}>
          {availableSettlementActions(selected).map(item => <Button key={item} type={item === 'release' ? 'primary' : 'default'} danger={item === 'retain'} disabled={mutation.isPending} onClick={() => choose(item)}>{actionLabels[item]}</Button>)}
        </div>}
      </>}
    </Drawer>
    <Modal className={styles.noteModal} open={!!action} title={action ? `${actionLabels[action]} · Ghi chú quyết toán` : 'Ghi chú quyết toán'}
      onCancel={() => { if (!mutation.isPending) { setAction(null); setNote(''); } }} onOk={() => void run()}
      okText={action ? actionLabels[action] : 'Xác nhận'}
      okButtonProps={{ loading: mutation.isPending, disabled: !note.trim() || !!missingReason }} cancelButtonProps={{ disabled: mutation.isPending }}
      closable={!mutation.isPending} mask={{ closable: !mutation.isPending }}>
      {selected && <div className={styles.actionContext}><strong>Booking {shortId(selected.bookingId)}</strong><span>{labels[selected.status] ?? selected.status}</span></div>}
      <label className={styles.noteLabel} htmlFor="settlement-action-note">Lý do xử lý</label>
      <Input.TextArea id="settlement-action-note" aria-label="Lý do xử lý quyết toán" rows={4} value={note} onChange={event => setNote(event.target.value)} placeholder="Nêu lý do xử lý" />
    </Modal>
  </div>;
}
