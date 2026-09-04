'use client';

import React from 'react';
import { Result, Button, Card, Typography, Space } from 'antd';
import Link from 'next/link';
import { InvoiceDetail } from '../types';
import { formatVnd } from './CheckoutQRView';

interface PaymentResultViewProps {
  invoice: InvoiceDetail;
}

export const PaymentResultView: React.FC<PaymentResultViewProps> = ({ invoice }) => {
  if (invoice.status === 'PAID') {
    return (
      <div style={{ maxWidth: 600, margin: '40px auto' }}>
        <Card
          style={{ borderRadius: 'var(--radius-lg, 12px)', textAlign: 'center' }}
          styles={{ body: { padding: 'var(--space-8, 32px)' } }}
        >
          <Result
            status="success"
            title="Thanh toán thành công!"
            subTitle={`Gói học của bạn đã được kích hoạt. Mã hóa đơn: ${invoice.invoiceNumber}.`}
            extra={[
              <Link href="/student/packages" key="packages">
                <Button type="primary" size="large">
                  Gói học của tôi
                </Button>
              </Link>,
              <Link href="/" key="home">
                <Button size="large">Về trang chủ</Button>
              </Link>,
            ]}
          >
            <div
              style={{
                backgroundColor: 'var(--color-surface-sunken, #F5F3EF)',
                padding: 16,
                borderRadius: 'var(--radius-md, 8px)',
                textAlign: 'left',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span style={{ color: 'var(--color-text-secondary, #57534E)' }}>Số tiền đã thanh toán:</span>
                <span style={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}>
                  {formatVnd(invoice.amountVnd)}
                </span>
              </div>
              {invoice.packageName && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--color-text-secondary, #57534E)' }}>Tên gói học:</span>
                  <span style={{ fontWeight: 600 }}>{invoice.packageName}</span>
                </div>
              )}
            </div>
          </Result>
        </Card>
      </div>
    );
  }

  if (invoice.status === 'EXPIRED') {
    return (
      <div style={{ maxWidth: 600, margin: '40px auto' }}>
        <Card
          style={{ borderRadius: 'var(--radius-lg, 12px)', textAlign: 'center' }}
          styles={{ body: { padding: 'var(--space-8, 32px)' } }}
        >
          <Result
            status="error"
            title="Hóa đơn đã hết hạn"
            subTitle={`Hóa đơn ${invoice.invoiceNumber} đã quá thời hạn thanh toán.`}
            extra={[
              <Link href="/" key="back">
                <Button type="primary" size="large">
                  Quay lại
                </Button>
              </Link>,
            ]}
          />
        </Card>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 600, margin: '40px auto' }}>
      <Card
        style={{ borderRadius: 'var(--radius-lg, 12px)', textAlign: 'center' }}
        styles={{ body: { padding: 'var(--space-8, 32px)' } }}
      >
        <Result
          status="info"
          title="Giao dịch chưa hoàn thành"
          subTitle={`Trạng thái đơn hàng: ${invoice.status}`}
          extra={[
            <Link href="/" key="home">
              <Button type="primary">Về trang chủ</Button>
            </Link>,
          ]}
        />
      </Card>
    </div>
  );
};
