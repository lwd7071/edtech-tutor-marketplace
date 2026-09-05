import React from 'react';
import { render, screen } from '@testing-library/react';
import { CheckoutQRView } from './CheckoutQRView';
import { InvoiceDetail } from '../types';

describe('CheckoutQRView (TDD)', () => {
  const mockInvoice: InvoiceDetail = {
    id: 'inv-123',
    invoiceNumber: 'INV-20260819-000123',
    pricingPackageId: 'pkg-1',
    packageName: 'Toán 11 — 10 buổi',
    amountVnd: 1000000,
    status: 'PENDING',
    checkoutUrl: 'https://pay.payos.vn/web/test',
    qrCode: 'https://api.vietqr.io/image/test.png',
    paymentExpiredAt: '2026-08-19T03:30:00Z',
  };

  it('should render invoice details, amount in VND, QR code and payOS button', () => {
    render(<CheckoutQRView invoice={mockInvoice} />);

    expect(screen.getByText(/Thanh toán đơn hàng/i)).toBeInTheDocument();
    expect(screen.getByText('INV-20260819-000123')).toBeInTheDocument();
    expect(screen.getByText('1.000.000 ₫')).toBeInTheDocument();
    expect(screen.getByAltText(/Mã QR Thanh toán/i)).toHaveAttribute('src', 'https://api.vietqr.io/image/test.png');
    expect(screen.getByRole('link', { name: /Mở cổng thanh toán payOS/i })).toHaveAttribute(
      'href',
      'https://pay.payos.vn/web/test'
    );
  });
});
