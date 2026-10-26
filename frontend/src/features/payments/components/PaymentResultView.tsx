'use client';

import { CheckCircleFilled, ClockCircleFilled, ExclamationCircleFilled } from '@ant-design/icons';
import type { InvoiceDetail } from '../types';
import { formatVnd } from './CheckoutQRView';
import { ActionLink, PageActions } from '@/shared/components/navigation/NavigationLinks';

interface PaymentResultViewProps {
  invoice: InvoiceDetail;
}

export const PaymentResultView: React.FC<PaymentResultViewProps> = ({ invoice }) => {
  const isPaid = invoice.status === 'PAID';
  const isExpired = invoice.status === 'EXPIRED';
  const Icon = isPaid ? CheckCircleFilled : isExpired ? ExclamationCircleFilled : ClockCircleFilled;
  const heading = isPaid
    ? 'Thanh toán thành công!'
    : isExpired
      ? 'Hóa đơn đã hết hạn'
      : 'Giao dịch chưa hoàn thành';

  return (
    <section className={`tm-payment-result tm-payment-result-${isPaid ? 'paid' : isExpired ? 'expired' : 'pending'}`} aria-labelledby="tm-payment-result-title">
      <header className="tm-payment-result-heading">
        <Icon className="tm-payment-result-icon" aria-hidden="true" />
        <h1 id="tm-payment-result-title">{heading}</h1>
        <p>
          {isPaid
            ? `Gói học của bạn đã được kích hoạt. Mã hóa đơn: ${invoice.invoiceNumber}.`
            : isExpired
              ? `Hóa đơn ${invoice.invoiceNumber} đã quá thời hạn thanh toán.`
              : `Trạng thái đơn hàng: ${invoice.status}`}
        </p>
      </header>

      {isPaid && (
        <section className="tm-payment-result-details" aria-labelledby="tm-payment-result-details-title">
          <h2 id="tm-payment-result-details-title">Chi tiết giao dịch</h2>
          <dl>
            <div>
              <dt>Số tiền đã thanh toán</dt>
              <dd>{formatVnd(invoice.amountVnd)}</dd>
            </div>
            {invoice.packageName && (
              <div>
                <dt>Tên gói học</dt>
                <dd>{invoice.packageName}</dd>
              </div>
            )}
          </dl>
        </section>
      )}

      <div className="tm-payment-result-actions">
        <PageActions>
          {isPaid && <ActionLink href="/student/packages">Gói học của tôi</ActionLink>}
          <ActionLink href="/" variant={isPaid ? 'secondary' : 'primary'}>Về trang chủ</ActionLink>
        </PageActions>
      </div>
    </section>
  );
};
