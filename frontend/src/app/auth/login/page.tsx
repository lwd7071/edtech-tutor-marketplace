'use client';

import React, { useState, Suspense } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Input, Button, Checkbox, Typography, Divider, Alert } from 'antd';
import { GoogleOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { useAuthStore } from '@/shared/store/useAuthStore';
import { ErrorState } from '@/shared/components/feedback/ErrorState';

const { Title, Text } = Typography;

const loginSchema = z.object({
  email: z.string().min(1, 'Email không được để trống').email('Email không đúng định dạng'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
  remember: z.boolean().optional(),
});

type LoginFormValues = z.infer<typeof loginSchema>;

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const setAuth = useAuthStore((state) => state.setAuth);
  
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isLocked, setIsLocked] = useState(false);

  const { control, handleSubmit, formState: { errors } } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '', remember: false },
  });

  const onSubmit = async (values: LoginFormValues) => {
    try {
      setLoading(true);
      setErrorMsg(null);
      
      const payload = {
        email: values.email,
        password: values.password,
        deviceInfo: navigator.userAgent,
      };

      const res = await authApi.login(payload);
      
      if (res.data) {
        setAuth(res.data.user, res.data.accessToken, res.data.refreshToken, values.remember);
        const redirectPath = searchParams.get('redirect') || '/';
        router.push(redirectPath);
      }
    } catch (error: any) {
      const code = error.response?.data?.errorCode;
      if (code === 'ACCOUNT_LOCKED') {
        setIsLocked(true);
      } else {
        setErrorMsg(error.response?.data?.message || 'Đăng nhập thất bại. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (isLocked) {
    return (
      <ErrorState 
        title="Tài khoản bị khóa"
        message="Tài khoản của bạn đã bị khóa do vi phạm chính sách của chúng tôi. Vui lòng liên hệ bộ phận hỗ trợ để biết thêm chi tiết."
        actionText="Liên hệ hỗ trợ"
        onRetry={() => {
          router.push('/support');
        }}
      />
    );
  }

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: 'var(--space-6)' }}>
        <Title level={3} style={{ marginBottom: 'var(--space-2)' }}>Đăng nhập</Title>
        <Text style={{ color: 'var(--color-text-secondary)' }}>
          Chào mừng bạn quay trở lại!
        </Text>
      </div>

      {errorMsg && (
        <Alert description={errorMsg} type="error" showIcon style={{ marginBottom: 'var(--space-4)' }} />
      )}

      <form onSubmit={handleSubmit(onSubmit)}>
        <div style={{ marginBottom: 'var(--space-4)' }}>
          <Text strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Email</Text>
          <Controller
            name="email"
            control={control}
            render={({ field }) => (
              <Input 
                {...field} 
                size="large" 
                placeholder="Nhập email của bạn" 
                autoComplete="username"
                status={errors.email ? 'error' : ''}
              />
            )}
          />
          {errors.email && <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.email.message}</Text>}
        </div>

        <div style={{ marginBottom: 'var(--space-4)' }}>
          <Text strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Mật khẩu</Text>
          <Controller
            name="password"
            control={control}
            render={({ field }) => (
              <Input.Password 
                {...field} 
                size="large" 
                placeholder="Nhập mật khẩu" 
                autoComplete="current-password"
                status={errors.password ? 'error' : ''}
              />
            )}
          />
          {errors.password && <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.password.message}</Text>}
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-6)' }}>
          <Controller
            name="remember"
            control={control}
            render={({ field }) => (
              <Checkbox checked={field.value} onChange={field.onChange}>
                Ghi nhớ đăng nhập
              </Checkbox>
            )}
          />
          <Link href="/auth/forgot-password" style={{ color: 'var(--color-primary-600)' }}>
            Quên mật khẩu?
          </Link>
        </div>

        <Button 
          type="primary" 
          htmlType="submit" 
          size="large" 
          block 
          loading={loading}
          style={{ marginBottom: 'var(--space-4)' }}
        >
          Đăng nhập
        </Button>
      </form>

      <Divider plain>Hoặc</Divider>

      <Button size="large" block icon={<GoogleOutlined />} style={{ marginBottom: 'var(--space-6)' }}>
        Đăng nhập bằng Google
      </Button>

      <div style={{ textAlign: 'center' }}>
        <Text type="secondary">Chưa có tài khoản? </Text>
        <Link href="/auth/register" style={{ color: 'var(--color-primary-600)', fontWeight: 'var(--weight-semibold)' }}>
          Đăng ký ngay
        </Link>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div>Đang tải...</div>}>
      <LoginForm />
    </Suspense>
  );
}
