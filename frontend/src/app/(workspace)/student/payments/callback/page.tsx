'use client';

import { CheckCircleFilled, ClockCircleFilled, ExclamationCircleFilled } from '@ant-design/icons';
import { Button, Skeleton } from 'antd';
import Link from 'next/link';
import { Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useInvoiceDetail } from '@/features/payments/hooks/usePayments';

function Callback() {
  const params = useSearchParams();
  const invoiceId = params.get('invoiceId') || '';
  const { data, isLoading, isError, refetch } = useInvoiceDetail(invoiceId, { polling: true });
  const status = data?.data?.status;
  const isPaid = status === 'PAID';
  const isTerminal = status === 'CANCELLED' || status === 'EXPIRED';
  const Icon = isPaid ? CheckCircleFilled : isTerminal ? ExclamationCircleFilled : ClockCircleFilled;
  const title = isPaid
    ? 'Thanh toán đã được xác nhận'
    : status === 'CANCELLED'
      ? 'Hóa đơn đã hủy'
      : status === 'EXPIRED'
        ? 'Hóa đơn đã hết hạn'
        : 'Đang đối chiếu thanh toán';

  return (
    <section className="tm-payment-callback" aria-labelledby="tm-payment-callback-title">
      {isLoading ? (
        <div className="tm-payment-callback-loading" role="status" aria-live="polite" aria-busy="true">
          <p>Đang kiểm tra hóa đơn</p>
          <Skeleton active title={{ width: '46%' }} paragraph={{ rows: 2 }} />
        </div>
      ) : isError ? (
        <header className="tm-payment-callback-heading tm-payment-callback-error">
          <ExclamationCircleFilled className="tm-payment-callback-icon" aria-hidden="true" />
          <div>
            <h1 id="tm-payment-callback-title">Chưa đọc được trạng thái thanh toán</h1>
            <p>Chưa đọc được trạng thái. Hãy thử kiểm tra lại.</p>
          </div>
        </header>
      ) : !invoiceId ? (
        <header className="tm-payment-callback-heading">
          <ExclamationCircleFilled className="tm-payment-callback-icon" aria-hidden="true" />
          <div>
            <h1 id="tm-payment-callback-title">Chưa có mã hóa đơn để đối chiếu</h1>
            <p>Kiểm tra gói học trong tài khoản; kết quả trên đường dẫn không xác nhận thanh toán.</p>
          </div>
        </header>
      ) : (
        <header className={`tm-payment-callback-heading tm-payment-callback-${isPaid ? 'paid' : isTerminal ? 'terminal' : 'pending'}`}>
          <Icon className="tm-payment-callback-icon" aria-hidden="true" />
          <div>
            <h1 id="tm-payment-callback-title">{title}</h1>
            <p>Gói học chỉ được cấp sau khi hệ thống xác nhận giao dịch.</p>
          </div>
        </header>
      )}

      <div className="tm-payment-callback-actions">
        {invoiceId && (
          <Button onClick={() => void refetch()} loading={isLoading}>
            Kiểm tra lại
          </Button>
        )}
        <Link className="tm-action-link tm-action-link-secondary" href="/student/packages">
          Xem gói học
        </Link>
      </div>
    </section>
  );
}

export default function Page() {
  return (
    <Suspense fallback={
      <section className="tm-payment-callback tm-payment-callback-loading" role="status" aria-live="polite">
        <p>Đang kiểm tra hóa đơn</p>
        <Skeleton active title={{ width: '46%' }} paragraph={{ rows: 2 }} />
      </section>
    }>
      <Callback />
    </Suspense>
  );
}
