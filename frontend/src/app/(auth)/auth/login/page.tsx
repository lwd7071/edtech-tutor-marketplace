'use client';

import React, { Suspense, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Alert, Button, Checkbox, Divider, Input } from 'antd';
import { GoogleOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/shared/api/auth';
import { BASE_API_URL } from '@/shared/backend';
import { useAuthStore } from '@/features/auth';
import { ErrorState } from '@/shared/components/feedback/ErrorState';
import { roleHome, safeReturnTo } from '@/shared/lib/navigation';

const loginSchema = z.object({
  email: z.string().min(1, 'Email không được để trống').email('Email không đúng định dạng'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
  remember: z.boolean().optional(),
});

type LoginFormValues = z.infer<typeof loginSchema>;

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const establish = useAuthStore((state) => state.establish);
  const queryClient = useQueryClient();
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
        queryClient.clear();
        establish(res.data, values.remember);
        const redirectPath = safeReturnTo(searchParams.get('redirect'), res.data.user.role, roleHome(res.data.user.role));
        router.replace(redirectPath);
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
        onRetry={() => router.push('/support')}
      />
    );
  }

  return (
    <main className="tm-login-page">
      <header className="tm-login-heading">
        <h1>Đăng nhập</h1>
        <p>Chào mừng bạn quay trở lại!</p>
      </header>

      {errorMsg && (
        <Alert className="tm-login-error" description={errorMsg} type="error" showIcon role="alert" />
      )}

      <form className="tm-login-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="tm-login-field">
          <label htmlFor="login-email">Email</label>
          <Controller
            name="email"
            control={control}
            render={({ field }) => (
              <Input
                {...field}
                id="login-email"
                size="large"
                placeholder="Nhập email của bạn"
                autoComplete="username"
                status={errors.email ? 'error' : ''}
                aria-invalid={Boolean(errors.email)}
                aria-describedby={errors.email ? 'login-email-error' : undefined}
              />
            )}
          />
          {errors.email && <p className="tm-login-field-error" id="login-email-error">{errors.email.message}</p>}
        </div>

        <div className="tm-login-field">
          <div className="tm-login-password-heading">
            <label htmlFor="login-password">Mật khẩu</label>
            <Link href="/auth/forgot-password">Quên mật khẩu?</Link>
          </div>
          <Controller
            name="password"
            control={control}
            render={({ field }) => (
              <Input.Password
                {...field}
                id="login-password"
                size="large"
                placeholder="Nhập mật khẩu"
                autoComplete="current-password"
                status={errors.password ? 'error' : ''}
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? 'login-password-error' : undefined}
              />
            )}
          />
          {errors.password && <p className="tm-login-field-error" id="login-password-error">{errors.password.message}</p>}
        </div>

        <Controller
          name="remember"
          control={control}
          render={({ field }) => (
            <Checkbox className="tm-login-remember" checked={field.value} onChange={field.onChange}>
              Ghi nhớ đăng nhập
            </Checkbox>
          )}
        />

        <Button className="tm-login-submit" type="primary" htmlType="submit" size="large" block loading={loading}>
          Đăng nhập
        </Button>
      </form>

      <Divider className="tm-login-divider" plain>Hoặc</Divider>

      <Button
        className="tm-login-google"
        size="large"
        block
        icon={<GoogleOutlined />}
        href={`${BASE_API_URL}/oauth2/authorization/google`}
      >
        Đăng nhập bằng Google
      </Button>

      <footer className="tm-login-register">
        <span>Chưa có tài khoản?</span>
        <Link href="/auth/register">Đăng ký ngay</Link>
      </footer>
    </main>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<main className="tm-login-page tm-login-loading" aria-busy="true">Đang tải...</main>}>
      <LoginForm />
    </Suspense>
  );
}
