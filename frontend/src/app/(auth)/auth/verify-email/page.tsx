'use client';

import React, { Suspense, useEffect, useState } from 'react';
import { Alert, Button, Spin } from 'antd';
import { CheckCircleOutlined, MailOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { ErrorState } from '@/shared/components/feedback/ErrorState';

function VerifyEmailContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const email = searchParams.get('email');

  const [verifying, setVerifying] = useState(!!token);
  const [verifySuccess, setVerifySuccess] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [resending, setResending] = useState(false);
  const [resendSuccess, setResendSuccess] = useState(false);

  useEffect(() => {
    if (token) {
      const verifyToken = async () => {
        try {
          await authApi.verifyEmail(token);
          setVerifySuccess(true);
        } catch (error: any) {
          setVerifyError(error.response?.data?.message || 'Liên kết xác minh không hợp lệ hoặc đã hết hạn.');
        } finally {
          setVerifying(false);
        }
      };
      verifyToken();
    }
  }, [token]);

  const handleResend = async () => {
    // Keep the current missing-email recovery behavior used by this route.
    if (!email) {
      alert('Vui lòng cung cấp email trong URL hoặc đăng nhập để gửi lại.');
      return;
    }

    try {
      setResending(true);
      setResendSuccess(false);
      await authApi.resendVerification(email);
      setResendSuccess(true);
    } catch (error: any) {
      // Preserve the existing resend failure behavior.
    } finally {
      setResending(false);
    }
  };

  if (verifying) {
    return (
      <main className="tm-verify-page tm-verify-progress" aria-busy="true" aria-live="polite">
        <Spin size="large" />
        <h1>Đang xác minh email...</h1>
      </main>
    );
  }

  if (verifyError) {
    return (
      <div className="tm-verify-page tm-verify-failure">
        <ErrorState
          title="Xác minh thất bại"
          message={verifyError}
          actionText="Tới trang Đăng nhập"
          onRetry={() => router.push('/auth/login')}
        />
      </div>
    );
  }

  if (verifySuccess) {
    return (
      <main className="tm-verify-page tm-verify-result">
        <CheckCircleOutlined className="tm-verify-icon tm-verify-success-icon" aria-hidden="true" />
        <h1>Xác minh thành công</h1>
        <p>Email của bạn đã được xác minh. Bây giờ bạn có thể sử dụng đầy đủ các tính năng của nền tảng.</p>
        <Link className="tm-verify-primary-link" href="/auth/login">
          <Button type="primary" size="large" block>Tới trang Đăng nhập</Button>
        </Link>
      </main>
    );
  }

  return (
    <main className="tm-verify-page tm-verify-waiting">
      <MailOutlined className="tm-verify-icon" aria-hidden="true" />
      <h1>Vui lòng xác minh email</h1>
      <p className="tm-verify-intro">Kiểm tra hộp thư đến và mở liên kết xác minh để kích hoạt tài khoản.</p>

      <Alert
        className="tm-verify-instructions"
        title="Kiểm tra hộp thư đến"
        description="Chúng tôi đã gửi một email xác minh đến địa chỉ của bạn. Vui lòng nhấp vào liên kết trong email để kích hoạt tài khoản."
        type="info"
        showIcon
      />

      {email && <p className="tm-verify-address">{email}</p>}
      {resendSuccess && (
        <Alert className="tm-verify-resend-success" title="Đã gửi lại email xác minh thành công." type="success" showIcon />
      )}

      <div className="tm-verify-actions">
        <Button className="tm-verify-resend" type="primary" size="large" onClick={handleResend} loading={resending}>
          Gửi lại email xác minh
        </Button>
        <Link href="/auth/login" className="tm-verify-login-link">
          Quay lại trang đăng nhập
        </Link>
      </div>
    </main>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<main className="tm-verify-page tm-verify-progress" aria-busy="true"><Spin size="large" /></main>}>
      <VerifyEmailContent />
    </Suspense>
  );
}
