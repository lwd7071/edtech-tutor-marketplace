'use client';

import React, { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Alert, Button, Input } from 'antd';
import Link from 'next/link';
import { authApi } from '@/shared/api/auth';

const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email không được để trống').email('Email không đúng định dạng'),
});

type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { control, handleSubmit, formState: { errors } } = useForm<ForgotPasswordValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  const onSubmit = async (values: ForgotPasswordValues) => {
    try {
      setLoading(true);
      setErrorMsg(null);
      await authApi.forgotPassword(values.email);
      setSuccess(true);
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="tm-forgot-page">
      <header className="tm-forgot-heading">
        <h1>Quên mật khẩu?</h1>
        <p>Đừng lo lắng, hãy nhập email của bạn và chúng tôi sẽ gửi liên kết để đặt lại mật khẩu.</p>
      </header>

      {success ? (
        <section className="tm-forgot-success" aria-live="polite">
          <Alert
            title="Đã gửi email khôi phục"
            description="Nếu email này có trong hệ thống, bạn sẽ nhận được một liên kết để khôi phục mật khẩu. Vui lòng kiểm tra hộp thư đến (và thư mục rác)."
            type="success"
            showIcon
          />
          <Link href="/auth/login" className="tm-forgot-success-link">
            <Button size="large">Quay lại đăng nhập</Button>
          </Link>
        </section>
      ) : (
        <>
          {errorMsg && <Alert className="tm-forgot-error" description={errorMsg} type="error" showIcon role="alert" />}

          <form className="tm-forgot-form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="tm-forgot-field">
              <label htmlFor="forgot-email">Email</label>
              <Controller
                name="email"
                control={control}
                render={({ field }) => (
                  <Input
                    {...field}
                    id="forgot-email"
                    size="large"
                    placeholder="Nhập email của bạn"
                    autoComplete="email"
                    status={errors.email ? 'error' : ''}
                    aria-invalid={Boolean(errors.email)}
                    aria-describedby={errors.email ? 'forgot-email-error' : undefined}
                  />
                )}
              />
              {errors.email && <p className="tm-forgot-field-error" id="forgot-email-error">{errors.email.message}</p>}
            </div>

            <Button className="tm-forgot-submit" type="primary" htmlType="submit" size="large" block loading={loading}>
              Gửi liên kết khôi phục
            </Button>
          </form>

          <Link href="/auth/login" className="tm-forgot-login-link">Quay lại đăng nhập</Link>
        </>
      )}
    </main>
  );
}
