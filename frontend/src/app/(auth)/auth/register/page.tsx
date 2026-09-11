'use client';

import React, { useEffect, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Input, Button, Checkbox, Typography, Divider, Alert, Space } from 'antd';
import { GoogleOutlined } from '@ant-design/icons';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { authApi } from '@/shared/api/auth';
import { BASE_API_URL } from '@/shared/backend';
import RadioCard from '@/shared/components/ui/RadioCard';

const { Title, Text } = Typography;

const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;

const registerSchema = z.object({
  fullName: z.string().min(1, 'Họ và tên không được để trống'),
  email: z.string().min(1, 'Email không được để trống').email('Email không đúng định dạng'),
  password: z.string()
    .min(8, 'Mật khẩu phải dài ít nhất 8 ký tự')
    .regex(passwordRegex, 'Mật khẩu phải bao gồm chữ cái, chữ số và ký tự đặc biệt'),
  role: z.enum(['STUDENT', 'TEACHER'] as const, { error: 'Vui lòng chọn vai trò' }),
  agreeTerms: z.boolean().refine((val) => val === true, { message: 'Vui lòng đồng ý với điều khoản sử dụng' }),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const { control, handleSubmit, setValue, formState: { errors } } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { fullName: '', email: '', password: '', role: 'STUDENT' },
  });

  useEffect(() => {
    if (new URLSearchParams(window.location.search).get('role') === 'TEACHER') setValue('role', 'TEACHER');
  }, [setValue]);

  const onSubmit = async (values: RegisterFormValues) => {
    try {
      setLoading(true);
      setErrorMsg(null);
      
      const payload = {
        fullName: values.fullName,
        email: values.email,
        password: values.password,
        role: values.role,
      };

      const result = await authApi.register(payload);
      
      // On success, redirect to verify email page
      router.push(`/auth/verify-email?email=${encodeURIComponent(result.data.email)}`);
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ textAlign: 'center', marginBottom: 'var(--space-6)' }}>
        <Title level={3} style={{ marginBottom: 'var(--space-2)' }}>Đăng ký tài khoản</Title>
        <Text style={{ color: 'var(--color-text-secondary)' }}>
          Bắt đầu hành trình học tập cùng EdTech Tutor
        </Text>
      </div>

      {errorMsg && (
        <Alert description={errorMsg} type="error" showIcon style={{ marginBottom: 'var(--space-4)' }} />
      )}

      <form onSubmit={handleSubmit(onSubmit)}>
        <div style={{ marginBottom: 'var(--space-4)' }}>
          <Text strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Bạn là:</Text>
          <Controller
            name="role"
            control={control}
            render={({ field }) => (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%' }}>
                <RadioCard 
                  title="Học viên / Phụ huynh"
                  description="Tìm kiếm gia sư và tham gia các khóa học"
                  value="STUDENT"
                  checked={field.value === 'STUDENT'}
                  onChange={field.onChange}
                />
                <RadioCard 
                  title="Gia sư" 
                  description="Trở thành người giảng dạy và tạo thu nhập"
                  value="TEACHER"
                  checked={field.value === 'TEACHER'}
                  onChange={field.onChange}
                />
              </div>
            )}
          />
          {errors.role && <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.role.message}</Text>}
        </div>

        <div style={{ marginBottom: 'var(--space-4)' }}>
          <Text strong style={{ display: 'block', marginBottom: 'var(--space-2)' }}>Họ và tên</Text>
          <Controller
            name="fullName"
            control={control}
            render={({ field }) => (
              <Input 
                {...field} 
                size="large" 
                placeholder="Ví dụ: Nguyễn Văn A" 
                status={errors.fullName ? 'error' : ''}
              />
            )}
          />
          {errors.fullName && <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.fullName.message}</Text>}
        </div>

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
                autoComplete="email"
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
                autoComplete="new-password"
                status={errors.password ? 'error' : ''}
              />
            )}
          />
          {errors.password && (
            <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.password.message}</Text>
          )}
          {!errors.password && (
            <Text type="secondary" style={{ fontSize: 'var(--text-body-xs)' }}>
              Mật khẩu phải dài ít nhất 8 ký tự, bao gồm chữ cái, chữ số và ký tự đặc biệt (@$!%*#?&)
            </Text>
          )}
        </div>

        <div style={{ marginBottom: 'var(--space-6)' }}>
          <Controller
            name="agreeTerms"
            control={control}
            render={({ field }) => (
              <Checkbox checked={field.value} onChange={(e) => field.onChange(e.target.checked)}>
                Tôi đồng ý với <Link href="/terms">Điều khoản sử dụng</Link> và <Link href="/privacy">Chính sách bảo mật</Link>
              </Checkbox>
            )}
          />
          {errors.agreeTerms && (
            <div style={{ marginTop: 4 }}>
              <Text type="danger" style={{ fontSize: 'var(--text-body-sm)' }}>{errors.agreeTerms.message}</Text>
            </div>
          )}
        </div>

        <Button 
          type="primary" 
          htmlType="submit" 
          size="large" 
          block 
          loading={loading}
          style={{ marginBottom: 'var(--space-4)' }}
        >
          Đăng ký
        </Button>
      </form>

      <Divider plain>Hoặc</Divider>

      <Button
        size="large"
        block
        icon={<GoogleOutlined />}
        href={`${BASE_API_URL}/oauth2/authorization/google`}
        style={{ marginBottom: 'var(--space-6)' }}
      >
        Đăng ký bằng Google
      </Button>

      <div style={{ textAlign: 'center' }}>
        <Text type="secondary">Đã có tài khoản? </Text>
        <Link href="/auth/login" style={{ color: 'var(--color-primary-600)', fontWeight: 'var(--weight-semibold)' }}>
          Đăng nhập
        </Link>
      </div>
    </div>
  );
}
