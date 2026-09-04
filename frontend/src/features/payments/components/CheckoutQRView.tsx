'use client';

import React from 'react';
import { Card, Typography, Button, Spin, Alert, Space } from 'antd';
import { QrcodeOutlined, ExportOutlined } from '@ant-design/icons';
import { InvoiceDetail } from '../types';

interface CheckoutQRViewProps {
  invoice: InvoiceDetail;
}

export const formatVnd = (amount: number): string => {
  return `${amount.toLocaleString('vi-VN')} ₫`;
};

export const CheckoutQRView: React.FC<CheckoutQRViewProps> = ({ invoice }) => {
  return (
    <div style={{ maxWidth: 540, margin: '0 auto' }}>
      <Card
        style={{
          borderRadius: 'var(--radius-lg, 12px)',
          border: '1px solid var(--color-border, #E7E3DC)',
          textAlign: 'center',
        }}
        styles={{ body: { padding: 'var(--space-6, 24px)' } }}
      >
        <Typography.Title level={3} style={{ marginBottom: 4 }}>
          Thanh toán đơn hàng
        </Typography.Title>
        <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
          Mã hóa đơn: <strong>{invoice.invoiceNumber}</strong>
        </Typography.Text>

        <div
          style={{
            backgroundColor: 'var(--color-primary-50, #F0FDFA)',
            padding: '16px',
            borderRadius: 'var(--radius-md, 8px)',
            marginBottom: 24,
            border: '1px solid var(--color-primary-100, #CCFBF1)',
          }}
        >
          <div style={{ fontSize: 13, color: 'var(--color-text-secondary, #57534E)' }}>
            Số tiền thanh toán
          </div>
          <div
            style={{
              fontSize: 28,
              fontWeight: 700,
              color: 'var(--color-primary-600, #0F766E)',
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {formatVnd(invoice.amountVnd)}
          </div>
          {invoice.packageName && (
            <div style={{ fontSize: 13, marginTop: 4, color: 'var(--color-text-primary, #1C1917)' }}>
              {invoice.packageName}
            </div>
          )}
        </div>

        {/* Khung hiển thị mã QR */}
        <div
          style={{
            display: 'inline-flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 16,
            backgroundColor: '#FFFFFF',
            borderRadius: 'var(--radius-md, 8px)',
            border: '1px solid var(--color-border, #E7E3DC)',
            boxShadow: 'var(--shadow-sm)',
            marginBottom: 20,
          }}
        >
          {invoice.qrCode ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={invoice.qrCode}
              alt="Mã QR Thanh toán VietQR"
              style={{ width: 220, height: 220, objectFit: 'contain' }}
            />
          ) : (
            <div
              style={{
                width: 220,
                height: 220,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: 'var(--color-surface-sunken, #F5F3EF)',
              }}
            >
              <QrcodeOutlined style={{ fontSize: 64, color: 'var(--color-text-tertiary, #8A837B)' }} />
            </div>
          )}
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary, #57534E)', marginTop: 8 }}>
            Quét mã bằng ứng dụng Ngân hàng hoặc Ví điện tử
          </span>
        </div>

        {/* Thông báo trạng thái polling */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, marginBottom: 20 }}>
          <Spin size="small" />
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Đang chờ bạn chuyển khoản... Hệ thống sẽ tự động chuyển trang khi hoàn tất.
          </Typography.Text>
        </div>

        {/* Nút thanh toán trực tiếp qua cổng payOS */}
        {invoice.checkoutUrl && (
          <Button
            type="primary"
            size="large"
            block
            icon={<ExportOutlined />}
            href={invoice.checkoutUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            Mở cổng thanh toán payOS
          </Button>
        )}
      </Card>
    </div>
  );
};
