'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { Spin, Empty, Button } from 'antd';
import Link from 'next/link';
import { useInvoiceDetail } from '../hooks/usePayments';
import { PaymentResultView } from '../components/PaymentResultView';

export const PaymentResultPage: React.FC = () => {
  const params = useParams();
  const invoiceId = params?.invoiceId as string;

  const { data, isLoading, isError } = useInvoiceDetail(invoiceId);
  const invoice = data?.data;

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" tip="Đang kiểm tra trạng thái thanh toán..." />
      </div>
    );
  }

  if (isError || !invoice) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 0' }}>
        <Empty description="Không tìm thấy thông tin thanh toán" />
        <Link href="/" style={{ marginTop: 16, display: 'inline-block' }}>
          <Button type="primary">Về trang chủ</Button>
        </Link>
      </div>
    );
  }

  return (
    <div style={{ padding: 'var(--space-6, 24px) var(--space-4, 16px)' }}>
      <PaymentResultView invoice={invoice} />
    </div>
  );
};
