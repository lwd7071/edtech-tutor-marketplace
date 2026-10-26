'use client';

import React, { Suspense, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Alert, Button, Input } from 'antd';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { ErrorState } from '@/shared/components/feedback/ErrorState';

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
      <div className="tm-reset-invalid">
        <ErrorState
          title="Liên kết không hợp lệ hoặc đã hết hạn"
          message="Liên kết đặt lại mật khẩu này đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu một liên kết mới."
          actionText="Gửi lại liên kết khôi phục"
          onRetry={() => router.push('/auth/forgot-password')}
        />
      </div>
    );
  }

  return (
    <main className="tm-reset-page">
      <header className="tm-reset-heading">
        <h1>Đặt lại mật khẩu</h1>
        <p>Vui lòng nhập mật khẩu mới cho tài khoản của bạn.</p>
      </header>

      {errorMsg && <Alert className="tm-reset-error" title={errorMsg} type="error" showIcon role="alert" />}

      <form className="tm-reset-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="tm-reset-field">
          <label htmlFor="reset-new-password">Mật khẩu mới</label>
          <Controller
            name="newPassword"
            control={control}
            render={({ field }) => (
              <Input.Password
                {...field}
                id="reset-new-password"
                size="large"
                placeholder="Nhập mật khẩu mới"
                autoComplete="new-password"
                status={errors.newPassword ? 'error' : ''}
                aria-invalid={Boolean(errors.newPassword)}
                aria-describedby={errors.newPassword ? 'reset-new-password-error' : 'reset-password-hint'}
              />
            )}
          />
          {errors.newPassword
            ? <p className="tm-reset-field-error" id="reset-new-password-error">{errors.newPassword.message}</p>
            : <p className="tm-reset-password-hint" id="reset-password-hint">Tối thiểu 8 ký tự, gồm chữ cái, chữ số và ký tự đặc biệt (@$!%*#?&).</p>}
        </div>

        <div className="tm-reset-field">
          <label htmlFor="reset-confirm-password">Xác nhận mật khẩu</label>
          <Controller
            name="confirmPassword"
            control={control}
            render={({ field }) => (
              <Input.Password
                {...field}
                id="reset-confirm-password"
                size="large"
                placeholder="Nhập lại mật khẩu mới"
                autoComplete="new-password"
                status={errors.confirmPassword ? 'error' : ''}
                aria-invalid={Boolean(errors.confirmPassword)}
                aria-describedby={errors.confirmPassword ? 'reset-confirm-password-error' : undefined}
              />
            )}
          />
          {errors.confirmPassword && <p className="tm-reset-field-error" id="reset-confirm-password-error">{errors.confirmPassword.message}</p>}
        </div>

        <Button className="tm-reset-submit" type="primary" htmlType="submit" size="large" block loading={loading}>
          Xác nhận đặt lại mật khẩu
        </Button>
      </form>

      <Link href="/auth/login" className="tm-reset-login-link">Quay lại đăng nhập</Link>
    </main>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<main className="tm-reset-page tm-reset-loading" aria-busy="true">Đang tải...</main>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
