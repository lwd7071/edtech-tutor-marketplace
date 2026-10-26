'use client';

import { useState } from 'react';
import { Alert, Button, Descriptions, Drawer, Input, Modal, Select, Table, Tag, Tooltip, Typography, message } from 'antd';
import { useAdminBookingSettlements, useBookingSettlementAction } from '../hooks/useAdminFinance';
import type { BookingSettlementAdminView } from '../types/finance';
import { availableSettlementActions, type SettlementAction } from '../model/settlementActions';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import { isConcurrentModification, parseApiError } from '@/shared/backend';

const labels: Record<string, string> = {
  HELD: 'Đang giữ tiền', DISPUTE_PENDING: 'Chờ khiếu nại', REOPENED: 'Đã mở lại',
  AWAITING_ADMIN_DECISION: 'Chờ quyết định', RELEASED: 'Đã giải ngân', RETAINED: 'Đã giữ lại',
};
const actionLabels: Record<SettlementAction, string> = {
  reopen: 'Mở lại', release: 'Chuyển gia sư', retain: 'Giữ nền tảng',
};
const shortId = (id: string) => `${id.slice(0, 8)}…${id.slice(-4)}`;
const confirmation = (value?: string | null) => value ? `Đã xác nhận · ${formatVietnamDateTime(value)}` : 'Chưa xác nhận';

export function AdminBookingSettlementsPage() {
  const [status, setStatus] = useState<string>();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<BookingSettlementAdminView | null>(null);
  const [action, setAction] = useState<SettlementAction | null>(null);
  const [note, setNote] = useState('');
  const query = useAdminBookingSettlements(status, page, 20);
  const mutation = useBookingSettlementAction(action ?? 'retain');
  const missingReason = selected?.status === 'DISPUTE_PENDING' && !selected.disputeReason?.trim();

  const resetSelection = () => { setSelected(null); setAction(null); setNote(''); };
  const closeDrawer = () => { if (!mutation.isPending) resetSelection(); };
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
    setAction(nextAction); setNote('');
  };

  return <div className="tm-stack">
    <div className="tm-toolbar">
      <h1>Quyết toán buổi học</h1>
      <Select allowClear aria-label="Lọc trạng thái quyết toán" placeholder="Lọc trạng thái" value={status}
        onChange={value => { setStatus(value); setPage(0); }}
        options={Object.entries(labels).map(([value, label]) => ({ value, label }))} />
    </div>
    {query.isError && <Alert type="error" title="Chưa tải được quyết toán" action={<Button onClick={() => query.refetch()}>Thử lại</Button>} />}
    {!query.isError && <Table rowKey="bookingId" loading={query.isLoading} dataSource={query.data?.data ?? []}
      pagination={{ current: page + 1, pageSize: 20, total: query.data?.meta?.totalElements, onChange: next => setPage(next - 1) }}
      columns={[
        { title: 'Booking', dataIndex: 'bookingId', render: (id: string) => <Tooltip title={id}>{shortId(id)}</Tooltip> },
        { title: 'Học viên / Gia sư', render: (_: unknown, row: BookingSettlementAdminView) => <div><div>{row.studentName}</div><Typography.Text type="secondary">{row.teacherName}</Typography.Text></div> },
        { title: 'Trạng thái', dataIndex: 'status', render: (value: string) => <Tag>{labels[value] ?? value}</Tag> },
        { title: 'Tiền gia sư', dataIndex: 'netAmountVnd', render: (value: number | null) => value == null ? '—' : `${value.toLocaleString('vi-VN')} ₫` },
        { title: 'Chi tiết', render: (_: unknown, row: BookingSettlementAdminView) => <Button onClick={() => setSelected(row)}>Xem chi tiết</Button> },
      ]} />}

    <Drawer title="Chi tiết quyết toán" open={!!selected} onClose={closeDrawer} width={560} destroyOnHidden>
      {selected && <div className="tm-stack">
        <Descriptions bordered size="small" column={1}>
          <Descriptions.Item label="Học viên"><div>{selected.studentName}</div><Tooltip title={selected.studentId}><Typography.Text copyable={{ text: selected.studentId }}>{shortId(selected.studentId)}</Typography.Text></Tooltip></Descriptions.Item>
          <Descriptions.Item label="Gia sư"><div>{selected.teacherName}</div><Tooltip title={selected.teacherId}><Typography.Text copyable={{ text: selected.teacherId }}>{shortId(selected.teacherId)}</Typography.Text></Tooltip></Descriptions.Item>
          <Descriptions.Item label="Booking"><Tooltip title={selected.bookingId}><Typography.Text copyable={{ text: selected.bookingId }}>{shortId(selected.bookingId)}</Typography.Text></Tooltip></Descriptions.Item>
          <Descriptions.Item label="Trạng thái buổi học">{selected.bookingStatus}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái quyết toán"><Tag>{labels[selected.status] ?? selected.status}</Tag></Descriptions.Item>
          <Descriptions.Item label="Thời gian học">{formatVietnamDateTime(selected.startTime)} – {formatVietnamDateTime(selected.endTime)}</Descriptions.Item>
          <Descriptions.Item label="Xác nhận gia sư">{confirmation(selected.teacherConfirmedAt)}</Descriptions.Item>
          <Descriptions.Item label="Xác nhận học viên">{confirmation(selected.studentConfirmedAt)}</Descriptions.Item>
          <Descriptions.Item label="Hạn xác nhận ban đầu">{formatVietnamDateTime(selected.confirmationDeadline)}</Descriptions.Item>
          {selected.reopenDeadline && <Descriptions.Item label="Hạn mở lại">{formatVietnamDateTime(selected.reopenDeadline)}</Descriptions.Item>}
          <Descriptions.Item label="Tiền gia sư">{selected.netAmountVnd == null ? '—' : `${selected.netAmountVnd.toLocaleString('vi-VN')} ₫`}</Descriptions.Item>
          <Descriptions.Item label="Lý do khiếu nại">{selected.disputeReason?.trim() || '—'}</Descriptions.Item>
          <Descriptions.Item label="Thời điểm khiếu nại">{selected.disputedAt ? formatVietnamDateTime(selected.disputedAt) : '—'}</Descriptions.Item>
        </Descriptions>
        {missingReason && <Alert type="error" showIcon title="Không có lý do khiếu nại được ghi nhận." />}
        {!missingReason && availableSettlementActions(selected).length > 0 && <div className="tm-inline">
          {availableSettlementActions(selected).map(item => <Button key={item} type={item === 'release' ? 'primary' : 'default'} danger={item === 'retain'} disabled={mutation.isPending} onClick={() => choose(item)}>{actionLabels[item]}</Button>)}
        </div>}
      </div>}
    </Drawer>
    <Modal open={!!action} title={action ? `${actionLabels[action]} · Ghi chú quyết toán` : 'Ghi chú quyết toán'}
      onCancel={() => { if (!mutation.isPending) { setAction(null); setNote(''); } }} onOk={() => void run()}
      okText={action ? actionLabels[action] : 'Xác nhận'}
      okButtonProps={{ loading: mutation.isPending, disabled: !note.trim() || !!missingReason }} cancelButtonProps={{ disabled: mutation.isPending }}
      closable={!mutation.isPending} maskClosable={!mutation.isPending}>
      <Input.TextArea aria-label="Lý do xử lý quyết toán" rows={4} value={note} onChange={event => setNote(event.target.value)} placeholder="Nêu lý do xử lý" />
    </Modal>
  </div>;
}
