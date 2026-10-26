'use client';

import React, { useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Button, Skeleton } from 'antd';
import { ActionLink, PageActions } from '@/shared/components/navigation/NavigationLinks';
import { useInvoiceDetail } from '../hooks/usePayments';
import { CheckoutQRView } from '../components/CheckoutQRView';

export const CheckoutPage: React.FC = () => {
  const params = useParams();
  const router = useRouter();
  const invoiceId = params?.invoiceId as string;
  const { data, isLoading, isError, refetch } = useInvoiceDetail(invoiceId, {
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
      <section className="tm-checkout-state" role="status" aria-live="polite" aria-busy="true">
        <p>Đang tải thông tin đơn hàng</p>
        <Skeleton active title={{ width: '54%' }} paragraph={{ rows: 3 }} />
      </section>
    );
  }

  if (isError || !invoice) {
    return (
      <section className="tm-checkout-state" aria-labelledby="tm-checkout-error-title">
        <div>
          <h1 id="tm-checkout-error-title">Không tìm thấy thông tin đơn hàng thanh toán</h1>
          <p>Kiểm tra lại đơn hàng hoặc tải lại thông tin thanh toán.</p>
        </div>
        <PageActions>
          {isError && <Button onClick={() => void refetch()}>Thử lại</Button>}
          <ActionLink href="/">Về trang chủ</ActionLink>
        </PageActions>
      </section>
    );
  }

  return <div className="tm-checkout-page"><CheckoutQRView invoice={invoice} /></div>;
};
