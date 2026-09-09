'use client';

import React, { useState, useEffect, Suspense } from 'react';
import { Button, Typography, Alert, Spin } from 'antd';
import { MailOutlined, CheckCircleOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { ErrorState } from '@/shared/components/feedback/ErrorState';

const { Title, Text } = Typography;

function VerifyEmailContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token');
  const email = searchParams.get('email'); // To pre-fill resend

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
    // In a real app, if we don't have the email from params, we might need a form
    // or we get it from auth context. For simplicity, we assume we have it or user is logged in.
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
      // Handle error
    } finally {
      setResending(false);
    }
  };

  if (verifying) {
    return (
      <div style={{ textAlign: 'center', padding: 'var(--space-10) 0' }}>
        <Spin size="large" />
        <Title level={4} style={{ marginTop: 'var(--space-4)' }}>Đang xác minh email...</Title>
      </div>
    );
  }

  if (verifyError) {
    return (
      <ErrorState 
        title="Xác minh thất bại"
        message={verifyError}
        actionText="Tới trang Đăng nhập"
        onRetry={() => {
          router.push('/auth/login');
        }}
      />
    );
  }

  if (verifySuccess) {
    return (
      <div style={{ textAlign: 'center' }}>
        <CheckCircleOutlined style={{ fontSize: 64, color: 'var(--color-success-500)', marginBottom: 'var(--space-4)' }} />
        <Title level={3} style={{ marginBottom: 'var(--space-2)' }}>Xác minh thành công</Title>
        <Text style={{ color: 'var(--color-text-secondary)', display: 'block', marginBottom: 'var(--space-6)' }}>
          Email của bạn đã được xác minh. Bây giờ bạn có thể sử dụng đầy đủ các tính năng của nền tảng.
        </Text>
        <Link href="/auth/login">
          <Button type="primary" size="large" block>Tới trang Đăng nhập</Button>
        </Link>
      </div>
    );
  }

  // State when just registered and waiting to check email
  return (
    <div style={{ textAlign: 'center' }}>
      <MailOutlined style={{ fontSize: 64, color: 'var(--color-primary-500)', marginBottom: 'var(--space-4)' }} />
      <Title level={3} style={{ marginBottom: 'var(--space-2)' }}>Vui lòng xác minh email</Title>
      
      <Alert 
        title="Kiểm tra hộp thư đến" 
        description="Chúng tôi đã gửi một email xác minh đến địa chỉ của bạn. Vui lòng nhấp vào liên kết trong email để kích hoạt tài khoản."
        type="info" 
        showIcon 
        style={{ marginBottom: 'var(--space-6)', textAlign: 'left' }} 
      />

      {resendSuccess && (
        <Alert title="Đã gửi lại email xác minh thành công." type="success" style={{ marginBottom: 'var(--space-4)' }} />
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        <Button 
          type="primary" 
          size="large" 
          onClick={handleResend} 
          loading={resending}
        >
          Gửi lại email xác minh
        </Button>
        <Link href="/auth/login">
          <Button size="large" block type="default">Quay lại trang đăng nhập</Button>
        </Link>
      </div>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<div style={{ textAlign: 'center', padding: 'var(--space-10)' }}><Spin size="large" /></div>}>
      <VerifyEmailContent />
    </Suspense>
  );
}
