'use client';

import React, { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Input, Button, Typography, Alert } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { authApi } from '@/shared/api/auth';

const { Title, Text } = Typography;

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
    <div>
      <div style={{ textAlign: 'center', marginBottom: 'var(--space-6)' }}>
        <Title level={3} style={{ marginBottom: 'var(--space-2)' }}>Quên mật khẩu?</Title>
        <Text style={{ color: 'var(--color-text-secondary)' }}>
          Đừng lo lắng, hãy nhập email của bạn và chúng tôi sẽ gửi liên kết để đặt lại mật khẩu.
        </Text>
      </div>

      {errorMsg && (
        <Alert title={errorMsg} type="error" showIcon style={{ marginBottom: 'var(--space-4)' }} />
      )}

      {success ? (
        <div aria-live="polite" style={{ textAlign: 'center' }}>
          <Alert 
            title="Đã gửi email khôi phục" 
            description="Nếu email này có trong hệ thống, bạn sẽ nhận được một liên kết để khôi phục mật khẩu. Vui lòng kiểm tra hộp thư đến (và thư mục rác)."
            type="success" 
            showIcon 
            style={{ marginBottom: 'var(--space-6)', textAlign: 'left' }} 
          />
          <Link href="/auth/login">
            <Button size="large" icon={<ArrowLeftOutlined />}>Quay lại đăng nhập</Button>
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)}>
          <div style={{ marginBottom: 'var(--space-6)' }}>
            <Text strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Email</Text>
            <Controller
              name="email"
              control={control}
              render={({ field }) => (
                <Input 
                  {...field} 
                  size="large" 
                  placeholder="Nhập email của bạn" 
                  autoComplete="email"
                  status={errors.email ? 'error' : ''}
                />
              )}
            />
            {errors.email && <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.email.message}</Text>}
          </div>

          <Button 
            type="primary" 
            htmlType="submit" 
            size="large" 
            block 
            loading={loading}
            style={{ marginBottom: 'var(--space-6)' }}
          >
            Gửi liên kết khôi phục
          </Button>

          <div style={{ textAlign: 'center' }}>
            <Link href="/auth/login" style={{ color: 'var(--color-primary-600)', fontWeight: 'var(--weight-semibold)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
              <ArrowLeftOutlined /> Quay lại đăng nhập
            </Link>
          </div>
        </form>
      )}
    </div>
  );
}
