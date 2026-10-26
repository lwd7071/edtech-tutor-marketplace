'use client';

import React, { useState, Suspense } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Input, Button, Typography, Alert } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { ErrorState } from '@/shared/components/feedback/ErrorState';

const { Title, Text } = Typography;

const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;

const resetPasswordSchema = z.object({
  newPassword: z.string()
    .min(8, 'Mật khẩu phải dài ít nhất 8 ký tự')
    .regex(passwordRegex, 'Mật khẩu phải bao gồm chữ cái, chữ số và ký tự đặc biệt'),
  confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword'],
});

type ResetPasswordValues = z.infer<typeof resetPasswordSchema>;

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isTokenInvalid, setIsTokenInvalid] = useState(false);

  const { control, handleSubmit, formState: { errors } } = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  const onSubmit = async (values: ResetPasswordValues) => {
    if (!token) {
      setIsTokenInvalid(true);
      return;
    }
    
    try {
      setLoading(true);
      setErrorMsg(null);
      await authApi.resetPassword({ token, newPassword: values.newPassword });
      router.push('/auth/login?reset=success');
    } catch (error: any) {
      if (error.response?.status === 400 || error.response?.status === 401) {
        setIsTokenInvalid(true);
      } else {
        setErrorMsg(error.response?.data?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại sau.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (!token || isTokenInvalid) {
    return (
      <ErrorState 
        title="Liên kết không hợp lệ hoặc đã hết hạn"
        message="Liên kết đặt lại mật khẩu này đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu một liên kết mới."
        actionText="Gửi lại liên kết khôi phục"
        onRetry={() => {
          router.push('/auth/forgot-password');
        }}
      />
    );
  }

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: 'var(--space-6)' }}>
        <Title level={3} style={{ marginBottom: 'var(--space-2)' }}>Đặt lại mật khẩu</Title>
        <Text style={{ color: 'var(--color-text-secondary)' }}>
          Vui lòng nhập mật khẩu mới cho tài khoản của bạn.
        </Text>
      </div>

      {errorMsg && (
        <Alert title={errorMsg} type="error" showIcon style={{ marginBottom: 'var(--space-4)' }} />
      )}

      <form onSubmit={handleSubmit(onSubmit)}>
        <div style={{ marginBottom: 'var(--space-4)' }}>
          <Text strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Mật khẩu mới</Text>
          <Controller
            name="newPassword"
            control={control}
            render={({ field }) => (
              <Input.Password 
                {...field} 
                size="large" 
                placeholder="Nhập mật khẩu mới" 
                status={errors.newPassword ? 'error' : ''}
              />
            )}
          />
          {errors.newPassword && <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.newPassword.message}</Text>}
        </div>

        <div style={{ marginBottom: 'var(--space-6)' }}>
          <Text strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Xác nhận mật khẩu</Text>
          <Controller
            name="confirmPassword"
            control={control}
            render={({ field }) => (
              <Input.Password 
                {...field} 
                size="large" 
                placeholder="Nhập lại mật khẩu mới" 
                status={errors.confirmPassword ? 'error' : ''}
              />
            )}
          />
          {errors.confirmPassword && <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.confirmPassword.message}</Text>}
        </div>

        <Button 
          type="primary" 
          htmlType="submit" 
          size="large" 
          block 
          loading={loading}
          style={{ marginBottom: 'var(--space-6)' }}
        >
          Xác nhận đặt lại mật khẩu
        </Button>

        <div style={{ textAlign: 'center' }}>
          <Link href="/auth/login" style={{ color: 'var(--color-primary-600)', fontWeight: 'var(--weight-semibold)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
            <ArrowLeftOutlined /> Quay lại đăng nhập
          </Link>
        </div>
      </form>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div>Đang tải...</div>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
