'use client';

import { Alert, Button, QRCode } from 'antd';
import Link from 'next/link';
import type { InvoiceDetail } from '../types';
import { formatVietnamDateTime } from '@/shared/lib/vietnamTime';

export const formatVnd = (amount: number) => amount.toLocaleString('vi-VN') + ' ₫';

export function CheckoutQRView({ invoice }: { invoice: InvoiceDetail }) {
  const pending = invoice.status === 'PENDING';
  const statusCopy = invoice.status === 'PAID'
    ? { type: 'success' as const, title: 'Đã xác nhận thanh toán' }
    : invoice.status === 'EXPIRED'
      ? { type: 'warning' as const, title: 'Hóa đơn hết hạn' }
      : { type: 'warning' as const, title: 'Hóa đơn đã hủy' };

  return (
    <section className="tm-checkout-invoice" aria-labelledby="tm-checkout-title">
      <header className="tm-checkout-invoice-heading">
        <h1 id="tm-checkout-title">Thanh toán gói học</h1>
        {invoice.packageName && <p className="tm-checkout-package-name">{invoice.packageName}</p>}
        <p className="tm-checkout-invoice-number">Hóa đơn {invoice.invoiceNumber}</p>
        <p className="tm-checkout-amount">{formatVnd(invoice.amountVnd)}</p>
      </header>

      {pending ? (
        <section className="tm-checkout-pending" aria-label="Hướng dẫn thanh toán">
          <div className="tm-checkout-pending-copy">
            <Alert
              type="info"
              showIcon
              title="Đang chờ xác nhận thanh toán"
              description="Trang sẽ tự cập nhật sau khi hệ thống nhận kết quả giao dịch."
            />
            {invoice.paymentExpiredAt && (
              <p className="tm-checkout-expiry">Hết hạn: {formatVietnamDateTime(invoice.paymentExpiredAt)}</p>
            )}
            {invoice.checkoutUrl && (
              <Button className="tm-checkout-pay-button" type="primary" href={invoice.checkoutUrl}>
                Mở trang thanh toán
              </Button>
            )}
          </div>
          {invoice.qrCode && (
            <div className="tm-checkout-qr" aria-label="Mã QR thanh toán">
              <QRCode value={invoice.qrCode} size={220} />
            </div>
          )}
        </section>
      ) : (
        <Alert className="tm-checkout-terminal-status" type={statusCopy.type} showIcon title={statusCopy.title} />
      )}

      <footer className="tm-checkout-footer">
        <Link href="/student/packages">Xem gói học của tôi</Link>
      </footer>
    </section>
  );
}
