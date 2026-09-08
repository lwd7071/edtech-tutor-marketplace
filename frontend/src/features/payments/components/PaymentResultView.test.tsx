import React from 'react';
import { render, screen } from '@testing-library/react';
import { PaymentResultView } from './PaymentResultView';
import { InvoiceDetail } from '../types';

describe('PaymentResultView (TDD)', () => {
  const baseInvoice: InvoiceDetail = {
    id: 'inv-123',
    invoiceNumber: 'INV-20260819-000123',
    pricingPackageId: 'pkg-1',
    packageName: 'Toán 11 — 10 buổi',
    amountVnd: 1000000,
    status: 'PAID',
    checkoutUrl: 'https://pay.payos.vn/web/test',
    qrCode: 'https://api.vietqr.io/image/test.png',
    paymentExpiredAt: '2026-08-19T03:30:00Z',
    paidAt: '2026-08-19T03:15:00Z',
  };

  it('should render success view when invoice status is PAID', () => {
    render(<PaymentResultView invoice={baseInvoice} />);

    expect(screen.getByText('Thanh toán thành công!')).toBeInTheDocument();
    expect(screen.getByText(/Gói học của bạn đã được kích hoạt/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Gói học của tôi/i })).toHaveAttribute(
      'href',
      '/student/packages'
    );
  });

  it('should render error view when invoice status is EXPIRED', () => {
    const expiredInvoice: InvoiceDetail = {
      ...baseInvoice,
      status: 'EXPIRED',
    };

    render(<PaymentResultView invoice={expiredInvoice} />);

    expect(screen.getByText('Hóa đơn đã hết hạn')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Về trang chủ/i })).toHaveAttribute('href', '/');
  });
});
