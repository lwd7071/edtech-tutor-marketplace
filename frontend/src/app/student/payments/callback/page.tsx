'use client';

import React, { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Result, Button, Spin, message } from 'antd';
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';

function PaymentCallbackContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');

  useEffect(() => {
    const isSuccess = searchParams.get('success') === 'true';
    const isCancel = searchParams.get('cancel') === 'true'; // Some gateways use cancel=true
    
    if (isSuccess && !isCancel) {
      setStatus('success');
      message.success('Thanh toán thành công! Gói học đã được kích hoạt.');
    } else {
      setStatus('error');
      message.error('Thanh toán thất bại hoặc đã bị hủy.');
    }
  }, [searchParams]);

  return (
    <div className="flex-1 flex items-center justify-center min-h-[60vh]">
      <div className="bg-surface p-8 rounded-xl border border-border shadow-md max-w-lg w-full">
        {status === 'loading' ? (
          <div className="flex flex-col items-center justify-center gap-4 py-12">
            <Spin size="large" />
            <p className="text-text-secondary text-lg">Đang xác thực kết quả thanh toán...</p>
          </div>
        ) : status === 'success' ? (
          <Result
            icon={<CheckCircleFilled className="text-success-600" />}
            title="Thanh toán thành công!"
            subTitle="Cảm ơn bạn đã tin tưởng. Gói học của bạn đã được thêm vào tài khoản."
            extra={[
              <Button 
                type="primary" 
                key="home" 
                size="large" 
                onClick={() => router.push('/')}
                className="font-semibold"
              >
                Về trang chủ
              </Button>,
            ]}
          />
        ) : (
          <Result
            icon={<CloseCircleFilled className="text-error-600" />}
            title="Thanh toán thất bại"
            subTitle="Giao dịch đã bị hủy hoặc có lỗi xảy ra. Vui lòng thử lại sau."
            extra={[
              <Button 
                type="primary" 
                key="retry" 
                size="large" 
                onClick={() => router.back()}
                className="font-semibold"
              >
                Quay lại
              </Button>,
              <Button 
                key="home" 
                size="large" 
                onClick={() => router.push('/')}
              >
                Về trang chủ
              </Button>,
            ]}
          />
        )}
      </div>
    </div>
  );
}

export default function PaymentCallbackPage() {
  return (
    <div className="container mx-auto px-4 py-12">
      <Suspense fallback={
        <div className="flex justify-center items-center h-[60vh]">
          <Spin size="large" tip="Đang tải..." />
        </div>
      }>
        <PaymentCallbackContent />
      </Suspense>
    </div>
  );
}
