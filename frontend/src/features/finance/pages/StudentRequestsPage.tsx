'use client';

import { Children, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Pagination, Skeleton } from 'antd';
import { bookingApi } from '@/features/bookings/api/bookingApi';
import { formatVietnamDate, formatVietnamDateTime } from '@/shared/lib/vietnamTime';
import { useStudentExtensions, useStudentRefunds } from '../hooks/useFinance';

const PAGE_SIZE = 10;
type RequestKind = 'trial' | 'refund' | 'extension';
type PageResponse = { meta?: { totalElements: number; totalPages: number; size: number } | null };

const labels: Record<RequestKind, string> = { trial: 'Học thử', refund: 'Hoàn tiền', extension: 'Gia hạn' };
const statusLabels: Record<string, string> = {
  PENDING: 'Chờ xử lý', ACCEPTED: 'Đã chấp nhận', REJECTED: 'Bị từ chối', APPROVED: 'Đã duyệt',
  PROCESSING: 'Đang xử lý', REFUNDED: 'Đã hoàn tiền', FAILED: 'Thất bại',
};

function RequestStatus({ value }: { value: string }) {
  const tone = value === 'PENDING' || value === 'PROCESSING' ? 'pending' :
    ['ACCEPTED', 'APPROVED', 'REFUNDED'].includes(value) ? 'complete' :
      ['REJECTED', 'FAILED'].includes(value) ? 'declined' : 'neutral';
  return <span className={`tm-requests__status tm-requests__status--${tone}`}>{statusLabels[value] ?? value}</span>;
}

function RequestRows({ children }: { children: React.ReactNode }) {
  return <div className="tm-requests__rows" role="list">{children}</div>;
}

function RequestRow({ title, detail, status }: { title: string; detail: string; status: string }) {
  return (
    <article className="tm-requests__row" role="listitem">
      <div className="tm-requests__row-copy">
        <h3>{title}</h3>
        <p>{detail}</p>
      </div>
      <RequestStatus value={status} />
    </article>
  );
}

function RequestContent({
  isLoading, isError, onRetry, empty, children, response, page, onPageChange,
}: {
  isLoading: boolean; isError: boolean; onRetry: () => void; empty: string; children: React.ReactNode;
  response?: PageResponse; page: number; onPageChange: (page: number) => void;
}) {
  if (isLoading) return <div className="tm-requests__loading" role="status" aria-label="Đang tải yêu cầu"><Skeleton active paragraph={{ rows: 3 }} /></div>;
  if (isError) return <Alert className="tm-requests__error" type="error" showIcon message="Chưa tải được danh sách yêu cầu" description="Kiểm tra kết nối rồi thử tải lại." action={<Button onClick={onRetry}>Thử lại</Button>} />;
  const hasRows = Children.count(children) > 0;
  return (
    <>
      {hasRows ? <RequestRows>{children}</RequestRows> : <div className="tm-requests__empty"><h2>{empty}</h2><p>Các yêu cầu bạn đã gửi sẽ xuất hiện tại đây.</p></div>}
      {(response?.meta?.totalPages ?? 0) > 1 && <Pagination className="tm-requests__pagination" current={page + 1} total={response?.meta?.totalElements} pageSize={response?.meta?.size || PAGE_SIZE} showSizeChanger={false} onChange={next => onPageChange(next - 1)} />}
    </>
  );
}

export const StudentRequestsPage = () => {
  const [active, setActive] = useState<RequestKind>('trial');
  const [trialPage, setTrialPage] = useState(0);
  const [refundPage, setRefundPage] = useState(0);
  const [extensionPage, setExtensionPage] = useState(0);
  const trials = useQuery({ queryKey: ['student-trials', trialPage], queryFn: () => bookingApi.getStudentTrialRequests(undefined, trialPage, PAGE_SIZE) });
  const refunds = useStudentRefunds(refundPage, PAGE_SIZE);
  const extensions = useStudentExtensions(extensionPage, PAGE_SIZE);
  const pages = { trial: [trialPage, setTrialPage, trials] as const, refund: [refundPage, setRefundPage, refunds] as const, extension: [extensionPage, setExtensionPage, extensions] as const };
  const [page, setPage] = pages[active];

  return (
    <main className="tm-stack tm-requests" aria-labelledby="student-requests-title">
      <header className="tm-page-heading tm-requests__heading">
        <h1 id="student-requests-title">Yêu cầu của tôi</h1>
        <p>Theo dõi tiến độ học thử, hoàn tiền và gia hạn.</p>
      </header>

      <nav className="tm-requests__tabs" role="tablist" aria-label="Loại yêu cầu">
        {(Object.keys(labels) as RequestKind[]).map(kind => (
          <button key={kind} id={`student-requests-tab-${kind}`} type="button" role="tab" aria-selected={active === kind}
            aria-controls="student-requests-panel" tabIndex={active === kind ? 0 : -1} className="tm-requests__tab"
            onClick={() => setActive(kind)} onKeyDown={event => {
              const kinds = Object.keys(labels) as RequestKind[];
              const current = kinds.indexOf(kind);
              const next = event.key === 'ArrowRight' ? (current + 1) % kinds.length : event.key === 'ArrowLeft' ? (current + kinds.length - 1) % kinds.length : event.key === 'Home' ? 0 : event.key === 'End' ? kinds.length - 1 : -1;
              if (next >= 0) { event.preventDefault(); setActive(kinds[next]); document.getElementById(`student-requests-tab-${kinds[next]}`)?.focus(); }
            }}>{labels[kind]}</button>
        ))}
      </nav>

      <section id="student-requests-panel" className="tm-requests__panel" role="tabpanel" aria-labelledby={`student-requests-tab-${active}`}>
        {active === 'trial' && <RequestContent isLoading={trials.isLoading} isError={trials.isError} onRetry={() => void trials.refetch()} empty="Chưa có yêu cầu học thử" response={trials.data} page={trialPage} onPageChange={setTrialPage}>
          {(trials.data?.data ?? []).map(item => <RequestRow key={item.id} title={formatVietnamDateTime(item.preferredStartTime)} detail={[item.note || 'Không có ghi chú', item.rejectionReason ? `Lý do từ chối: ${item.rejectionReason}` : ''].filter(Boolean).join(' · ')} status={item.status} />)}
        </RequestContent>}
        {active === 'refund' && <RequestContent isLoading={refunds.isLoading} isError={refunds.isError} onRetry={() => void refunds.refetch()} empty="Chưa có yêu cầu hoàn tiền" response={refunds.data} page={refundPage} onPageChange={setRefundPage}>
          {(refunds.data?.data ?? []).map(item => <RequestRow key={item.id} title={`${item.requestedSessions} buổi · ${item.refundAmountVnd != null ? `${item.refundAmountVnd.toLocaleString('vi-VN')} ₫` : 'Số tiền đang được ước tính'}`} detail={item.adminNote || item.reason} status={item.status} />)}
        </RequestContent>}
        {active === 'extension' && <RequestContent isLoading={extensions.isLoading} isError={extensions.isError} onRetry={() => void extensions.refetch()} empty="Chưa có yêu cầu gia hạn" response={extensions.data} page={extensionPage} onPageChange={setExtensionPage}>
          {(extensions.data?.data ?? []).map(item => <RequestRow key={item.id} title={`Xin gia hạn đến ${formatVietnamDate(item.requestedExpiryDate)}`} detail={item.adminNote || item.reason} status={item.status} />)}
        </RequestContent>}
      </section>
    </main>
  );
};
