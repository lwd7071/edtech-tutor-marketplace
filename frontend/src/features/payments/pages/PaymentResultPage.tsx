'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { Button, Skeleton } from 'antd';
import { ActionLink, PageActions } from '@/shared/components/navigation/NavigationLinks';
import { useInvoiceDetail } from '../hooks/usePayments';
import { PaymentResultView } from '../components/PaymentResultView';

export const PaymentResultPage: React.FC = () => {
  const params = useParams();
  const invoiceId = params?.invoiceId as string;
  const { data, isLoading, isError, refetch } = useInvoiceDetail(invoiceId);
  const invoice = data?.data;

  if (isLoading) {
    return (
      <section className="tm-payment-result-state" role="status" aria-live="polite" aria-busy="true">
        <p>Đang kiểm tra trạng thái thanh toán</p>
        <Skeleton active title={{ width: '48%' }} paragraph={{ rows: 3 }} />
      </section>
    );
  }

  if (isError || !invoice) {
    return (
      <section className="tm-payment-result-state" aria-labelledby="tm-payment-result-error-title">
        <div>
          <h1 id="tm-payment-result-error-title">Không tìm thấy thông tin thanh toán</h1>
          <p>Kiểm tra lại hóa đơn hoặc tải lại trạng thái giao dịch.</p>
        </div>
        <PageActions>
          {isError && <Button onClick={() => void refetch()}>Thử lại</Button>}
          <ActionLink href="/">Về trang chủ</ActionLink>
        </PageActions>
      </section>
    );
  }

  return <PaymentResultView invoice={invoice} />;
};
