'use client';

import React, { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Spin, Empty, Button } from 'antd';
import Link from 'next/link';
import { useInvoiceDetail } from '../hooks/usePayments';
import { CheckoutQRView } from '../components/CheckoutQRView';

export const CheckoutPage: React.FC = () => {
  const params = useParams();
  const router = useRouter();
  const invoiceId = params?.invoiceId as string;

  const { data, isLoading, isError } = useInvoiceDetail(invoiceId, {
    polling: true,
    pollingInterval: 3000,
  });

  const invoice = data?.data;

  useEffect(() => {
    if (invoice?.status === 'PAID') {
      router.replace(`/student/payment-result/${invoiceId}`);
    }
  }, [invoice?.status, invoiceId, router]);

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 0' }}>
        <Spin size="large" description="Đang tải thông tin đơn hàng..." />
      </div>
    );
  }

  if (isError || !invoice) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 0' }}>
        <Empty description="Không tìm thấy thông tin đơn hàng thanh toán" />
        <Link href="/" style={{ marginTop: 16, display: 'inline-block' }}>
          <Button type="primary">Về trang chủ</Button>
        </Link>
      </div>
    );
  }

  return (
    <div style={{ padding: 'var(--space-6, 24px) var(--space-4, 16px)' }}>
      <CheckoutQRView invoice={invoice} />
    </div>
  );
};
