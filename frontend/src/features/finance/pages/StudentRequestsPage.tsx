'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Empty, Pagination, Skeleton, Tabs, Tag } from 'antd';
import { useStudentRefunds, useStudentExtensions } from '../hooks/useFinance';
import { bookingApi } from '@/features/bookings/api/bookingApi';

const PAGE_SIZE = 10;
const Pager = ({ page, response, onChange }: { page: number; response?: { meta?: { totalElements: number; totalPages: number; size: number } | null }; onChange: (page: number) => void }) =>
  (response?.meta?.totalPages ?? 0) > 1 ? <Pagination current={page + 1} total={response!.meta!.totalElements} pageSize={response!.meta!.size} showSizeChanger={false} onChange={next => onChange(next - 1)} /> : null;

export const StudentRequestsPage = () => {
  const [trialPage, setTrialPage] = useState(0);
  const [refundPage, setRefundPage] = useState(0);
  const [extensionPage, setExtensionPage] = useState(0);
  const trials = useQuery({ queryKey: ['student-trials', trialPage], queryFn: () => bookingApi.getStudentTrialRequests(undefined, trialPage, PAGE_SIZE) });
  const refunds = useStudentRefunds(refundPage, PAGE_SIZE);
  const extensions = useStudentExtensions(extensionPage, PAGE_SIZE);
  const status = (value: string) => <Tag color={value === 'PENDING' ? 'warning' : ['ACCEPTED','APPROVED','REFUNDED'].includes(value) ? 'success' : 'default'}>{({ PENDING:'Chờ xử lý', ACCEPTED:'Đã chấp nhận', REJECTED:'Bị từ chối', APPROVED:'Đã duyệt', PROCESSING:'Đang xử lý', REFUNDED:'Đã hoàn tiền', FAILED:'Thất bại' } as Record<string,string>)[value] ?? value}</Tag>;
  const list = (loading: boolean, rows: React.ReactNode[], empty: string) => loading ? <Skeleton active /> : rows.length === 0 ? <Empty description={empty} /> : <div className="tm-stack">{rows}</div>;

  return <div className="tm-stack">
    <header className="tm-page-heading"><p className="tm-eyebrow">Theo dõi xử lý</p><h1>Yêu cầu của tôi</h1><p>Xem yêu cầu học thử, hoàn tiền và gia hạn theo từng trạng thái.</p></header>
    <section className="tm-panel"><Tabs items={[
      { key:'trial', label:'Học thử', children:<>{list(trials.isLoading, (trials.data?.data ?? []).map(item => <article key={item.id} className="tm-list-row"><div><strong>{new Date(item.preferredStartTime).toLocaleString('vi-VN')}</strong><p>{item.note || 'Không có ghi chú'}{item.rejectionReason ? ` · Lý do: ${item.rejectionReason}` : ''}</p></div>{status(item.status)}</article>), 'Bạn chưa gửi yêu cầu học thử.')}<Pager page={trialPage} response={trials.data} onChange={setTrialPage} /></> },
      { key:'refund', label:'Hoàn tiền', children:<>{list(refunds.isLoading, (refunds.data?.data ?? []).map(item => <article key={item.id} className="tm-list-row"><div><strong>{item.requestedSessions} buổi · {item.refundAmountVnd != null ? `${item.refundAmountVnd.toLocaleString('vi-VN')} ₫` : 'Số tiền đang được ước tính'}</strong><p>{item.adminNote || item.reason}</p></div>{status(item.status)}</article>), 'Bạn chưa gửi yêu cầu hoàn tiền.')}<Pager page={refundPage} response={refunds.data} onChange={setRefundPage} /></> },
      { key:'extension', label:'Gia hạn', children:<>{list(extensions.isLoading, (extensions.data?.data ?? []).map(item => <article key={item.id} className="tm-list-row"><div><strong>Xin gia hạn đến {new Date(item.requestedExpiryDate).toLocaleDateString('vi-VN')}</strong><p>{item.adminNote || item.reason}</p></div>{status(item.status)}</article>), 'Bạn chưa gửi yêu cầu gia hạn.')}<Pager page={extensionPage} response={extensions.data} onChange={setExtensionPage} /></> },
    ]} /></section>
  </div>;
};
